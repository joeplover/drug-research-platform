package com.aiforaso.platform.dto;

import java.util.List;

public record AnalysisReportResponse(
        Long taskId,
        String taskStatus,
        String reportSummary,
        List<RagHitView> evidence,
        List<IndicatorView> indicators) {
}
