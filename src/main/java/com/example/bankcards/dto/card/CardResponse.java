package com.example.bankcards.dto.card;

import com.example.bankcards.entity.enums.CardStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CardResponse(
        Long id,
        String maskedNumber,
        Long ownerId,
        String ownerUsername,
        LocalDate expiryDate,
        CardStatus status,
        BigDecimal balance,
        Instant createdAt
) {
}
