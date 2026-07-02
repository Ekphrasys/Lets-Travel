package com.travel.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserConsentRequest(
        @NotBlank @Size(min = 1, max = 50) String consentType,
        @NotBlank @Size(min = 1, max = 20) String version
) {
}
