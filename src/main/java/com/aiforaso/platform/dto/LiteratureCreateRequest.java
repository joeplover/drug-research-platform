package com.aiforaso.platform.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LiteratureCreateRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 64) String sourceType,
        @NotBlank @Size(max = 120) String diseaseArea,
        @NotBlank @Size(max = 4000) String summary,
        @Size(max = 1000) String keywords,
        LocalDate publicationDate,
        @Size(max = 300) String storagePath) {
}
