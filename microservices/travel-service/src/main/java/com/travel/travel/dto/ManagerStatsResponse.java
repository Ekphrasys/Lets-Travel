package com.travel.travel.dto;

import java.math.BigDecimal;

public record ManagerStatsResponse(
        long totalTrips,
        long totalTravelers,
        BigDecimal totalIncome
) {}
