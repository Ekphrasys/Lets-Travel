package com.travel.user.dto;

public record UserStatsResponse(
        long pastTravelParticipation,
        long reportsFiled,
        long reportsReceived,
        long subscriptionCancellations,
        String preferredPaymentMethod
) {}
