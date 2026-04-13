package com.aiforaso.platform.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiforaso.platform.dto.AnalysisReportRequest;
import com.aiforaso.platform.dto.AnalysisReportResponse;
import com.aiforaso.platform.dto.IndicatorView;
import com.aiforaso.platform.dto.LiteratureChunkView;
import com.aiforaso.platform.dto.WorkflowProcessRequest;
import com.aiforaso.platform.dto.WorkflowProcessResponse;

@Service
public class WorkflowService {

    private final LiteratureIngestionService literatureIngestionService;
    private final KnowledgeExtractionService knowledgeExtractionService;
    private final AnalysisService analysisService;

    public WorkflowService(
            LiteratureIngestionService literatureIngestionService,
            KnowledgeExtractionService knowledgeExtractionService,
            AnalysisService analysisService) {
        this.literatureIngestionService = literatureIngestionService;
        this.knowledgeExtractionService = knowledgeExtractionService;
        this.analysisService = analysisService;
    }

    @Transactional
    public WorkflowProcessResponse processLiterature(Long literatureId, WorkflowProcessRequest request) {
        List<LiteratureChunkView> chunks = request.reingest()
                ? reingestAndVectorize(literatureId)
                : existingOrPrepare(literatureId);

        List<IndicatorView> indicators = request.reextract()
                ? knowledgeExtractionService.extractIndicatorsByChunks(literatureId)
                : existingOrExtract(literatureId);

        String question = request.question() == null || request.question().isBlank()
                ? "Summarize the main evidence and indicators for this literature."
                : request.question();

        AnalysisReportResponse report = analysisService.generate(
                new AnalysisReportRequest(question, List.of(literatureId), request.analysisFocus(), request.onlyConfirmedIndicators()));

        return new WorkflowProcessResponse(literatureId, chunks.size(), indicators.size(), report, chunks, indicators);
    }

    private List<LiteratureChunkView> existingOrPrepare(Long literatureId) {
        List<LiteratureChunkView> existing = literatureIngestionService.list(literatureId);
        if (existing.isEmpty()) {
            return reingestAndVectorize(literatureId);
        }

        var vectorStatus = literatureIngestionService.vectorStatus(literatureId);
        if (vectorStatus.chunkCount() == null || vectorStatus.chunkCount() == 0) {
            return reingestAndVectorize(literatureId);
        }
        if (vectorStatus.embeddedChunkCount() == null || vectorStatus.embeddedChunkCount() < vectorStatus.chunkCount()) {
            return literatureIngestionService.vectorize(literatureId);
        }
        return existing;
    }

    private List<LiteratureChunkView> reingestAndVectorize(Long literatureId) {
        literatureIngestionService.ingest(literatureId);
        return literatureIngestionService.vectorize(literatureId);
    }

    private List<IndicatorView> existingOrExtract(Long literatureId) {
        List<IndicatorView> existing = knowledgeExtractionService.listByLiterature(literatureId);
        return existing.isEmpty() ? knowledgeExtractionService.extractIndicatorsByChunks(literatureId) : existing;
    }
}
