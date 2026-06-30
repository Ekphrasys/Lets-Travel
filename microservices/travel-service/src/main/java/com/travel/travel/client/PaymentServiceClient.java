package com.travel.travel.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentServiceClient {

    private final WebClient webClient;
    private final String internalApiKey;

    public PaymentServiceClient(WebClient.Builder webClientBuilder,
                                @Value("${internal.api-key}") String internalApiKey) {
        this.webClient = webClientBuilder.baseUrl("http://payment-service").build();
        this.internalApiKey = internalApiKey;
    }

    public PaymentResult createPayment(UUID bookingId, UUID userId, BigDecimal amount, String paymentMethod) {
        return webClient.post()
                .uri("/api/payments/internal")
                .header("X-Internal-Key", internalApiKey)
                .bodyValue(Map.of(
                        "bookingId", bookingId,
                        "userId", userId,
                        "amount", amount,
                        "paymentMethod", paymentMethod != null ? paymentMethod : "CARD"
                ))
                .retrieve()
                .bodyToMono(PaymentResult.class)
                .block();
    }

    public PaymentResult refund(UUID paymentId) {
        return webClient.post()
                .uri("/api/payments/internal/{id}/refund", paymentId)
                .header("X-Internal-Key", internalApiKey)
                .retrieve()
                .bodyToMono(PaymentResult.class)
                .block();
    }

    public record PaymentResult(UUID id, UUID bookingId, UUID userId, BigDecimal amount, String status, Instant createdAt) {
    }
}
