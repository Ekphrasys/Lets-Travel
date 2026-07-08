package com.travel.user.dto;

import java.time.Instant;
import java.util.UUID;

public record FiledReportView(
        UUID id,
        UUID reportedId,
        String reportedFirstName,
        String reportedLastName,
        UUID tripId,
        String reason,
        String status,
        Instant createdAt
) {}
