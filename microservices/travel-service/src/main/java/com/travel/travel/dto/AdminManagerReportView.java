package com.travel.travel.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminManagerReportView(
        UUID id,
        UUID managerId,
        String managerFirstName,
        String managerLastName,
        String managerEmail,
        UUID reporterId,
        String reporterFirstName,
        String reporterLastName,
        String reporterEmail,
        String reason,
        String status,
        Instant createdAt
) {}
