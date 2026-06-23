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

    public UserService(UserRepository userRepository, ReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
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

    public List<ReportResponse> findAllReports() {
        return reportRepository.findAll().stream().map(this::toReportResponse).toList();
    }

    public ReportCountsResponse getReportCounts(UUID userId) {
        long filed = reportRepository.countByReporterId(userId);
        long received = reportRepository.countByReportedId(userId);
        return new ReportCountsResponse(filed, received);
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
