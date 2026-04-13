package com.aiforaso.platform.dto;

public record UserSessionView(
        Long userId,
        String username,
        String email,
        String role,
        String token) {
}
