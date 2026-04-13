package com.aiforaso.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record IndicatorReviewRequest(
        @NotBlank String reviewStatus,
        String reviewerNote) {
}
