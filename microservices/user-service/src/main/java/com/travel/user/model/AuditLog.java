package com.travel.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", schema = "user", indexes = {
        @Index(name = "idx_audit_logs_user", columnList = "principal_user_id"),
        @Index(name = "idx_audit_logs_action", columnList = "action"),
        @Index(name = "idx_audit_logs_created", columnList = "created_at")
})
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "principal_user_id")
    private UUID principalUserId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPrincipalUserId() {
        return principalUserId;
    }

    public void setPrincipalUserId(UUID principalUserId) {
        this.principalUserId = principalUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
