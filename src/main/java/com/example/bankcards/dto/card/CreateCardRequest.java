package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCardRequest(
        @Schema(description = "Full card number, 13-19 digits", example = "4111111111111111")
        @NotNull @Pattern(regexp = "\\d{13,19}", message = "card number must be 13-19 digits")
        String cardNumber,

        @Schema(description = "Id of the user who will own the card", example = "2")
        @NotNull Long ownerId,

        @Schema(description = "Expiry date (must be in the future)", example = "2030-12-31")
        @NotNull @Future LocalDate expiryDate,

        @Schema(description = "Initial balance; defaults to 0 when omitted", example = "1000.00")
        @PositiveOrZero BigDecimal initialBalance
) {
}
