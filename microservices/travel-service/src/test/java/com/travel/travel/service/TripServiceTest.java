package com.travel.travel.service;

import com.travel.travel.dto.CreateTripRequest;
import com.travel.travel.model.Trip;
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

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private TripService tripService;

    @Test
    void create_persistsActiveTrip() {
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = tripService.create(new CreateTripRequest(
                "Paris-Lyon", "Paris", "Lyon", LocalDate.of(2026, 12, 1),
                new BigDecimal("99.00"), 10));

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.title()).isEqualTo("Paris-Lyon");
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

        assertThatThrownBy(() -> tripService.update(id, sampleRequest()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void delete_notFound() {
        UUID id = UUID.randomUUID();
        when(tripRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> tripService.delete(id))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void delete_success() {
        UUID id = UUID.randomUUID();
        when(tripRepository.existsById(id)).thenReturn(true);

        tripService.delete(id);

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
