package com.aiforaso.platform.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record BatchLiteratureImportRequest(
        @NotEmpty List<@Valid LiteratureImportRequest> items,
        boolean autoIngest) {
}
