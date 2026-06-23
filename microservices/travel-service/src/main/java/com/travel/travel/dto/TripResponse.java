package com.travel.travel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TripResponse(
        UUID id,
        String title,
        String originCity,
        String destinationCity,
        LocalDate departureDate,
        BigDecimal price,
        int seatsAvailable,
        String status
) {
}
