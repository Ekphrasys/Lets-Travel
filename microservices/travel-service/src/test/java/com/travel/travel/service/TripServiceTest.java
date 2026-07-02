package com.travel.travel.service;

import com.travel.travel.dto.CreateTripRequest;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.TripRepository;
import com.travel.travel.repository.BookingRepository;
import com.travel.travel.repository.FeedbackRepository;
import com.travel.travel.search.TripSearchService;
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

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private TripSearchService tripSearchService;

    @Mock
    private TripGraphService tripGraphService;

    @InjectMocks
    private TripService tripService;

    @Test
    void create_persistsActiveTrip() {
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID managerId = UUID.randomUUID();

        var response = tripService.create(new CreateTripRequest(
                "Paris-Lyon", "Paris", "Lyon", LocalDate.of(2026, 12, 1),
                new BigDecimal("99.00"), 10), managerId);

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.title()).isEqualTo("Paris-Lyon");
        assertThat(response.managerId()).isEqualTo(managerId);
    }

    @Test
    void getById_notFound() {
        UUID id = UUID.randomUUID();
        when(tripRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getById(id))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void findAll_returnsTrips() {
        Trip trip = trip(UUID.randomUUID());
        when(tripRepository.findAll()).thenReturn(List.of(trip));

        assertThat(tripService.findAll()).hasSize(1);
    }

    @Test
    void update_notFound() {
        UUID id = UUID.randomUUID();
        when(tripRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.update(id, sampleRequest(), UUID.randomUUID(), true))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void update_forbidden_whenNotOwner() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Trip existing = trip(id);
        existing.setManagerId(ownerId);
        when(tripRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tripService.update(id, sampleRequest(), callerId, false))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void delete_notFound() {
        UUID id = UUID.randomUUID();
        when(tripRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.delete(id, UUID.randomUUID(), true))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void delete_success() {
        UUID id = UUID.randomUUID();
        Trip trip = trip(id);
        when(tripRepository.findById(id)).thenReturn(Optional.of(trip));

        tripService.delete(id, UUID.randomUUID(), true);

        verify(tripRepository).deleteById(id);
    }

    private static CreateTripRequest sampleRequest() {
        return new CreateTripRequest("T", "A", "B", LocalDate.now(), BigDecimal.TEN, 5);
    }

    private static Trip trip(UUID id) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setTitle("T");
        trip.setOriginCity("A");
        trip.setDestinationCity("B");
        trip.setDepartureDate(LocalDate.now());
        trip.setPrice(BigDecimal.TEN);
        trip.setSeatsAvailable(5);
        trip.setStatus("ACTIVE");
        return trip;
    }
}
