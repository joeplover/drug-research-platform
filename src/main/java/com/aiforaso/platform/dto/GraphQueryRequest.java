package com.aiforaso.platform.dto;

public record GraphQueryRequest(
        Long literatureId,
        String keyword,
        String nodeType,
        String reviewStatus) {
}
