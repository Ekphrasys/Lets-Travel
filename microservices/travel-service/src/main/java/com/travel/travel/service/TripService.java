package com.travel.travel.service;

import com.travel.travel.dto.CreateTripRequest;
import com.travel.travel.dto.ManagerStatsResponse;
import com.travel.travel.dto.TripAnalyticsResponse;
import com.travel.travel.dto.TripResponse;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.BookingRepository;
import com.travel.travel.repository.FeedbackRepository;
import com.travel.travel.repository.TripRepository;
import com.travel.travel.search.TripSearchService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final FeedbackRepository feedbackRepository;
    private final TripSearchService tripSearchService;
    private final TripGraphService tripGraphService;

    public TripService(TripRepository tripRepository, BookingRepository bookingRepository,
                       FeedbackRepository feedbackRepository, TripSearchService tripSearchService,
                       TripGraphService tripGraphService) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.feedbackRepository = feedbackRepository;
        this.tripSearchService = tripSearchService;
        this.tripGraphService = tripGraphService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reindexOnStartup() {
        List<Trip> trips = tripRepository.findAll();
        tripSearchService.reindexAll(trips);
        tripGraphService.syncAllTrips(trips);
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

    public List<TripAnalyticsResponse> getManagerAnalytics(UUID managerId) {
        List<Trip> trips = tripRepository.findByManagerId(managerId);
        if (trips.isEmpty()) return List.of();

        List<UUID> tripIds = trips.stream().map(Trip::getId).toList();

        Map<UUID, Long> bookingCounts = new HashMap<>();
        Map<UUID, BigDecimal> revenues = new HashMap<>();
        bookingRepository.bookingStatsByTripIds(tripIds, "CONFIRMED").forEach(row -> {
            UUID tid = (UUID) row[0];
            bookingCounts.put(tid, ((Number) row[1]).longValue());
            revenues.put(tid, (BigDecimal) row[2]);
        });

        Map<UUID, Double> avgRatings = new HashMap<>();
        Map<UUID, Long> feedbackCounts = new HashMap<>();
        feedbackRepository.ratingStatsByTripIds(tripIds).forEach(row -> {
            UUID tid = (UUID) row[0];
            avgRatings.put(tid, ((Number) row[1]).doubleValue());
            feedbackCounts.put(tid, ((Number) row[2]).longValue());
        });

        return trips.stream().map(trip -> {
            UUID tid = trip.getId();
            long confirmed = bookingCounts.getOrDefault(tid, 0L);
            BigDecimal revenue = revenues.getOrDefault(tid, BigDecimal.ZERO);
            double avgRating = avgRatings.getOrDefault(tid, 0.0);
            long fbCount = feedbackCounts.getOrDefault(tid, 0L);
            int total = (int) confirmed + trip.getSeatsAvailable();
            double occupancy = total > 0 ? (double) confirmed / total * 100.0 : 0.0;
            return new TripAnalyticsResponse(
                    tid, trip.getTitle(), trip.getOriginCity(), trip.getDestinationCity(),
                    trip.getDepartureDate(), trip.getPrice(), trip.getSeatsAvailable(), trip.getStatus(),
                    confirmed, revenue, occupancy, avgRating, fbCount
            );
        }).toList();
    }

    public TripResponse getById(UUID id) {
        return tripRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));
    }

    @Transactional
    public TripResponse create(CreateTripRequest request) {
        return create(request, UUID.randomUUID());
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
        Trip saved = tripRepository.save(trip);
        tripSearchService.indexTrip(saved);
        tripGraphService.syncTrip(saved);
        return toResponse(saved);
    }

    @Transactional
    public TripResponse update(UUID id, CreateTripRequest request, UUID updaterId, boolean isAdmin) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));

        if (!isAdmin && updaterId != null && !updaterId.equals(trip.getManagerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        trip.setTitle(request.title());
        trip.setOriginCity(request.originCity());
        trip.setDestinationCity(request.destinationCity());
        trip.setDepartureDate(request.departureDate());
        trip.setPrice(request.price());
        trip.setSeatsAvailable(request.seatsAvailable());
        Trip saved = tripRepository.save(trip);
        tripSearchService.indexTrip(saved);
        tripGraphService.syncTrip(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id, UUID updaterId, boolean isAdmin) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));

        if (!isAdmin && updaterId != null && !updaterId.equals(trip.getManagerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        tripRepository.deleteById(id);
        tripSearchService.removeTrip(id);
    }

    public List<TripResponse> getSuggestions(UUID userId) {
        Set<UUID> alreadyBooked = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()) || "PENDING".equals(b.getStatus()))
                .map(b -> b.getTrip().getId())
                .collect(Collectors.toSet());

        List<String> suggestedIds = tripGraphService.getSuggestedTripIds(userId);
        if (suggestedIds.isEmpty()) {
            return tripRepository.findByStatusOrderByDepartureDateAsc("ACTIVE")
                    .stream()
                    .filter(t -> !alreadyBooked.contains(t.getId()))
                    .limit(5)
                    .map(this::toResponse)
                    .toList();
        }
        Map<UUID, Trip> tripMap = tripRepository.findAllById(
                suggestedIds.stream().map(UUID::fromString).toList()
        ).stream().collect(Collectors.toMap(Trip::getId, t -> t));
        return suggestedIds.stream()
                .map(id -> tripMap.get(UUID.fromString(id)))
                .filter(Objects::nonNull)
                .filter(t -> !alreadyBooked.contains(t.getId()))
                .map(this::toResponse)
                .toList();
    }

    public List<Trip> getAllTripEntities() {
        return tripRepository.findAll();
    }

    public Trip getTripEntity(UUID id) {
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
