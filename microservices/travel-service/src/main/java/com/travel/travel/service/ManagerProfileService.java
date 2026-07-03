package com.travel.travel.service;

import com.travel.travel.client.UserServiceClient;
import com.travel.travel.dto.AdminManagerReportView;
import com.travel.travel.dto.CreateReportRequest;
import com.travel.travel.dto.ManagerProfileResponse;
import com.travel.travel.dto.ManagerTripSummary;
import com.travel.travel.dto.ReportDetailResponse;
import com.travel.travel.dto.ReportResponse;
import com.travel.travel.model.Report;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.BookingRepository;
import com.travel.travel.repository.FeedbackRepository;
import com.travel.travel.repository.ReportRepository;
import com.travel.travel.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ManagerProfileService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final FeedbackRepository feedbackRepository;
    private final ReportRepository reportRepository;
    private final UserServiceClient userServiceClient;

    public ManagerProfileService(TripRepository tripRepository,
                                 BookingRepository bookingRepository,
                                 FeedbackRepository feedbackRepository,
                                 ReportRepository reportRepository,
                                 UserServiceClient userServiceClient) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.feedbackRepository = feedbackRepository;
        this.reportRepository = reportRepository;
        this.userServiceClient = userServiceClient;
    }

    public ManagerProfileResponse getProfile(UUID managerId) {
        UserServiceClient.ManagerInfo managerInfo = userServiceClient.getManagerInfo(managerId);
        if (managerInfo == null || !"TRAVEL_MANAGER".equals(managerInfo.role())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager introuvable");
        }

        List<Trip> trips = tripRepository.findByManagerId(managerId);
        List<UUID> tripIds = trips.stream().map(Trip::getId).toList();

        Map<UUID, Long> bookingCounts = new HashMap<>();
        if (!tripIds.isEmpty()) {
            bookingRepository.bookingStatsByTripIds(tripIds, "CONFIRMED").forEach(row -> {
                UUID tid = (UUID) row[0];
                bookingCounts.put(tid, ((Number) row[1]).longValue());
            });
        }

        Map<UUID, Double> avgRatings = new HashMap<>();
        Map<UUID, Long> feedbackCounts = new HashMap<>();
        if (!tripIds.isEmpty()) {
            feedbackRepository.ratingStatsByTripIds(tripIds).forEach(row -> {
                UUID tid = (UUID) row[0];
                avgRatings.put(tid, ((Number) row[1]).doubleValue());
                feedbackCounts.put(tid, ((Number) row[2]).longValue());
            });
        }

        List<ManagerTripSummary> tripSummaries = trips.stream().map(trip -> {
            UUID tid = trip.getId();
            long confirmed = bookingCounts.getOrDefault(tid, 0L);
            double avgRating = avgRatings.getOrDefault(tid, 0.0);
            long fbCount = feedbackCounts.getOrDefault(tid, 0L);
            return new ManagerTripSummary(
                    tid, trip.getTitle(), trip.getOriginCity(), trip.getDestinationCity(),
                    trip.getDepartureDate(), trip.getPrice(), trip.getStatus(),
                    confirmed, avgRating, fbCount
            );
        }).toList();

        long totalTravelers = bookingCounts.values().stream().mapToLong(Long::longValue).sum();

        double overallAvgRating = avgRatings.isEmpty() ? 0.0
                : avgRatings.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        long reportCount = reportRepository.countByManagerId(managerId);

        return new ManagerProfileResponse(
                managerId,
                managerInfo.firstName(),
                managerInfo.lastName(),
                trips.size(),
                totalTravelers,
                overallAvgRating,
                reportCount,
                tripSummaries
        );
    }

    public List<ReportDetailResponse> getMyReports(UUID managerId) {
        return reportRepository.findByManagerIdOrderByCreatedAtDesc(managerId).stream()
                .map(report -> {
                    UserServiceClient.ManagerInfo reporter = userServiceClient.getManagerInfo(report.getReporterId());
                    String firstName = reporter != null ? reporter.firstName() : "Utilisateur";
                    String lastName = reporter != null ? reporter.lastName() : "inconnu";
                    return new ReportDetailResponse(
                            report.getId(),
                            report.getReporterId(),
                            firstName,
                            lastName,
                            report.getReason(),
                            report.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public ReportResponse createReport(UUID managerId, UUID reporterId, CreateReportRequest request) {
        UserServiceClient.ManagerInfo managerInfo = userServiceClient.getManagerInfo(managerId);
        if (managerInfo == null || !"TRAVEL_MANAGER".equals(managerInfo.role())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager introuvable");
        }

        if (managerId.equals(reporterId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas vous signaler vous-même");
        }

        if (reportRepository.existsByManagerIdAndReporterId(managerId, reporterId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vous avez déjà signalé ce manager");
        }

        Report report = new Report();
        report.setId(UUID.randomUUID());
        report.setManagerId(managerId);
        report.setReporterId(reporterId);
        report.setReason(request.reason());
        report = reportRepository.save(report);

        return new ReportResponse(report.getId(), report.getManagerId(),
                report.getReporterId(), report.getReason(), report.getCreatedAt());
    }

    public List<AdminManagerReportView> findAllReportsForAdmin() {
        return reportRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(report -> {
                    UserServiceClient.UserProfile manager = userServiceClient.getById(report.getManagerId());
                    UserServiceClient.UserProfile reporter = userServiceClient.getById(report.getReporterId());
                    return new AdminManagerReportView(
                            report.getId(),
                            report.getManagerId(),
                            manager != null ? manager.firstName() : "Inconnu",
                            manager != null ? manager.lastName() : "",
                            manager != null ? manager.email() : "",
                            report.getReporterId(),
                            reporter != null ? reporter.firstName() : "Inconnu",
                            reporter != null ? reporter.lastName() : "",
                            reporter != null ? reporter.email() : "",
                            report.getReason(),
                            report.getStatus(),
                            report.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public AdminManagerReportView resolveReport(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signalement introuvable"));
        report.setStatus("RESOLVED");
        report = reportRepository.save(report);
        UserServiceClient.UserProfile manager = userServiceClient.getById(report.getManagerId());
        UserServiceClient.UserProfile reporter = userServiceClient.getById(report.getReporterId());
        return new AdminManagerReportView(
                report.getId(),
                report.getManagerId(),
                manager != null ? manager.firstName() : "Inconnu",
                manager != null ? manager.lastName() : "",
                manager != null ? manager.email() : "",
                report.getReporterId(),
                reporter != null ? reporter.firstName() : "Inconnu",
                reporter != null ? reporter.lastName() : "",
                reporter != null ? reporter.email() : "",
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
