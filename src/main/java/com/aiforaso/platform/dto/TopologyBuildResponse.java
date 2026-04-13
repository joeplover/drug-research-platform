package com.aiforaso.platform.dto;

import java.util.List;

public record TopologyBuildResponse(
        Long taskId,
        Long literatureId,
        String indicatorName,
        List<IndicatorStateView> states,
        List<StateTransitionView> transitions,
        KnowledgeGraphResponse graph) {
}
