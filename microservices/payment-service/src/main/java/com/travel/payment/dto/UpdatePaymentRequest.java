package com.travel.payment.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdatePaymentRequest(
        @Positive BigDecimal amount,
        String status
) {}
