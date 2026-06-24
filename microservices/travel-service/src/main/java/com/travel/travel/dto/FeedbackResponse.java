package com.travel.travel.dto;

import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        UUID tripId,
        UUID userId,
        int rating,
        String comment,
        Instant createdAt
) {
}
