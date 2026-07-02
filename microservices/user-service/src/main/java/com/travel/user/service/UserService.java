package com.travel.user.service;

import com.travel.user.dto.*;
import com.travel.user.model.Report;
import com.travel.user.model.User;
import com.travel.user.repository.ReportRepository;
import com.travel.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public UserService(
            UserRepository userRepository,
            ReportRepository reportRepository,
            jakarta.persistence.EntityManager entityManager
    ) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
        }
        User user = new User();
        user.setId(request.id());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.role() != null ? request.role() : "USER");
        return toResponse(userRepository.save(user));
    }

    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    public UserResponse getByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
        }
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.role());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
        }
        userRepository.deleteById(id);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole());
    }

    public List<UserResponse> findManagers() {
        return userRepository.findAll().stream()
                .filter(u -> "MANAGER".equalsIgnoreCase(u.getRole()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReportResponse createReport(UUID reporterId, CreateReportRequest request) {
        if (!userRepository.existsById(request.reportedId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur signalé introuvable");
        }
        Report report = new Report();
        report.setId(UUID.randomUUID());
        report.setReporterId(reporterId);
        report.setReportedId(request.reportedId());
        report.setTripId(request.tripId());
        report.setReason(request.reason());
        report.setStatus("PENDING");
        report = reportRepository.save(report);
        return toReportResponse(report);
    }

    @Transactional
    public ReportResponse resolveReport(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signalement introuvable"));
        report.setStatus("RESOLVED");
        report = reportRepository.save(report);
        return toReportResponse(report);
    }

    public List<AdminReportView> findAllReports() {
        return reportRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Report::getCreatedAt).reversed())
                .map(report -> {
                    User reporter = userRepository.findById(report.getReporterId()).orElse(null);
                    User reported = userRepository.findById(report.getReportedId()).orElse(null);
                    return new AdminReportView(
                            report.getId(),
                            report.getReporterId(),
                            reporter != null ? reporter.getFirstName() : "Inconnu",
                            reporter != null ? reporter.getLastName() : "",
                            reporter != null ? reporter.getEmail() : "",
                            report.getReportedId(),
                            reported != null ? reported.getFirstName() : "Inconnu",
                            reported != null ? reported.getLastName() : "",
                            reported != null ? reported.getEmail() : "",
                            reported != null ? reported.getRole() : "",
                            report.getTripId(),
                            report.getReason(),
                            report.getStatus(),
                            report.getCreatedAt()
                    );
                })
                .toList();
    }

    public ReportCountsResponse getReportCounts(UUID userId) {
        long filed = reportRepository.countByReporterId(userId);
        long received = reportRepository.countByReportedId(userId);
        return new ReportCountsResponse(filed, received);
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
        }

        long pastTravelParticipation = 0;
        try {
            Number count = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM travel.bookings WHERE user_id = :userId AND status = 'CONFIRMED'"
            ).setParameter("userId", userId).getSingleResult();
            pastTravelParticipation = count.longValue();
        } catch (Exception e) {
            // Ignore
        }

        long subscriptionCancellations = 0;
        try {
            Number count = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM travel.bookings WHERE user_id = :userId AND status = 'CANCELLED'"
            ).setParameter("userId", userId).getSingleResult();
            subscriptionCancellations = count.longValue();
        } catch (Exception e) {
            // Ignore
        }

        long reportsFiled = reportRepository.countByReporterId(userId);
        long reportsReceived = reportRepository.countByReportedId(userId);

        String preferredPaymentMethod = "Aucun";
        try {
            List<?> resultList = entityManager.createNativeQuery(
                    "SELECT payment_method FROM payment.payments WHERE user_id = :userId AND status = 'COMPLETED' GROUP BY payment_method ORDER BY COUNT(*) DESC LIMIT 1"
            ).setParameter("userId", userId).getResultList();
            if (!resultList.isEmpty()) {
                preferredPaymentMethod = resultList.get(0).toString();
            }
        } catch (Exception e) {
            // Ignore
        }

        return new UserStatsResponse(
                pastTravelParticipation,
                reportsFiled,
                reportsReceived,
                subscriptionCancellations,
                preferredPaymentMethod
        );
    }

    private ReportResponse toReportResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReporterId(),
                report.getReportedId(),
                report.getTripId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
