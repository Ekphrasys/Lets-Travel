package com.travel.travel.service;

import com.travel.travel.dto.CreateTripRequest;
import com.travel.travel.dto.ManagerStatsResponse;
import com.travel.travel.dto.TripResponse;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.BookingRepository;
import com.travel.travel.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    public TripService(TripRepository tripRepository, BookingRepository bookingRepository) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<TripResponse> findAll() {
        return tripRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<TripResponse> findByManager(UUID managerId) {
        return tripRepository.findByManagerId(managerId).stream().map(this::toResponse).toList();
    }

    public ManagerStatsResponse getManagerStats(UUID managerId) {
        List<Trip> trips = tripRepository.findByManagerId(managerId);
        List<UUID> tripIds = trips.stream().map(Trip::getId).toList();
        if (tripIds.isEmpty()) {
            return new ManagerStatsResponse(0, 0, BigDecimal.ZERO);
        }
        long travelers = bookingRepository.countByTripIdInAndStatus(tripIds, "CONFIRMED");
        BigDecimal income = bookingRepository.sumPriceByTripIdInAndStatus(tripIds, "CONFIRMED");
        return new ManagerStatsResponse(trips.size(), travelers, income);
    }

    public TripResponse getById(UUID id) {
        return tripRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));
    }

    @Transactional
    public TripResponse create(CreateTripRequest request, UUID managerId) {
        Trip trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setTitle(request.title());
        trip.setOriginCity(request.originCity());
        trip.setDestinationCity(request.destinationCity());
        trip.setDepartureDate(request.departureDate());
        trip.setPrice(request.price());
        trip.setSeatsAvailable(request.seatsAvailable());
        trip.setStatus("ACTIVE");
        trip.setManagerId(managerId);
        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse update(UUID id, CreateTripRequest request, UUID callerId, boolean isAdmin) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));
        if (!isAdmin && !callerId.equals(trip.getManagerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        trip.setTitle(request.title());
        trip.setOriginCity(request.originCity());
        trip.setDestinationCity(request.destinationCity());
        trip.setDepartureDate(request.departureDate());
        trip.setPrice(request.price());
        trip.setSeatsAvailable(request.seatsAvailable());
        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public void delete(UUID id, UUID callerId, boolean isAdmin) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));
        if (!isAdmin && !callerId.equals(trip.getManagerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        tripRepository.deleteById(id);
    }

    Trip getTripEntity(UUID id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));
    }

    @Transactional
    public void saveTrip(Trip trip) {
        tripRepository.save(trip);
    }

    private TripResponse toResponse(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getOriginCity(),
                trip.getDestinationCity(),
                trip.getDepartureDate(),
                trip.getPrice(),
                trip.getSeatsAvailable(),
                trip.getStatus(),
                trip.getManagerId()
        );
    }
}
