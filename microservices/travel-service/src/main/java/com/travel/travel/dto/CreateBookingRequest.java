package com.travel.travel.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateBookingRequest(@NotNull UUID tripId) {
}
