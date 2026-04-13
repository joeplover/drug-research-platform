package com.aiforaso.platform.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.aiforaso.platform.config.MilvusProperties;
import com.aiforaso.platform.domain.LiteratureChunk;
import com.aiforaso.platform.dto.HealthComponentView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PreDestroy;

@Service
public class MilvusVectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStoreService.class);
    private static final long HEALTH_RETRY_COOLDOWN_MS = 15_000L;
    private static final long HEALTH_SNAPSHOT_CACHE_MS = 8_000L;

    private final MilvusProperties milvusProperties;
    private final Gson gson = new Gson();
    private final AtomicBoolean connectionWarningLogged = new AtomicBoolean(false);
    private final Method connectMethod;
    private final Method initServicesMethod;

    private volatile MilvusClientV2 milvusClient;
    private volatile boolean initialized;
    private volatile long lastFailureAt;
    private volatile String lastFailureMessage = "";
    private volatile HealthComponentView lastHealthSnapshot;
    private volatile long lastHealthSnapshotAt;

    public MilvusVectorStoreService(MilvusProperties milvusProperties) {
        this.milvusProperties = milvusProperties;
        this.connectMethod = resolveMethod("connect", ConnectConfig.class);
        this.initServicesMethod = resolveMethod("initServices", String.class);
    }

    public boolean isEnabled() {
        return milvusProperties.isEnabled();
    }

    public String currentStatus() {
        return healthComponent().status();
    }

    public HealthComponentView healthComponent() {
        long start = System.currentTimeMillis();
        HealthComponentView cachedSnapshot = lastHealthSnapshot;
        if (cachedSnapshot != null && System.currentTimeMillis() - lastHealthSnapshotAt < HEALTH_SNAPSHOT_CACHE_MS) {
            return cachedSnapshot;
        }

        if (!isEnabled()) {
            HealthComponentView snapshot = new HealthComponentView(
                    "Milvus",
                    "DOWN",
                    "Milvus is disabled by configuration",
                    System.currentTimeMillis() - start);
            updateHealthSnapshot(snapshot);
            return snapshot;
        }

        if (shouldSkipReconnect()) {
            HealthComponentView snapshot = new HealthComponentView(
                    "Milvus",
                    "DEGRADED",
                    "Milvus is temporarily unavailable. Last error: " + lastFailureMessage,
                    System.currentTimeMillis() - start);
            updateHealthSnapshot(snapshot);
            return snapshot;
        }

        try {
            MilvusClientV2 client = getClient();
            boolean exists = client.hasCollection(HasCollectionReq.builder()
                    .collectionName(milvusProperties.getCollectionName())
                    .build());
            initialized = initialized || exists;
            lastFailureAt = 0L;
            lastFailureMessage = "";
            connectionWarningLogged.set(false);
            HealthComponentView snapshot = new HealthComponentView(
                    "Milvus",
                    "UP",
                    "Milvus is reachable, collection=" + milvusProperties.getCollectionName() + ", exists=" + exists,
                    System.currentTimeMillis() - start);
            updateHealthSnapshot(snapshot);
            return snapshot;
        } catch (Exception exception) {
            handleMilvusFailure(exception);
            HealthComponentView snapshot = new HealthComponentView(
                    "Milvus",
                    "DEGRADED",
                    "Milvus is unavailable. The system will temporarily fall back to local retrieval. Cause: " + exception.getMessage(),
                    System.currentTimeMillis() - start);
            updateHealthSnapshot(snapshot);
            return snapshot;
        }
    }

    public VectorSyncResult syncLiteratureChunks(Long literatureId, List<LiteratureChunk> chunks, ChunkEmbeddingService chunkEmbeddingService) {
        if (!isEnabled() || chunks == null || chunks.isEmpty()) {
            return new VectorSyncResult("SKIPPED", 0, !isEnabled() ? "Milvus is disabled" : "No chunks available");
        }

        float[] sampleVector = toFloatArray(chunkEmbeddingService.deserialize(chunks.get(0).getEmbeddingJson()));
        if (!ensureCollection(sampleVector.length)) {
            return new VectorSyncResult("DEGRADED", 0, "Milvus collection is unavailable");
        }

        deleteByLiteratureId(literatureId);

        List<JsonObject> rows = new ArrayList<>();
        for (LiteratureChunk chunk : chunks) {
            JsonObject row = new JsonObject();
            row.addProperty(milvusProperties.getPrimaryFieldName(), chunk.getId());
            row.addProperty(milvusProperties.getLiteratureIdFieldName(), chunk.getLiterature().getId());
            row.add(milvusProperties.getVectorFieldName(), gson.toJsonTree(toFloatList(chunkEmbeddingService.deserialize(chunk.getEmbeddingJson()))));
            rows.add(row);
        }

        try {
            getClient().insert(InsertReq.builder()
                    .collectionName(milvusProperties.getCollectionName())
                    .data(rows)
                    .build());
            return new VectorSyncResult("SYNCED", rows.size(), "Milvus insert completed");
        } catch (Exception exception) {
            handleMilvusFailure(exception);
            return new VectorSyncResult("DEGRADED", 0, "Milvus insert failed: " + exception.getMessage());
        }
    }

    public List<SearchResp.SearchResult> search(double[] queryVector, int topK) {
        if (!isEnabled() || !initialized || queryVector == null || queryVector.length == 0) {
            return List.of();
        }

        try {
            SearchResp response = getClient().search(SearchReq.builder()
                    .collectionName(milvusProperties.getCollectionName())
                    .annsField(milvusProperties.getVectorFieldName())
                    .consistencyLevel(ConsistencyLevel.BOUNDED)
                    .topK(topK)
                    .data(Collections.singletonList(new FloatVec(toFloatArray(queryVector))))
                    .build());

            List<List<SearchResp.SearchResult>> results = response.getSearchResults();
            if (results == null || results.isEmpty()) {
                return List.of();
            }
            return results.get(0);
        } catch (Exception exception) {
            handleMilvusFailure(exception);
            return List.of();
        }
    }

    public void deleteByLiteratureId(Long literatureId) {
        if (!isEnabled() || !initialized || literatureId == null) {
            return;
        }

        try {
            getClient().delete(DeleteReq.builder()
                    .collectionName(milvusProperties.getCollectionName())
                    .filter(milvusProperties.getLiteratureIdFieldName() + " == " + literatureId)
                    .build());
        } catch (Exception exception) {
            handleMilvusFailure(exception);
        }
    }

    private synchronized boolean ensureCollection(int dimension) {
        if (initialized) {
            return true;
        }

        try {
            MilvusClientV2 client = getClient();
            boolean exists = client.hasCollection(HasCollectionReq.builder()
                    .collectionName(milvusProperties.getCollectionName())
                    .build());

            if (!exists) {
                CreateCollectionReq.FieldSchema pkField = CreateCollectionReq.FieldSchema.builder()
                        .name(milvusProperties.getPrimaryFieldName())
                        .dataType(DataType.Int64)
                        .isPrimaryKey(true)
                        .autoID(false)
                        .build();

                CreateCollectionReq.FieldSchema literatureIdField = CreateCollectionReq.FieldSchema.builder()
                        .name(milvusProperties.getLiteratureIdFieldName())
                        .dataType(DataType.Int64)
                        .build();

                CreateCollectionReq.FieldSchema vectorField = CreateCollectionReq.FieldSchema.builder()
                        .name(milvusProperties.getVectorFieldName())
                        .dataType(DataType.FloatVector)
                        .dimension(dimension)
                        .build();

                CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                        .fieldSchemaList(List.of(pkField, literatureIdField, vectorField))
                        .build();

                Map<String, Object> extraParams = new HashMap<>();
                extraParams.put("M", 16);
                extraParams.put("efConstruction", 64);

                IndexParam indexParam = IndexParam.builder()
                        .fieldName(milvusProperties.getVectorFieldName())
                        .indexType(IndexParam.IndexType.valueOf(milvusProperties.getIndexType()))
                        .metricType(IndexParam.MetricType.valueOf(milvusProperties.getMetricType()))
                        .extraParams(extraParams)
                        .build();

                client.createCollection(CreateCollectionReq.builder()
                        .collectionName(milvusProperties.getCollectionName())
                        .description("Vector collection for literature chunks")
                        .enableDynamicField(false)
                        .collectionSchema(schema)
                        .build());

                client.createIndex(CreateIndexReq.builder()
                        .collectionName(milvusProperties.getCollectionName())
                        .indexParams(Collections.singletonList(indexParam))
                        .build());
            }

            client.loadCollection(LoadCollectionReq.builder()
                    .collectionName(milvusProperties.getCollectionName())
                    .build());

            initialized = true;
            lastFailureAt = 0L;
            lastFailureMessage = "";
            connectionWarningLogged.set(false);
            return true;
        } catch (Exception exception) {
            handleMilvusFailure(exception);
            return false;
        }
    }

    private MilvusClientV2 getClient() {
        if (milvusClient != null) {
            return milvusClient;
        }
        synchronized (this) {
            if (milvusClient != null) {
                return milvusClient;
            }
            ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                    .uri(milvusProperties.getUri())
                    .dbName(milvusProperties.getDatabaseName());
            if (milvusProperties.getToken() != null && !milvusProperties.getToken().isBlank()) {
                builder.token(milvusProperties.getToken());
            }
            milvusClient = createClient(builder.build());
            return milvusClient;
        }
    }

    private MilvusClientV2 createClient(ConnectConfig connectConfig) {
        MilvusClientV2 client = new MilvusClientV2(null);
        try {
            connectMethod.invoke(client, connectConfig);
            initServicesMethod.invoke(client, connectConfig.getDbName());
            return client;
        } catch (InvocationTargetException exception) {
            client.close();
            Throwable targetException = exception.getTargetException();
            if (targetException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Failed to initialize Milvus client", targetException);
        } catch (Exception exception) {
            client.close();
            throw new IllegalStateException("Failed to initialize Milvus client", exception);
        }
    }

    private boolean shouldSkipReconnect() {
        return milvusClient == null
                && lastFailureAt > 0
                && System.currentTimeMillis() - lastFailureAt < HEALTH_RETRY_COOLDOWN_MS;
    }

    private void handleMilvusFailure(Exception exception) {
        logMilvusUnavailable(exception);
        initialized = false;
        lastFailureAt = System.currentTimeMillis();
        lastFailureMessage = exception.getMessage() == null ? "unknown" : exception.getMessage();
        resetClient();
    }

    private void updateHealthSnapshot(HealthComponentView snapshot) {
        lastHealthSnapshot = snapshot;
        lastHealthSnapshotAt = System.currentTimeMillis();
    }

    private void resetClient() {
        MilvusClientV2 currentClient = milvusClient;
        milvusClient = null;
        if (currentClient == null) {
            return;
        }
        try {
            currentClient.close();
        } catch (Exception closeException) {
            log.debug("Milvus client close failed: {}", closeException.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        resetClient();
    }

    private Method resolveMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = MilvusClientV2.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to access Milvus client method: " + name, exception);
        }
    }

    private void logMilvusUnavailable(Exception exception) {
        if (connectionWarningLogged.compareAndSet(false, true)) {
            log.warn(
                    "Milvus is unavailable at {}. The application will continue to run, and vector operations will temporarily fall back to local retrieval logic. Cause: {}",
                    milvusProperties.getUri(),
                    exception.getMessage());
        }
    }

    private float[] toFloatArray(double[] source) {
        float[] target = new float[source.length];
        for (int index = 0; index < source.length; index++) {
            target[index] = (float) source[index];
        }
        return target;
    }

    private List<Float> toFloatList(double[] source) {
        List<Float> values = new ArrayList<>(source.length);
        for (double value : source) {
            values.add((float) value);
        }
        return values;
    }
}
