package com.travel.travel.service;

import com.travel.travel.client.UserServiceClient;
import com.travel.travel.dto.FeedbackGivenSummary;
import com.travel.travel.dto.ParticipationSummary;
import com.travel.travel.dto.ReportMadeSummary;
import com.travel.travel.dto.TravelerProfileResponse;
import com.travel.travel.model.Booking;
import com.travel.travel.model.Feedback;
import com.travel.travel.model.Report;
import com.travel.travel.repository.BookingRepository;
import com.travel.travel.repository.FeedbackRepository;
import com.travel.travel.repository.ReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TravelerProfileService {

    private static final Set<String> TRAVELER_ROLES = Set.of("TRAVELER", "USER");

    private final BookingRepository bookingRepository;
    private final FeedbackRepository feedbackRepository;
    private final ReportRepository reportRepository;
    private final UserServiceClient userServiceClient;

    public TravelerProfileService(BookingRepository bookingRepository,
                                  FeedbackRepository feedbackRepository,
                                  ReportRepository reportRepository,
                                  UserServiceClient userServiceClient) {
        this.bookingRepository = bookingRepository;
        this.feedbackRepository = feedbackRepository;
        this.reportRepository = reportRepository;
        this.userServiceClient = userServiceClient;
    }

    @Transactional(readOnly = true)
    public TravelerProfileResponse getProfile(UUID travelerId) {
        UserServiceClient.UserProfile traveler = userServiceClient.getById(travelerId);
        if (traveler == null || !TRAVELER_ROLES.contains(traveler.role())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyageur introuvable");
        }

        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(travelerId);
        List<ParticipationSummary> participations = bookings.stream()
                .map(b -> new ParticipationSummary(
                        b.getId(),
                        b.getTrip().getId(),
                        b.getTrip().getTitle(),
                        b.getTrip().getOriginCity(),
                        b.getTrip().getDestinationCity(),
                        b.getTrip().getDepartureDate(),
                        b.getTrip().getPrice(),
                        b.getStatus(),
                        b.getCreatedAt()
                ))
                .toList();
        long totalParticipations = bookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus())).count();

        List<Feedback> feedbacks = feedbackRepository.findByUserIdOrderByCreatedAtDesc(travelerId);
        List<FeedbackGivenSummary> feedbackGiven = feedbacks.stream()
                .map(f -> new FeedbackGivenSummary(
                        f.getId(),
                        f.getTrip().getId(),
                        f.getTrip().getTitle(),
                        f.getRating(),
                        f.getComment(),
                        f.getCreatedAt()
                ))
                .toList();

        List<ReportMadeSummary> reportsMade = new ArrayList<>();

        List<Report> managerReports = reportRepository.findByReporterIdOrderByCreatedAtDesc(travelerId);
        for (Report report : managerReports) {
            UserServiceClient.ManagerInfo manager = userServiceClient.getManagerInfo(report.getManagerId());
            reportsMade.add(new ReportMadeSummary(
                    report.getId(),
                    "MANAGER",
                    report.getManagerId(),
                    manager != null ? manager.firstName() : "Inconnu",
                    manager != null ? manager.lastName() : "",
                    null,
                    report.getReason(),
                    report.getStatus(),
                    report.getCreatedAt()
            ));
        }

        List<UserServiceClient.FiledReportView> userReports = userServiceClient.getFiledReports(travelerId);
        for (UserServiceClient.FiledReportView report : userReports) {
            reportsMade.add(new ReportMadeSummary(
                    report.id(),
                    "USER",
                    report.reportedId(),
                    report.reportedFirstName(),
                    report.reportedLastName(),
                    report.tripId(),
                    report.reason(),
                    report.status(),
                    report.createdAt()
            ));
        }

        reportsMade.sort(Comparator.comparing(ReportMadeSummary::createdAt).reversed());

        return new TravelerProfileResponse(
                travelerId,
                traveler.firstName(),
                traveler.lastName(),
                traveler.email(),
                totalParticipations,
                feedbackGiven.size(),
                reportsMade.size(),
                participations,
                feedbackGiven,
                reportsMade
        );
    }
}
