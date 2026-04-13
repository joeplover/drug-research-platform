package com.aiforaso.platform.dto;

public record LiteratureChunkView(
        Long id,
        Long literatureId,
        Integer chunkIndex,
        String sourceSection,
        String chunkLabel,
        String content) {
}
