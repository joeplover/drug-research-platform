package com.aiforaso.platform.dto;

import java.time.LocalDateTime;

public record UserView(
        Long id,
        String username,
        String email,
        String role,
        String status,
        LocalDateTime createdAt) {
}
