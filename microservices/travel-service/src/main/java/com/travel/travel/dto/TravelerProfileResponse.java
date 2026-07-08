package com.travel.travel.dto;

import java.util.List;
import java.util.UUID;

public record TravelerProfileResponse(
        UUID travelerId,
        String firstName,
        String lastName,
        String email,
        long totalParticipations,
        long totalFeedbackGiven,
        long totalReportsFiled,
        List<ParticipationSummary> participations,
        List<FeedbackGivenSummary> feedbackGiven,
        List<ReportMadeSummary> reportsMade
) {}
