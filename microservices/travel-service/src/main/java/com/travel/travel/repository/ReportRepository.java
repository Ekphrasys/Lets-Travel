package com.travel.travel.repository;

import com.travel.travel.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    long countByManagerId(UUID managerId);

    boolean existsByManagerIdAndReporterId(UUID managerId, UUID reporterId);

    List<Report> findByManagerIdOrderByCreatedAtDesc(UUID managerId);

    List<Report> findAllByOrderByCreatedAtDesc();
}
