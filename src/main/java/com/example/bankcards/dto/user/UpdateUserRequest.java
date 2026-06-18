package com.example.bankcards.dto.user;

import com.example.bankcards.entity.enums.Role;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 150) String fullName,
        Role role,
        Boolean enabled,
        @Size(min = 8, max = 100) String password
) {
}
