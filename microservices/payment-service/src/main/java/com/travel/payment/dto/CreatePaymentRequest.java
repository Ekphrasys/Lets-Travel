package com.travel.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID bookingId,
        @NotNull UUID userId,
        @NotNull @Positive BigDecimal amount,
        String paymentMethod
) {
}
