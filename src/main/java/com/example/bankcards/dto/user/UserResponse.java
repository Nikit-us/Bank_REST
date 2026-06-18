package com.example.bankcards.dto.user;

import com.example.bankcards.entity.enums.Role;
import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}
