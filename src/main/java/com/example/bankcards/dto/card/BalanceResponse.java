package com.example.bankcards.dto.card;

import java.math.BigDecimal;

public record BalanceResponse(Long cardId, BigDecimal balance) {
}
