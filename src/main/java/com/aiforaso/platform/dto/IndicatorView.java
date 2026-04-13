package com.aiforaso.platform.dto;

import java.math.BigDecimal;

public record IndicatorView(
        Long id,
        Long literatureId,
        String indicatorName,
        String category,
        String timeWindow,
        String cohort,
        BigDecimal observedValue,
        BigDecimal confidenceScore,
        String evidenceSnippet,
        String evidenceLocator,
        String reviewStatus,
        String reviewerNote,
        java.time.LocalDateTime reviewedAt) {
}
