package com.aiforaso.platform.dto;

public record TopologyBuildRequest(
        String indicatorName,
        boolean rebuildIndicators) {
}
