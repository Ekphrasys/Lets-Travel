package com.travel.travel.dto;

import java.math.BigDecimal;
import java.util.List;

public record ManagerDashboardResponse(
        long tripsCount,
        long travelersCount,
        BigDecimal totalIncome,
        List<FeedbackResponse> feedbacks
) {}
