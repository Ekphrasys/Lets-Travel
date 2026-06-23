package com.travel.travel.dto;

import java.math.BigDecimal;
import java.util.List;

public record RouteResponse(
        List<String> cities,
        int totalDurationMin,
        BigDecimal totalPrice
) {
}
