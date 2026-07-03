package com.travel.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID bookingId,
        @NotNull UUID userId,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Size(min = 1, max = 20) String paymentMethod
) {}
