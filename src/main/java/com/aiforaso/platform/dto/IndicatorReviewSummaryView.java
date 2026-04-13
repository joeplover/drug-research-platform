package com.aiforaso.platform.dto;

public record IndicatorReviewSummaryView(
        long totalCount,
        long pendingCount,
        long confirmedCount,
        long rejectedCount) {
}
