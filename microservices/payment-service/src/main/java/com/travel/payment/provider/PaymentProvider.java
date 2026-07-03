package com.travel.payment.provider;

import java.math.BigDecimal;

public interface PaymentProvider {

    String TYPE_CARD = "CARD";
    String TYPE_PAYPAL = "PAYPAL";

    record PaymentResult(
            boolean success,
            String providerTransactionId,
            String providerStatus,
            String failedReason
    ) {}

    record PaymentContext(
            String bookingId,
            String userId,
            BigDecimal amount
    ) {}

    PaymentResult process(PaymentContext context);

    PaymentResult capture(String providerTransactionId, BigDecimal amount);

    PaymentResult refund(String providerTransactionId, BigDecimal amount);
}
