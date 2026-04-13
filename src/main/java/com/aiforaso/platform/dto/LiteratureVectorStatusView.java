package com.aiforaso.platform.dto;

public record LiteratureVectorStatusView(
        Long literatureId,
        Integer chunkCount,
        Integer embeddedChunkCount,
        String milvusStatus,
        String embeddingMode,
        String message) {
}
