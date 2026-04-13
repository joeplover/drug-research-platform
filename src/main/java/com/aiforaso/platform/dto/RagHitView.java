package com.aiforaso.platform.dto;

public record RagHitView(
        Long literatureId,
        String title,
        String snippet,
        double score,
        String sourceType,
        String publicationDate,
        String evidenceLocator,
        String retrievalMode) {
}
