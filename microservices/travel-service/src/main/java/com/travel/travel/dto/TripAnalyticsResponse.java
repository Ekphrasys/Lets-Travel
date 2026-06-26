package com.travel.travel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TripAnalyticsResponse(
        UUID id,
        String title,
        String originCity,
        String destinationCity,
        LocalDate departureDate,
        BigDecimal price,
        int seatsAvailable,
        String status,
        long confirmedBookings,
        BigDecimal revenue,
        double occupancyRate,
        double averageRating,
        long feedbackCount
) {}
