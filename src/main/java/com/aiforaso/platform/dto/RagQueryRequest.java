package com.aiforaso.platform.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RagQueryRequest(
        @NotBlank String query,
        @Min(1) @Max(10) Integer topK,
        List<Long> literatureIds) {
}
