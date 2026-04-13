package com.aiforaso.platform.dto;

public record HealthComponentView(
        String name,
        String status,
        String detail,
        Long latencyMs) {
}
