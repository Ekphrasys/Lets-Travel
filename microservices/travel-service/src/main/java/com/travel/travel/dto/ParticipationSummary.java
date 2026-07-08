package com.travel.travel.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ParticipationSummary(
        UUID bookingId,
        UUID tripId,
        String tripTitle,
        String originCity,
        String destinationCity,
        LocalDate departureDate,
        BigDecimal price,
        String status,
        Instant createdAt
) {}
