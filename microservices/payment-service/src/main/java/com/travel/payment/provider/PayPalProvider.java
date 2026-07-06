package com.travel.payment.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;

@Component
public class PayPalProvider implements PaymentProvider {

    private static final String PREFIX = "pp_";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public PaymentResult process(PaymentContext context) {
        try {
            Thread.sleep(500 + RANDOM.nextInt(700));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String txnId = PREFIX + System.nanoTime() + "_" + RANDOM.nextInt(9000 + 1000);
        if (isDeclined(context.amount())) {
            return new PaymentResult(false, txnId, "DENIED", "PayPal : transaction refusée par l'acheteur ou le compte");
        }
        return new PaymentResult(true, txnId, "APPROVED", null);
    }

    @Override
    public PaymentResult capture(String providerTransactionId, BigDecimal amount) {
        if (!providerTransactionId.startsWith(PREFIX)) {
            return new PaymentResult(false, providerTransactionId, "FAILED", "Transaction PayPal introuvable");
        }
        return new PaymentResult(true, providerTransactionId, "CAPTURED", null);
    }

    @Override
    public PaymentResult refund(String providerTransactionId, BigDecimal amount) {
        if (!providerTransactionId.startsWith(PREFIX)) {
            return new PaymentResult(false, providerTransactionId, "FAILED", "Transaction PayPal introuvable");
        }
        return new PaymentResult(true, providerTransactionId, "REFUNDED", null);
    }

    private boolean isDeclined(BigDecimal amount) {
        if (amount == null) return false;
        int cents = amount.setScale(2, RoundingMode.HALF_UP).unscaledValue().intValue();
        return cents % 11 == 0;
    }
}
