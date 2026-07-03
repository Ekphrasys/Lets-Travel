package com.travel.payment.controller;

import com.travel.payment.model.Payment;
import com.travel.payment.model.PaymentEvent;
import com.travel.payment.repository.PaymentEventRepository;
import com.travel.payment.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/webhook")
public class PaymentWebhookController {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    public PaymentWebhookController(PaymentRepository paymentRepository, PaymentEventRepository paymentEventRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    @PostMapping("/{provider}/simulate")
    public ResponseEntity<?> simulate(
            @PathVariable String provider,
            @RequestBody Map<String, String> body) {
        UUID paymentId = UUID.fromString(body.getOrDefault("paymentId", ""));
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        String eventType = body.getOrDefault("event", "COMPLETED");
        if ("COMPLETED".equalsIgnoreCase(eventType)) {
            payment.setStatus("COMPLETED");
            payment.setProviderStatus("CAPTURED");
        } else if ("FAILED".equalsIgnoreCase(eventType)) {
            payment.setStatus("FAILED");
            payment.setFailedReason(body.getOrDefault("reason", "Simulated failure"));
        } else if ("REFUNDED".equalsIgnoreCase(eventType)) {
            payment.setStatus("REFUNDED");
            payment.setProviderStatus("REFUNDED");
        }

        paymentRepository.save(payment);

        PaymentEvent event = new PaymentEvent();
        event.setId(UUID.randomUUID());
        event.setPaymentId(paymentId);
        event.setStatus(PaymentEvent.EventType.WEBHOOK_RECEIVED);
        event.setProviderType(provider);
        event.setHttpStatus(200);
        event.setPayload(body.toString());
        event.setProviderTransactionId(payment.getProviderTransactionId());
        paymentEventRepository.save(event);

        return ResponseEntity.ok(Map.of("status", "OK", "paymentStatus", payment.getStatus()));
    }
}
