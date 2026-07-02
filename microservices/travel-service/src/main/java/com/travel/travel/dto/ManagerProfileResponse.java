package com.travel.travel.dto;

import java.util.List;
import java.util.UUID;

public record ManagerProfileResponse(
        UUID managerId,
        String firstName,
        String lastName,
        int totalTrips,
        long totalTravelers,
        double averageRating,
        long reportCount,
        List<ManagerTripSummary> trips
) {}
