package com.travel.travel.service;

import com.travel.travel.dto.CreateTripRequest;
import com.travel.travel.dto.TripResponse;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final ElasticsearchService elasticsearchService;
    private final Neo4jRecommendationService neo4jRecommendationService;

    public TripService(
            TripRepository tripRepository,
            ElasticsearchService elasticsearchService,
            Neo4jRecommendationService neo4jRecommendationService
    ) {
        this.tripRepository = tripRepository;
        this.elasticsearchService = elasticsearchService;
        this.neo4jRecommendationService = neo4jRecommendationService;
    }

    public List<TripResponse> findAll() {
        return tripRepository.findAll().stream().map(this::toResponse).toList();
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

        Trip savedTrip = tripRepository.save(trip);

        // Sync to Elasticsearch and Neo4j
        elasticsearchService.indexTrip(savedTrip);
        neo4jRecommendationService.syncTrip(
                savedTrip.getId(),
                savedTrip.getTitle(),
                savedTrip.getOriginCity(),
                savedTrip.getDestinationCity(),
                savedTrip.getPrice().doubleValue(),
                savedTrip.getDepartureDate()
        );

        return toResponse(savedTrip);
    }

    @Transactional
    public TripResponse update(UUID id, CreateTripRequest request) {
        return update(id, request, null, true);
    }

    @Transactional
    public TripResponse update(UUID id, CreateTripRequest request, UUID updaterId, boolean isAdmin) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));

        if (!isAdmin && !updaterId.equals(trip.getManagerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        trip.setTitle(request.title());
        trip.setOriginCity(request.originCity());
        trip.setDestinationCity(request.destinationCity());
        trip.setDepartureDate(request.departureDate());
        trip.setPrice(request.price());
        trip.setSeatsAvailable(request.seatsAvailable());

        Trip savedTrip = tripRepository.save(trip);

        // Sync to Elasticsearch and Neo4j
        elasticsearchService.indexTrip(savedTrip);
        neo4jRecommendationService.syncTrip(
                savedTrip.getId(),
                savedTrip.getTitle(),
                savedTrip.getOriginCity(),
                savedTrip.getDestinationCity(),
                savedTrip.getPrice().doubleValue(),
                savedTrip.getDepartureDate()
        );

        return toResponse(savedTrip);
    }

    @Transactional
    public void delete(UUID id) {
        delete(id, null, true);
    }

    @Transactional
    public void delete(UUID id, UUID updaterId, boolean isAdmin) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));

        if (!isAdmin && !updaterId.equals(trip.getManagerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        tripRepository.delete(trip);

        // Sync delete to Elasticsearch and Neo4j
        elasticsearchService.deleteTrip(id);
        neo4jRecommendationService.deleteTrip(id);
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
