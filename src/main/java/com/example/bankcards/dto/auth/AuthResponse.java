package com.example.bankcards.dto.auth;

import com.example.bankcards.entity.enums.Role;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String username,
        Role role
) {
    public static AuthResponse bearer(String token, long expiresInMs, String username, Role role) {
        return new AuthResponse(token, "Bearer", expiresInMs, username, role);
    }
}
