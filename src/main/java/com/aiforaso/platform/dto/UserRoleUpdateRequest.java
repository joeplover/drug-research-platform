package com.aiforaso.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record UserRoleUpdateRequest(
        @NotBlank String role,
        @NotBlank String status) {
}
