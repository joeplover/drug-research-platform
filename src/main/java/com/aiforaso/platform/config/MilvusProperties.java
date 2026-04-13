package com.aiforaso.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.vector.milvus")
public class MilvusProperties {

    private boolean enabled = true;
    private String uri = "http://127.0.0.1:19530";
    private String token;
    private String databaseName = "default";
    private String collectionName = "literature_chunk_vectors";
    private String primaryFieldName = "chunk_id";
    private String literatureIdFieldName = "literature_id";
    private String vectorFieldName = "embedding";
    private String metricType = "COSINE";
    private String indexType = "HNSW";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getPrimaryFieldName() {
        return primaryFieldName;
    }

    public void setPrimaryFieldName(String primaryFieldName) {
        this.primaryFieldName = primaryFieldName;
    }

    public String getLiteratureIdFieldName() {
        return literatureIdFieldName;
    }

    public void setLiteratureIdFieldName(String literatureIdFieldName) {
        this.literatureIdFieldName = literatureIdFieldName;
    }

    public String getVectorFieldName() {
        return vectorFieldName;
    }

    public void setVectorFieldName(String vectorFieldName) {
        this.vectorFieldName = vectorFieldName;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }
}
