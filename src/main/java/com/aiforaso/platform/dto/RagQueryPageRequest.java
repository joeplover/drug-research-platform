package com.aiforaso.platform.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RagQueryPageRequest(
        @NotBlank String query,
        @Min(1) @Max(100) Integer topK,
        List<Long> literatureIds,
        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size) {
}
