package com.aiforaso.platform.dto;

public record IndicatorStateView(
        Long id,
        Long literatureId,
        String indicatorName,
        String stageType,
        Integer stateOrder,
        String stateLabel,
        String description,
        String evidenceLocator) {
}
