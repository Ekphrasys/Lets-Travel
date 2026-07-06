package com.travel.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_events", indexes = {
        @Index(name = "idx_payment_events_payment", columnList = "payment_id"),
        @Index(name = "idx_payment_events_status", columnList = "status"),
        @Index(name = "idx_payment_events_created", columnList = "created_at")
})
public class PaymentEvent {

    public enum EventType {
        CREATED, PROCESSING, COMPLETED, FAILED, REFUNDED, WEBHOOK_RECEIVED
    }

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EventType status;

    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId;

    @Column(length = 255)
    private String reason;

    @Column(name = "provider_type", length = 20)
    private String providerType;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public EventType getStatus() { return status; }
    public void setStatus(EventType status) { this.status = status; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public void setProviderTransactionId(String providerTransactionId) { this.providerTransactionId = providerTransactionId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
