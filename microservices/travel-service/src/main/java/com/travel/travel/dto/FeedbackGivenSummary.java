package com.travel.travel.dto;

import java.time.Instant;
import java.util.UUID;

public record FeedbackGivenSummary(
        UUID feedbackId,
        UUID tripId,
        String tripTitle,
        int rating,
        String comment,
        Instant createdAt
) {}
