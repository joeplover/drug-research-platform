package com.aiforaso.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LiteraturePreparationService {

    private final LiteratureIngestionService literatureIngestionService;
    private final KnowledgeExtractionService knowledgeExtractionService;

    public LiteraturePreparationService(
            LiteratureIngestionService literatureIngestionService,
            KnowledgeExtractionService knowledgeExtractionService) {
        this.literatureIngestionService = literatureIngestionService;
        this.knowledgeExtractionService = knowledgeExtractionService;
    }

    @Transactional
    public void prepareImportedLiterature(Long literatureId) {
        var chunks = literatureIngestionService.list(literatureId);
        if (chunks.isEmpty()) {
            chunks = literatureIngestionService.ingest(literatureId);
        }

        var vectorStatus = literatureIngestionService.vectorStatus(literatureId);
        int chunkCount = vectorStatus.chunkCount() == null ? 0 : vectorStatus.chunkCount();
        int embeddedChunkCount = vectorStatus.embeddedChunkCount() == null ? 0 : vectorStatus.embeddedChunkCount();
        if (chunkCount > 0 && embeddedChunkCount < chunkCount) {
            literatureIngestionService.vectorize(literatureId);
        }

        if (knowledgeExtractionService.listByLiterature(literatureId).isEmpty()) {
            knowledgeExtractionService.extractIndicatorsByChunks(literatureId);
        }
    }
}
