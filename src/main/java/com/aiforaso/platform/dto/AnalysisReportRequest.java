package com.aiforaso.platform.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record AnalysisReportRequest(
        @NotBlank String question,
        List<Long> literatureIds,
        String analysisFocus,
        Boolean onlyConfirmedIndicators) {
}
