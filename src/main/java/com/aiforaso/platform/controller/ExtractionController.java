package com.aiforaso.platform.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aiforaso.platform.dto.IndicatorExtractionRequest;
import com.aiforaso.platform.dto.IndicatorReviewRequest;
import com.aiforaso.platform.dto.IndicatorReviewSummaryView;
import com.aiforaso.platform.dto.IndicatorView;
import com.aiforaso.platform.service.KnowledgeExtractionService;
import com.aiforaso.platform.service.OperationLogService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/extractions")
public class ExtractionController {

    private final KnowledgeExtractionService knowledgeExtractionService;
    private final OperationLogService operationLogService;

    public ExtractionController(
            KnowledgeExtractionService knowledgeExtractionService,
            OperationLogService operationLogService) {
        this.knowledgeExtractionService = knowledgeExtractionService;
        this.operationLogService = operationLogService;
    }

    @PostMapping("/indicators")
    public List<IndicatorView> extractIndicators(@Valid @RequestBody IndicatorExtractionRequest request) {
        List<IndicatorView> indicators = knowledgeExtractionService.extractIndicators(request);
        operationLogService.record("system", "EXTRACT", "LITERATURE", String.valueOf(request.literatureId()), "Ran direct indicator extraction");
        return indicators;
    }

    @GetMapping("/literatures/{literatureId}/indicators")
    public List<IndicatorView> listIndicators(@PathVariable Long literatureId) {
        return knowledgeExtractionService.listByLiterature(literatureId);
    }

    @GetMapping("/literatures/{literatureId}/indicators/page")
    public Page<IndicatorView> listIndicatorsPaginated(
            @PathVariable Long literatureId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return knowledgeExtractionService.listByLiteraturePaginated(literatureId, page, size);
    }

    @GetMapping("/summary")
    public IndicatorReviewSummaryView reviewSummary() {
        return knowledgeExtractionService.reviewSummary();
    }

    @PostMapping("/literatures/{literatureId}/run")
    public List<IndicatorView> extractFromChunks(@PathVariable Long literatureId) {
        List<IndicatorView> indicators = knowledgeExtractionService.extractIndicatorsByChunks(literatureId);
        operationLogService.record("system", "EXTRACT_CHUNKS", "LITERATURE", String.valueOf(literatureId), "Ran chunk-based indicator extraction");
        return indicators;
    }

    @PostMapping("/indicators/{indicatorId}/review")
    public IndicatorView reviewIndicator(@PathVariable Long indicatorId, @Valid @RequestBody IndicatorReviewRequest request) {
        IndicatorView indicator = knowledgeExtractionService.reviewIndicator(indicatorId, request.reviewStatus(), request.reviewerNote());
        operationLogService.record("system", "REVIEW_INDICATOR", "INDICATOR", String.valueOf(indicatorId), "Updated indicator review status to " + request.reviewStatus());
        return indicator;
    }

    @PostMapping("/literatures/{literatureId}/indicators/review-all")
    public int reviewAllIndicators(
            @PathVariable Long literatureId,
            @RequestParam String reviewStatus,
            @RequestParam(required = false) String reviewerNote) {
        int count = knowledgeExtractionService.reviewAllByLiterature(literatureId, reviewStatus, reviewerNote);
        operationLogService.record("system", "REVIEW_ALL_INDICATORS", "LITERATURE", String.valueOf(literatureId), 
                "Batch reviewed " + count + " indicators to " + reviewStatus);
        return count;
    }
}
