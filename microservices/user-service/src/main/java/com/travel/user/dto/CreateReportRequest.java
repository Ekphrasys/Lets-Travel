package com.travel.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateReportRequest(
        @NotNull UUID reportedId,
        UUID tripId,
        @NotNull String reason
) {}
