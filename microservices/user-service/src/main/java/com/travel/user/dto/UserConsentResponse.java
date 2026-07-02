package com.travel.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
