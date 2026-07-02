package com.travel.travel.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdminDashboardResponse(
        Map<String, BigDecimal> incomeByMonth,
        BigDecimal totalIncome,
        long totalTrips,
        List<TripResponse> topTrips,
        List<FeedbackResponse> recentFeedbacks,
        List<ManagerPerformance> managersPerformance
) {
    public record ManagerPerformance(
            UUID managerId,
            String name,
            String email,
            long tripsCount,
            BigDecimal income,
            double averageRating,
            long feedbackCount,
            double performanceScore
    ) {}
}
