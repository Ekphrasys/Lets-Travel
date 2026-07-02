package com.travel.user.repository;

import com.travel.user.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByPrincipalUserIdOrderByCreatedAtDesc(UUID principalUserId);

    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
}
