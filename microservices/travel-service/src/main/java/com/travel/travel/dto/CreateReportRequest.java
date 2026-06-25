package com.travel.travel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotBlank(message = "La raison ne peut pas être vide")
        @Size(max = 1000, message = "La raison ne doit pas dépasser 1000 caractères")
        String reason
) {}
