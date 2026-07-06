package com.travel.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserConsentResponse(
        UUID id,
        UUID userId,
        String consentType,
        String version,
        Instant acceptedAt,
        String ipAddress,
        String userAgent
) {
}
