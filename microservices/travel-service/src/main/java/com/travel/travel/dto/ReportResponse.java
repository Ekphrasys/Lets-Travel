package com.travel.travel.dto;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID managerId,
        UUID reporterId,
        String reason,
        Instant createdAt
) {}
