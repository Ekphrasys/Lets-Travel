package com.travel.travel.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID tripId,
        String tripTitle,
        UUID userId,
        String status,
        UUID paymentId,
        Instant createdAt,
        LocalDate tripDepartureDate
) {
}
