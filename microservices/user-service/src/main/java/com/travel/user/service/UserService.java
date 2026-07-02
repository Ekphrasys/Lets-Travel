package com.travel.user.service;

import com.travel.user.dto.*;
import com.travel.user.model.Report;
import com.travel.user.model.User;
import com.travel.user.model.UserConsent;
import com.travel.user.model.AuditLog;
import com.travel.user.repository.ReportRepository;
import com.travel.user.repository.UserRepository;
import com.travel.user.repository.UserConsentRepository;
import com.travel.user.repository.AuditLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final UserConsentRepository userConsentRepository;
    private final AuditLogRepository auditLogRepository;

    public UserService(
            UserRepository userRepository,
            ReportRepository reportRepository,
            jakarta.persistence.EntityManager entityManager,
            UserConsentRepository userConsentRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.entityManager = entityManager;
        this.userConsentRepository = userConsentRepository;
        this.auditLogRepository = auditLogRepository;
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
    public UserResponse updateMe(UUID id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
        }
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        return toResponse(userRepository.save(user));
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
    public void anonymizeAndDelete(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        String anonymizedEmail = "anonymized-" + userId.toString() + "@deleted.invalid";
        Instant now = Instant.now();

        user.setEmail(anonymizedEmail);
        user.setFirstName("Deleted");
        user.setLastName("Deleted");
        user.setRole("DELETED");
        userRepository.save(user);

        try {
            entityManager.createNativeQuery(
                    "UPDATE auth.users_auth SET email = :email, password_hash = 'GDPR_DELETED' WHERE id = :id"
            ).setParameter("email", anonymizedEmail).setParameter("id", userId).executeUpdate();
        } catch (Exception e) {
        }

        try {
            entityManager.createNativeQuery(
                    "UPDATE travel.bookings SET user_id = NULL WHERE user_id = :userId"
            ).setParameter("userId", userId).executeUpdate();
        } catch (Exception e) {
        }

        try {
            entityManager.createNativeQuery(
                    "UPDATE travel.feedbacks SET user_id = NULL, comment = NULL WHERE user_id = :userId"
            ).setParameter("userId", userId).executeUpdate();
        } catch (Exception e) {
        }

        try {
            entityManager.createNativeQuery(
                    "UPDATE payment.payments SET user_id = NULL WHERE user_id = :userId"
            ).setParameter("userId", userId).executeUpdate();
        } catch (Exception e) {
        }

        try {
            entityManager.createNativeQuery(
                    "UPDATE user.reports SET reporter_id = NULL WHERE reporter_id = :userId"
            ).setParameter("userId", userId).executeUpdate();
        } catch (Exception e) {
        }
    }

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
        }
        anonymizeAndDelete(id);
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

    public List<ReportResponse> findAllReports() {
        return reportRepository.findAll().stream().map(this::toReportResponse).toList();
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

    @Transactional(readOnly = true)
    public UserDataExportResponse exportUserData(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        UserDataExportResponse.UserProfileExport profile = new UserDataExportResponse.UserProfileExport(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getCreatedAt()
        );

        List<UserDataExportResponse.BookingExport> bookingExports = new ArrayList<>();
        try {
            List<?> bookingResults = entityManager.createNativeQuery(
                    "SELECT id, trip_id, status, payment_id, created_at FROM travel.bookings WHERE user_id = :userId"
            ).setParameter("userId", userId).getResultList();
            for (Object row : bookingResults) {
                Object[] cols = (Object[]) row;
                bookingExports.add(new UserDataExportResponse.BookingExport(
                        cols[0].toString(),
                        cols[1].toString(),
                        (String) cols[2],
                        cols[3] != null ? cols[3].toString() : null,
                        ((java.sql.Timestamp) cols[4]).toInstant()
                ));
            }
        } catch (Exception e) {
        }

        List<UserDataExportResponse.FeedbackExport> feedbackExports = new ArrayList<>();
        try {
            List<?> feedbackResults = entityManager.createNativeQuery(
                    "SELECT id, trip_id, rating, comment, created_at FROM travel.feedbacks WHERE user_id = :userId"
            ).setParameter("userId", userId).getResultList();
            for (Object row : feedbackResults) {
                Object[] cols = (Object[]) row;
                feedbackExports.add(new UserDataExportResponse.FeedbackExport(
                        cols[0].toString(),
                        cols[1].toString(),
                        ((Number) cols[2]).intValue(),
                        (String) cols[3],
                        ((java.sql.Timestamp) cols[4]).toInstant()
                ));
            }
        } catch (Exception e) {
        }

        List<UserDataExportResponse.PaymentExport> paymentExports = new ArrayList<>();
        try {
            List<?> paymentResults = entityManager.createNativeQuery(
                    "SELECT id, booking_id, status, payment_method, created_at FROM payment.payments WHERE user_id = :userId"
            ).setParameter("userId", userId).getResultList();
            for (Object row : paymentResults) {
                Object[] cols = (Object[]) row;
                paymentExports.add(new UserDataExportResponse.PaymentExport(
                        cols[0].toString(),
                        cols[1].toString(),
                        (String) cols[2],
                        (String) cols[3],
                        ((java.sql.Timestamp) cols[4]).toInstant()
                ));
            }
        } catch (Exception e) {
        }

        List<UserDataExportResponse.ReportExport> filedExports = new ArrayList<>();
        try {
            List<Report> reports = reportRepository.findByReporterId(userId);
            for (Report r : reports) {
                filedExports.add(new UserDataExportResponse.ReportExport(
                        r.getId().toString(),
                        r.getReporterId().toString(),
                        r.getTripId() != null ? r.getTripId().toString() : null,
                        r.getReason(),
                        r.getStatus(),
                        r.getCreatedAt()
                ));
            }
        } catch (Exception e) {
        }

        List<UserDataExportResponse.ReportExport> receivedExports = new ArrayList<>();
        try {
            List<Report> reports = reportRepository.findByReportedId(userId);
            for (Report r : reports) {
                receivedExports.add(new UserDataExportResponse.ReportExport(
                        r.getId().toString(),
                        r.getReporterId().toString(),
                        r.getTripId() != null ? r.getTripId().toString() : null,
                        r.getReason(),
                        r.getStatus(),
                        r.getCreatedAt()
                ));
            }
        } catch (Exception e) {
        }

        return new UserDataExportResponse(profile, bookingExports, feedbackExports, paymentExports, filedExports, receivedExports);
    }

    @Transactional
    public UserConsentResponse recordConsent(UUID userId, UserConsentRequest request) {
        UserConsent consent = new UserConsent();
        consent.setId(UUID.randomUUID());
        consent.setUserId(userId);
        consent.setConsentType(request.consentType());
        consent.setVersion(request.version());
        consent.setIpAddress(currentRequestIp());
        consent.setUserAgent(currentRequestUserAgent());
        userConsentRepository.save(consent);

        audit("CONSENT_ACCEPTED", userId, consent.getId().toString(), "OK");

        return toConsentResponse(consent);
    }

    public List<UserConsentResponse> listConsents(UUID userId) {
        return userConsentRepository.findByUserIdOrderByAcceptedAtDesc(userId).stream()
                .map(this::toConsentResponse)
                .toList();
    }

    private UserConsentResponse toConsentResponse(UserConsent c) {
        return new UserConsentResponse(
                c.getId(), c.getUserId(), c.getConsentType(), c.getVersion(),
                c.getAcceptedAt(), c.getIpAddress(), c.getUserAgent()
        );
    }

    public void audit(String action, UUID principalUserId, String targetId, String status) {
        try {
            AuditLog log = new AuditLog();
            log.setId(UUID.randomUUID());
            log.setPrincipalUserId(principalUserId != null ? principalUserId : null);
            log.setAction(action);
            log.setTargetId(targetId);
            log.setStatus(status);
            log.setIpAddress(currentRequestIp());
            log.setUserAgent(currentRequestUserAgent());
            auditLogRepository.save(log);
        } catch (Exception e) {
        }
    }

    private String currentRequestIp() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String xf = request.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isEmpty()) {
                return xf.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String currentRequestUserAgent() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
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
