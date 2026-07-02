package com.travel.travel.service;

import com.travel.travel.dto.CreateFeedbackRequest;
import com.travel.travel.model.Feedback;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.BookingRepository;
import com.travel.travel.repository.FeedbackRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    void create_tripNotFound_throws404() {
        UUID userId = UUID.randomUUID();
        CreateFeedbackRequest request = new CreateFeedbackRequest(UUID.randomUUID(), 4, "Great trip!");
        when(tripRepository.findById(request.tripId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.create(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(NOT_FOUND.value());
    }

    @Test
    void create_noConfirmedBooking_throwsForbidden() {
        UUID userId = UUID.randomUUID();
        Trip trip = trip(LocalDate.now().minusDays(1));
        CreateFeedbackRequest request = new CreateFeedbackRequest(trip.getId(), 3, null);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(bookingRepository.existsByTripIdAndUserIdAndStatus(trip.getId(), userId, "CONFIRMED"))
                .thenReturn(false);

        assertThatThrownBy(() -> feedbackService.create(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(FORBIDDEN.value());
    }

    @Test
    void create_alreadySubmittedFeedback_throwsConflict() {
        UUID userId = UUID.randomUUID();
        Trip trip = trip(LocalDate.now().minusDays(1));
        CreateFeedbackRequest request = new CreateFeedbackRequest(trip.getId(), 5, "Loved it");
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(bookingRepository.existsByTripIdAndUserIdAndStatus(trip.getId(), userId, "CONFIRMED"))
                .thenReturn(true);
        when(feedbackRepository.existsByTripIdAndUserId(trip.getId(), userId)).thenReturn(true);

        assertThatThrownBy(() -> feedbackService.create(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(CONFLICT.value());
    }

    @Test
    void create_validParticipant_savesFeedback() {
        UUID userId = UUID.randomUUID();
        Trip trip = trip(LocalDate.now().minusDays(5));
        CreateFeedbackRequest request = new CreateFeedbackRequest(trip.getId(), 5, "Amazing!");
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(bookingRepository.existsByTripIdAndUserIdAndStatus(trip.getId(), userId, "CONFIRMED"))
                .thenReturn(true);
        when(feedbackRepository.existsByTripIdAndUserId(trip.getId(), userId)).thenReturn(false);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = feedbackService.create(userId, request);

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Amazing!");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.tripId()).isEqualTo(trip.getId());
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    void create_withoutComment_savesFeedback() {
        UUID userId = UUID.randomUUID();
        Trip trip = trip(LocalDate.now().minusDays(2));
        CreateFeedbackRequest request = new CreateFeedbackRequest(trip.getId(), 3, null);
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(bookingRepository.existsByTripIdAndUserIdAndStatus(trip.getId(), userId, "CONFIRMED"))
                .thenReturn(true);
        when(feedbackRepository.existsByTripIdAndUserId(trip.getId(), userId)).thenReturn(false);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = feedbackService.create(userId, request);

        assertThat(response.comment()).isNull();
        assertThat(response.rating()).isEqualTo(3);
    }

    @Test
    void findByUser_returnsFeedbacks() {
        UUID userId = UUID.randomUUID();
        Trip trip = trip(LocalDate.now().minusDays(10));
        Feedback feedback = feedback(userId, trip, 4, "Good");
        when(feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(feedback));

        var result = feedbackService.findByUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rating()).isEqualTo(4);
        assertThat(result.get(0).comment()).isEqualTo("Good");
    }

    @Test
    void findByUser_noFeedbacks_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        assertThat(feedbackService.findByUser(userId)).isEmpty();
    }

    @Test
    void findByTrip_nonManagerNonAdmin_throwsForbidden() {
        UUID callerId = UUID.randomUUID();
        Trip trip = trip(LocalDate.now().minusDays(1));
        trip.setManagerId(UUID.randomUUID());
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> feedbackService.findByTrip(trip.getId(), callerId, false))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(FORBIDDEN.value());
    }

    @Test
    void findByTrip_admin_returnsFeedbacks() {
        UUID adminId = UUID.randomUUID();
        Trip trip = trip(LocalDate.now().minusDays(1));
        trip.setManagerId(UUID.randomUUID());
        Feedback feedback = feedback(UUID.randomUUID(), trip, 5, "Great");
        when(tripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(feedbackRepository.findByTripIdOrderByCreatedAtDesc(trip.getId())).thenReturn(List.of(feedback));

        var result = feedbackService.findByTrip(trip.getId(), adminId, true);

        assertThat(result).hasSize(1);
    }

    private static Trip trip(LocalDate departureDate) {
        Trip trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setTitle("Test Trip");
        trip.setOriginCity("Paris");
        trip.setDestinationCity("London");
        trip.setDepartureDate(departureDate);
        trip.setPrice(new BigDecimal("100.00"));
        trip.setSeatsAvailable(10);
        trip.setStatus("ACTIVE");
        return trip;
    }

    private static Feedback feedback(UUID userId, Trip trip, int rating, String comment) {
        Feedback f = new Feedback();
        f.setId(UUID.randomUUID());
        f.setUserId(userId);
        f.setTrip(trip);
        f.setRating(rating);
        f.setComment(comment);
        return f;
    }
}
