package com.aiforaso.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record IndicatorExtractionRequest(
        Long literatureId,
        @NotBlank String content,
        String cohort,
        String timeWindow) {
}
