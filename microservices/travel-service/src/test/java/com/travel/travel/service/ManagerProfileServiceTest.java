package com.travel.travel.service;

import com.travel.travel.client.UserServiceClient;
import com.travel.travel.dto.CreateReportRequest;
import com.travel.travel.model.Report;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.BookingRepository;
import com.travel.travel.repository.FeedbackRepository;
import com.travel.travel.repository.ReportRepository;
import com.travel.travel.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ManagerProfileServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private UserServiceClient userServiceClient;

    @InjectMocks
    private ManagerProfileService managerProfileService;

    @Test
    void getProfile_managerNotFound_throws404() {
        UUID managerId = UUID.randomUUID();
        when(userServiceClient.getManagerInfo(managerId)).thenReturn(null);

        assertThatThrownBy(() -> managerProfileService.getProfile(managerId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(NOT_FOUND.value());
    }

    @Test
    void getProfile_notAManager_throws404() {
        UUID managerId = UUID.randomUUID();
        when(userServiceClient.getManagerInfo(managerId))
                .thenReturn(new UserServiceClient.ManagerInfo(managerId, "a@b.com", "John", "Doe", "USER"));

        assertThatThrownBy(() -> managerProfileService.getProfile(managerId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(NOT_FOUND.value());
    }

    @Test
    void getProfile_noTrips_returnsZeroStats() {
        UUID managerId = UUID.randomUUID();
        when(userServiceClient.getManagerInfo(managerId))
                .thenReturn(new UserServiceClient.ManagerInfo(managerId, "m@b.com", "Alice", "Smith", "TRAVEL_MANAGER"));
        when(tripRepository.findByManagerId(managerId)).thenReturn(List.of());
        when(reportRepository.countByManagerId(managerId)).thenReturn(0L);

        var profile = managerProfileService.getProfile(managerId);

        assertThat(profile.totalTrips()).isZero();
        assertThat(profile.totalTravelers()).isZero();
        assertThat(profile.averageRating()).isZero();
        assertThat(profile.reportCount()).isZero();
        assertThat(profile.trips()).isEmpty();
        assertThat(profile.firstName()).isEqualTo("Alice");
        assertThat(profile.lastName()).isEqualTo("Smith");
    }

    @Test
    void getProfile_withTrips_aggregatesStats() {
        UUID managerId = UUID.randomUUID();
        Trip trip = trip(managerId);
        UUID tripId = trip.getId();

        when(userServiceClient.getManagerInfo(managerId))
                .thenReturn(new UserServiceClient.ManagerInfo(managerId, "m@b.com", "Bob", "Martin", "TRAVEL_MANAGER"));
        when(tripRepository.findByManagerId(managerId)).thenReturn(List.of(trip));
        List<Object[]> bookingStats = java.util.Collections.singletonList(new Object[]{tripId, 5L, new BigDecimal("450.00")});
        List<Object[]> feedbackStats = java.util.Collections.singletonList(new Object[]{tripId, 4.2, 3L});
        when(bookingRepository.bookingStatsByTripIds(List.of(tripId), "CONFIRMED")).thenReturn(bookingStats);
        when(feedbackRepository.ratingStatsByTripIds(List.of(tripId))).thenReturn(feedbackStats);
        when(reportRepository.countByManagerId(managerId)).thenReturn(2L);

        var profile = managerProfileService.getProfile(managerId);

        assertThat(profile.totalTrips()).isEqualTo(1);
        assertThat(profile.totalTravelers()).isEqualTo(5L);
        assertThat(profile.averageRating()).isEqualTo(4.2);
        assertThat(profile.reportCount()).isEqualTo(2L);
        assertThat(profile.trips()).hasSize(1);
        assertThat(profile.trips().get(0).confirmedBookings()).isEqualTo(5L);
        assertThat(profile.trips().get(0).feedbackCount()).isEqualTo(3L);
    }

    @Test
    void createReport_selfReport_throws400() {
        UUID managerId = UUID.randomUUID();
        when(userServiceClient.getManagerInfo(managerId))
                .thenReturn(new UserServiceClient.ManagerInfo(managerId, "m@b.com", "Bob", "Martin", "TRAVEL_MANAGER"));

        assertThatThrownBy(() -> managerProfileService.createReport(managerId, managerId,
                new CreateReportRequest("Self report attempt")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(BAD_REQUEST.value());
    }

    @Test
    void createReport_managerNotFound_throws404() {
        UUID managerId = UUID.randomUUID();
        when(userServiceClient.getManagerInfo(managerId)).thenReturn(null);

        assertThatThrownBy(() -> managerProfileService.createReport(managerId, UUID.randomUUID(),
                new CreateReportRequest("Bad service")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(NOT_FOUND.value());
    }

    @Test
    void createReport_alreadyReported_throwsConflict() {
        UUID managerId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        when(userServiceClient.getManagerInfo(managerId))
                .thenReturn(new UserServiceClient.ManagerInfo(managerId, "m@b.com", "Bob", "Martin", "TRAVEL_MANAGER"));
        when(reportRepository.existsByManagerIdAndReporterId(managerId, reporterId)).thenReturn(true);

        assertThatThrownBy(() -> managerProfileService.createReport(managerId, reporterId,
                new CreateReportRequest("Duplicate")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(CONFLICT.value());
    }

    @Test
    void createReport_valid_savesAndReturnsReport() {
        UUID managerId = UUID.randomUUID();
        UUID reporterId = UUID.randomUUID();
        when(userServiceClient.getManagerInfo(managerId))
                .thenReturn(new UserServiceClient.ManagerInfo(managerId, "m@b.com", "Bob", "Martin", "TRAVEL_MANAGER"));
        when(reportRepository.existsByManagerIdAndReporterId(managerId, reporterId)).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = managerProfileService.createReport(managerId, reporterId,
                new CreateReportRequest("Unprofessional behaviour"));

        assertThat(result.managerId()).isEqualTo(managerId);
        assertThat(result.reporterId()).isEqualTo(reporterId);
        assertThat(result.reason()).isEqualTo("Unprofessional behaviour");
        verify(reportRepository).save(any(Report.class));
    }

    private static Trip trip(UUID managerId) {
        Trip trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setTitle("Paris → Tokyo");
        trip.setOriginCity("Paris");
        trip.setDestinationCity("Tokyo");
        trip.setDepartureDate(LocalDate.now().plusDays(30));
        trip.setPrice(new BigDecimal("650.00"));
        trip.setSeatsAvailable(20);
        trip.setStatus("ACTIVE");
        trip.setManagerId(managerId);
        return trip;
    }
}
