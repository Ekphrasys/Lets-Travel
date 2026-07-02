package com.travel.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentIntentResponse(
        UUID paymentId,
        String clientSecret,
        BigDecimal amount,
        String currency,
        String status
) {
}
