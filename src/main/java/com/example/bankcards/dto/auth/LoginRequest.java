package com.example.bankcards.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "admin")
        @NotBlank String username,

        @Schema(example = "Admin12345")
        @NotBlank String password
) {
}
