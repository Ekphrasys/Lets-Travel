package com.travel.payment.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Component
public class CreditCardProvider implements PaymentProvider {

    private static final String PREFIX = "cc_";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, BigDecimal> captured = new HashMap<>();

    @Override
    public PaymentResult process(PaymentContext context) {
        try {
            Thread.sleep(400 + RANDOM.nextInt(300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String txnId = PREFIX + System.nanoTime() + "_" + RANDOM.nextInt(9000 + 1000);
        if (isDeclined(context.amount())) {
            return new PaymentResult(false, txnId, "DECLINED", "Fonds insuffisants ou carte refusée par l'émetteur");
        }
        return new PaymentResult(true, txnId, "CAPTURED", null);
    }

    @Override
    public PaymentResult capture(String providerTransactionId, BigDecimal amount) {
        if (!providerTransactionId.startsWith(PREFIX)) {
            return new PaymentResult(false, providerTransactionId, "FAILED", "Transaction introuvable");
        }
        captured.put(providerTransactionId, amount);
        return new PaymentResult(true, providerTransactionId, "CAPTURED", null);
    }

    @Override
    public PaymentResult refund(String providerTransactionId, BigDecimal amount) {
        if (!providerTransactionId.startsWith(PREFIX)) {
            return new PaymentResult(false, providerTransactionId, "FAILED", "Transaction introuvable");
        }
        captured.remove(providerTransactionId);
        return new PaymentResult(true, providerTransactionId, "REFUNDED", null);
    }

    private boolean isDeclined(BigDecimal amount) {
        if (amount == null) return false;
        int cents = amount.setScale(2, RoundingMode.HALF_UP).unscaledValue().intValue();
        return cents % 7 == 0;
    }
}
