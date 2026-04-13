package com.aiforaso.platform.dto;

import java.util.List;

public record LiteratureOverviewView(
        Long literatureId,
        String title,
        String processingStage,
        int chunkCount,
        int indicatorCount,
        String overviewSummary,
        String researchFocus,
        String methodSummary,
        String resultSummary,
        String safetySummary,
        String conclusionSummary,
        List<String> keyPoints,
        List<String> keyConcepts,
        List<String> evidenceHighlights,
        List<IndicatorView> keyIndicators,
        KnowledgeGraphResponse graph) {
}
