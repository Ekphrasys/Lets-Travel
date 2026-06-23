package com.travel.travel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTripRequest(
        @NotBlank String title,
        @NotBlank String originCity,
        @NotBlank String destinationCity,
        @NotNull LocalDate departureDate,
        @NotNull @Positive BigDecimal price,
        @NotNull @Positive Integer seatsAvailable
) {
}
