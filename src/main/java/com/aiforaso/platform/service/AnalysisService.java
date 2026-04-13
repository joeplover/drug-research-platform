package com.aiforaso.platform.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.aiforaso.platform.domain.AnalysisTask;
import com.aiforaso.platform.dto.AnalysisReportRequest;
import com.aiforaso.platform.dto.AnalysisReportResponse;
import com.aiforaso.platform.dto.IndicatorExtractionRequest;
import com.aiforaso.platform.dto.IndicatorView;
import com.aiforaso.platform.dto.LiteratureOverviewView;
import com.aiforaso.platform.dto.RagHitView;
import com.aiforaso.platform.dto.RagQueryRequest;
import com.aiforaso.platform.repository.AnalysisTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AnalysisService {

    private final RagService ragService;
    private final KnowledgeExtractionService knowledgeExtractionService;
    private final AnalysisTaskRepository analysisTaskRepository;
    private final LiteratureService literatureService;
    private final AiReportClient aiReportClient;
    private final LiteratureOverviewService literatureOverviewService;
    private final ObjectMapper objectMapper;

    public AnalysisService(
            RagService ragService,
            KnowledgeExtractionService knowledgeExtractionService,
            AnalysisTaskRepository analysisTaskRepository,
            LiteratureService literatureService,
            AiReportClient aiReportClient,
            LiteratureOverviewService literatureOverviewService,
            ObjectMapper objectMapper) {
        this.ragService = ragService;
        this.knowledgeExtractionService = knowledgeExtractionService;
        this.analysisTaskRepository = analysisTaskRepository;
        this.literatureService = literatureService;
        this.aiReportClient = aiReportClient;
        this.literatureOverviewService = literatureOverviewService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalysisReportResponse generate(AnalysisReportRequest request) {
        List<RagHitView> evidence = ragService.retrieve(new RagQueryRequest(request.question(), 5, request.literatureIds()));
        if (evidence.isEmpty()) {
            evidence = buildFallbackEvidence(request.literatureIds());
        }
        List<IndicatorView> indicators = collectIndicators(request);
        String sourceContext = buildSourceContext(request.literatureIds());
        String reportSummary = aiReportClient.generateReport(
                request.question(),
                request.analysisFocus(),
                evidence,
                indicators,
                sourceContext);

        AnalysisTask task = new AnalysisTask();
        task.setTaskType("REPORT");
        task.setStatus("COMPLETED");
        task.setInputText(request.question());
        task.setResultSummary(reportSummary);
        task.setAnalysisFocus(request.analysisFocus());
        task.setCitations(evidence.stream()
                .map(hit -> hit.title() + (hit.evidenceLocator() == null || hit.evidenceLocator().isBlank()
                        ? ""
                        : "@" + hit.evidenceLocator()))
                .distinct()
                .collect(Collectors.joining(" | ")));
        task.setContextLiteratureIds(request.literatureIds() == null ? "" : request.literatureIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
        task.setEvidenceJson(writeJson(evidence));
        task.setIndicatorsJson(writeJson(indicators));
        task = analysisTaskRepository.save(task);

        return new AnalysisReportResponse(task.getId(), task.getStatus(), reportSummary, evidence, indicators);
    }

    private List<IndicatorView> collectIndicators(AnalysisReportRequest request) {
        if (request.literatureIds() == null || request.literatureIds().isEmpty()) {
            return new ArrayList<>();
        }

        List<IndicatorView> indicators = new ArrayList<>();
        boolean onlyConfirmed = Boolean.TRUE.equals(request.onlyConfirmedIndicators());
        for (Long literatureId : request.literatureIds()) {
            List<IndicatorView> allIndicators = knowledgeExtractionService.listByLiterature(literatureId);
            List<IndicatorView> existingIndicators = onlyConfirmed
                    ? allIndicators.stream().filter(item -> "已确认".equalsIgnoreCase(item.reviewStatus())).toList()
                    : allIndicators;
            if (!existingIndicators.isEmpty() || !allIndicators.isEmpty()) {
                indicators.addAll(existingIndicators);
                continue;
            }

            List<IndicatorView> extractedIndicators = knowledgeExtractionService.extractIndicatorsByChunks(literatureId);
            if (extractedIndicators.isEmpty()) {
                var literature = literatureService.getEntity(literatureId);
                extractedIndicators = knowledgeExtractionService.extractIndicators(
                        new IndicatorExtractionRequest(literatureId, literature.getSummary(), "overall", "unspecified"));
            }
            indicators.addAll(onlyConfirmed
                    ? extractedIndicators.stream().filter(item -> "已确认".equalsIgnoreCase(item.reviewStatus())).toList()
                    : extractedIndicators);
        }
        return indicators;
    }

    private String buildSourceContext(List<Long> literatureIds) {
        if (literatureIds == null || literatureIds.isEmpty()) {
            return "";
        }

        return literatureIds.stream()
                .map(this::safeBuildOverview)
                .filter(item -> item != null)
                .map(this::formatOverviewContext)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n\n"));
    }

    private LiteratureOverviewView safeBuildOverview(Long literatureId) {
        try {
            return literatureOverviewService.build(literatureId);
        } catch (Exception exception) {
            return null;
        }
    }

    private String formatOverviewContext(LiteratureOverviewView overview) {
        StringBuilder builder = new StringBuilder();
        builder.append("文献：").append(overview.title()).append("\n");
        builder.append("研究目标：").append(safeText(overview.researchFocus(), "暂无解析结果")).append("\n");
        builder.append("研究方法：").append(safeText(overview.methodSummary(), "暂无解析结果")).append("\n");
        builder.append("主要结果：").append(safeText(overview.resultSummary(), "暂无解析结果")).append("\n");
        builder.append("安全性：").append(safeText(overview.safetySummary(), "暂无解析结果")).append("\n");
        builder.append("研究意义：").append(safeText(overview.conclusionSummary(), "暂无解析结果"));
        if (overview.evidenceHighlights() != null && !overview.evidenceHighlights().isEmpty()) {
            builder.append("\n关键证据：")
                    .append(overview.evidenceHighlights().stream().limit(3).collect(Collectors.joining(" | ")));
        }
        return builder.toString();
    }

    private List<RagHitView> buildFallbackEvidence(List<Long> literatureIds) {
        if (literatureIds == null || literatureIds.isEmpty()) {
            return List.of();
        }

        List<RagHitView> hits = new ArrayList<>();
        for (Long literatureId : literatureIds) {
            LiteratureOverviewView overview = safeBuildOverview(literatureId);
            if (overview == null) {
                continue;
            }
            if (overview.evidenceHighlights() != null && !overview.evidenceHighlights().isEmpty()) {
                for (String snippet : overview.evidenceHighlights().stream().limit(3).toList()) {
                    hits.add(new RagHitView(
                            overview.literatureId(),
                            overview.title(),
                            snippet,
                            0.35,
                            "OVERVIEW",
                            null,
                            "overview",
                            "OVERVIEW_FALLBACK"));
                }
                continue;
            }

            if (StringUtils.hasText(overview.overviewSummary())) {
                hits.add(new RagHitView(
                        overview.literatureId(),
                        overview.title(),
                        overview.overviewSummary(),
                        0.2,
                        "OVERVIEW",
                        null,
                        "overview",
                        "OVERVIEW_FALLBACK"));
            }
        }
        return hits.stream().limit(5).toList();
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize analysis task payload.", exception);
        }
    }
}
