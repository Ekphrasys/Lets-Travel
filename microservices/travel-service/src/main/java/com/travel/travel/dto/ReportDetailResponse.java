package com.travel.travel.dto;

import java.time.Instant;
import java.util.UUID;

public record ReportDetailResponse(
        UUID id,
        UUID reporterId,
        String reporterFirstName,
        String reporterLastName,
        String reason,
        Instant createdAt
) {}
