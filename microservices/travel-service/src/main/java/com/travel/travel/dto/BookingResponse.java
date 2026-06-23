package com.travel.travel.dto;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID tripId,
        UUID userId,
        String status,
        UUID paymentId,
        Instant createdAt
) {
}
