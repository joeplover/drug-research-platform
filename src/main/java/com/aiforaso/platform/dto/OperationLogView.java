package com.aiforaso.platform.dto;

import java.time.LocalDateTime;

public record OperationLogView(
        Long id,
        String operatorEmail,
        String actionType,
        String resourceType,
        String resourceId,
        String detail,
        LocalDateTime createdAt) {
}
