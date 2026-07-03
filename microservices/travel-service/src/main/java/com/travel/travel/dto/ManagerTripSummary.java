package com.travel.travel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ManagerTripSummary(
        UUID id,
        String title,
        String originCity,
        String destinationCity,
        LocalDate departureDate,
        BigDecimal price,
        String status,
        long confirmedBookings,
        double averageRating,
        long feedbackCount
) {}
