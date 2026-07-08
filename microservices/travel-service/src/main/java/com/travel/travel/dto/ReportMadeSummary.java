package com.travel.travel.dto;

import java.time.Instant;
import java.util.UUID;

public record ReportMadeSummary(
        UUID reportId,
        String targetType,
        UUID targetId,
        String targetFirstName,
        String targetLastName,
        UUID tripId,
        String reason,
        String status,
        Instant createdAt
) {}
