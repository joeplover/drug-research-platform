package com.aiforaso.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AnalysisTaskDetailView(
        Long id,
        String taskType,
        String status,
        String inputText,
        String resultSummary,
        String citations,
        String contextLiteratureIds,
        String analysisFocus,
        List<RagHitView> evidence,
        List<IndicatorView> indicators,
        LocalDateTime createdAt) {
}
