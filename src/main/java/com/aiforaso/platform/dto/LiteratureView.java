package com.aiforaso.platform.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LiteratureView(
        Long id,
        String title,
        String sourceType,
        String diseaseArea,
        String summary,
        String keywords,
        LocalDate publicationDate,
        String storagePath,
        String vectorSyncStatus,
        String vectorSyncDetail,
        Integer vectorSyncedChunkCount,
        LocalDateTime vectorSyncedAt,
        LocalDateTime createdAt,
        Long createdBy) {
}
