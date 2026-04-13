package com.aiforaso.platform.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LiteratureImportRequest(
        @NotBlank @Size(max = 500) String storagePath,
        @Size(max = 300) String title,
        @Size(max = 64) String sourceType,
        @Size(max = 120) String diseaseArea,
        @Size(max = 1000) String keywords,
        LocalDate publicationDate) {
}
