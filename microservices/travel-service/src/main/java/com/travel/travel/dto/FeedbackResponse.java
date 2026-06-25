package com.travel.travel.dto;

import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        UUID tripId,
        UUID userId,
        String userEmail,
        String userFirstName,
        String userLastName,
        int rating,
        String comment,
        Instant createdAt
) {}
