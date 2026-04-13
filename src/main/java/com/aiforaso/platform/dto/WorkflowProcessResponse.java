package com.aiforaso.platform.dto;

import java.util.List;

public record WorkflowProcessResponse(
        Long literatureId,
        int chunkCount,
        int indicatorCount,
        AnalysisReportResponse report,
        List<LiteratureChunkView> chunks,
        List<IndicatorView> indicators) {
}
