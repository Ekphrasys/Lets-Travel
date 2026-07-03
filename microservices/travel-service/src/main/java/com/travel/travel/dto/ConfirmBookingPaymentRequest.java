package com.travel.travel.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmBookingPaymentRequest(
        @NotBlank String clientSecret
) {
}
