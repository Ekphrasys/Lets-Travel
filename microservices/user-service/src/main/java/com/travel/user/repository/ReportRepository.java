package com.travel.user.repository;

import com.travel.user.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByReportedId(UUID reportedId);
    List<Report> findByReporterId(UUID reporterId);
    long countByReportedId(UUID reportedId);
    long countByReporterId(UUID reporterId);
}
