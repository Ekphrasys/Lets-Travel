package com.travel.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdatePaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank String status
) {
}
