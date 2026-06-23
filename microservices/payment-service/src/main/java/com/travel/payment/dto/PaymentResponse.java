package com.travel.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String status,
        Instant createdAt
) {
}
