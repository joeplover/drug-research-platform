package com.aiforaso.platform.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record HealthView(
        String status,
        String service,
        OffsetDateTime timestamp,
        List<HealthComponentView> components) {
}
