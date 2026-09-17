package com.example.wallet.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey,

        @NotBlank(message = "fromWalletId is required")
        String fromWalletId,

        @NotBlank(message = "toWalletId is required")
        String toWalletId,

        @NotNull(message = "amount is required")
        @DecimalMin(
                value = "0.01",
                message = "amount must be greater than zero"
        )
        @Digits(
                integer = 17,
                fraction = 2,
                message = "amount must have at most 17 integer digits and 2 decimal places"
        )
        BigDecimal amount
) {
}