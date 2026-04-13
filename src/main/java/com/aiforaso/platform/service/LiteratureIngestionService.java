package com.aiforaso.platform.service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiforaso.platform.domain.Literature;
import com.aiforaso.platform.domain.LiteratureChunk;
import com.aiforaso.platform.dto.LiteratureChunkView;
import com.aiforaso.platform.dto.LiteratureVectorStatusView;
import com.aiforaso.platform.repository.LiteratureChunkRepository;

@Service
public class LiteratureIngestionService {

    private static final String OVERVIEW_CACHE = "literature-overview";

    private final LiteratureChunkRepository literatureChunkRepository;
    private final DocumentParserService documentParserService;
    private final LiteratureChunkingService literatureChunkingService;
    private final ChunkEmbeddingService chunkEmbeddingService;
    private final LiteratureService literatureService;
    private final MilvusVectorStoreService milvusVectorStoreService;

    public LiteratureIngestionService(
            LiteratureChunkRepository literatureChunkRepository,
            DocumentParserService documentParserService,
            LiteratureChunkingService literatureChunkingService,
            ChunkEmbeddingService chunkEmbeddingService,
            LiteratureService literatureService,
            MilvusVectorStoreService milvusVectorStoreService) {
        this.literatureChunkRepository = literatureChunkRepository;
        this.documentParserService = documentParserService;
        this.literatureChunkingService = literatureChunkingService;
        this.chunkEmbeddingService = chunkEmbeddingService;
        this.literatureService = literatureService;
        this.milvusVectorStoreService = milvusVectorStoreService;
    }

    @CacheEvict(cacheNames = OVERVIEW_CACHE, key = "#literatureId")
    @Transactional
    public List<LiteratureChunkView> ingest(Long literatureId) {
        Literature literature = literatureService.getEntity(literatureId);
        if (literature.getStoragePath() == null || literature.getStoragePath().isBlank()) {
            throw new IllegalArgumentException("Literature storagePath is empty: " + literatureId);
        }

        Path path = literatureService.resolveStoragePath(literature.getStoragePath());
        String extractedText = documentParserService.extractText(path, literature.getSourceType());
        List<String> chunks = literatureChunkingService.chunk(extractedText);

        literatureChunkRepository.deleteByLiteratureId(literatureId);
        literatureChunkRepository.flush();
        for (int index = 0; index < chunks.size(); index++) {
            LiteratureChunk chunk = new LiteratureChunk();
            chunk.setLiterature(literature);
            chunk.setChunkIndex(index);
            chunk.setSourceSection(literature.getSourceType());
            chunk.setChunkLabel(path.getFileName().toString() + "#chunk-" + index);
            chunk.setContent(chunks.get(index));
            chunk.setEmbeddingJson(null);
            literatureChunkRepository.save(chunk);
        }

        literature.setVectorSyncStatus("CHUNKED");
        literature.setVectorSyncDetail("Chunking completed. Vectorization has not been executed yet.");
        literature.setVectorSyncedChunkCount(0);
        literature.setVectorSyncedAt(null);
        literatureService.saveEntity(literature);
        milvusVectorStoreService.deleteByLiteratureId(literatureId);

        return list(literatureId);
    }

    @CacheEvict(cacheNames = OVERVIEW_CACHE, key = "#literatureId")
    @Transactional
    public List<LiteratureChunkView> vectorize(Long literatureId) {
        List<LiteratureChunk> persistedChunks = literatureChunkRepository.findByLiteratureIdOrderByChunkIndexAsc(literatureId);
        if (persistedChunks.isEmpty()) {
            ingest(literatureId);
            persistedChunks = literatureChunkRepository.findByLiteratureIdOrderByChunkIndexAsc(literatureId);
        }

        for (LiteratureChunk chunk : persistedChunks) {
            chunk.setEmbeddingJson(chunkEmbeddingService.serialize(chunkEmbeddingService.embed(chunk.getContent())));
        }
        literatureChunkRepository.saveAll(persistedChunks);

        Literature literature = literatureService.getEntity(literatureId);
        VectorSyncResult vectorSyncResult = milvusVectorStoreService.syncLiteratureChunks(literatureId, persistedChunks, chunkEmbeddingService);
        literature.setVectorSyncStatus(vectorSyncResult.status());
        literature.setVectorSyncDetail(vectorSyncResult.detail());
        literature.setVectorSyncedChunkCount(vectorSyncResult.syncedCount());
        literature.setVectorSyncedAt(LocalDateTime.now());
        literatureService.saveEntity(literature);

        return list(literatureId);
    }

    @Transactional(readOnly = true)
    public List<LiteratureChunkView> list(Long literatureId) {
        return literatureChunkRepository.findByLiteratureIdOrderByChunkIndexAsc(literatureId).stream()
                .map(chunk -> new LiteratureChunkView(
                        chunk.getId(),
                        chunk.getLiterature().getId(),
                        chunk.getChunkIndex(),
                        chunk.getSourceSection(),
                        chunk.getChunkLabel(),
                        chunk.getContent()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LiteratureVectorStatusView vectorStatus(Long literatureId) {
        List<LiteratureChunk> chunks = literatureChunkRepository.findByLiteratureIdOrderByChunkIndexAsc(literatureId);
        int chunkCount = chunks.size();
        int embeddedChunkCount = (int) chunks.stream()
                .filter(chunk -> chunk.getEmbeddingJson() != null && !chunk.getEmbeddingJson().isBlank() && !"[]".equals(chunk.getEmbeddingJson()))
                .count();
        String milvusStatus = milvusVectorStoreService.currentStatus();
        String embeddingMode = chunkEmbeddingService.getLastMode();

        return new LiteratureVectorStatusView(
                literatureId,
                chunkCount,
                embeddedChunkCount,
                milvusStatus,
                embeddingMode,
                resolveVectorStatusMessage(literatureId, chunkCount, embeddedChunkCount));
    }

    private String resolveVectorStatusMessage(Long literatureId, int chunkCount, int embeddedChunkCount) {
        Literature literature = literatureService.getEntity(literatureId);
        if (chunkCount == 0) {
            return "No chunks are available for this literature yet";
        }
        if ("SYNCED".equalsIgnoreCase(literature.getVectorSyncStatus())) {
            return "Milvus sync completed with " + literature.getVectorSyncedChunkCount() + " vectors";
        }
        if (literature.getVectorSyncDetail() != null && !literature.getVectorSyncDetail().isBlank()) {
            return literature.getVectorSyncDetail();
        }
        if (embeddedChunkCount == chunkCount) {
            return "Chunking and embedding generation are complete";
        }
        return "Some chunks are missing embeddings, please rerun vectorization";
    }
}
