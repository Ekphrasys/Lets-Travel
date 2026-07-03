package com.travel.user.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminReportView(
        UUID id,
        UUID reporterId,
        String reporterFirstName,
        String reporterLastName,
        String reporterEmail,
        UUID reportedId,
        String reportedFirstName,
        String reportedLastName,
        String reportedEmail,
        String reportedRole,
        UUID tripId,
        String reason,
        String status,
        Instant createdAt
) {}
