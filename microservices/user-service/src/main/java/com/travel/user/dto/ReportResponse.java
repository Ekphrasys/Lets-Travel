package com.travel.user.dto;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID reporterId,
        UUID reportedId,
        UUID tripId,
        String reason,
        String status,
        Instant createdAt
) {}
