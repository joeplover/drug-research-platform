package com.aiforaso.platform.dto;

import java.time.LocalDateTime;

public record AnalysisTaskView(
        Long id,
        String taskType,
        String status,
        String inputText,
        String resultSummary,
        String citations,
        String contextLiteratureIds,
        LocalDateTime createdAt) {
}
