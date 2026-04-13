package com.aiforaso.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 6, max = 64) String password,
        @NotBlank @Size(max = 40) String role) {
}
