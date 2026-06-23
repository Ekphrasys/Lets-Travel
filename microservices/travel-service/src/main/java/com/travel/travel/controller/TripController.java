package com.travel.travel.controller;

import com.travel.travel.dto.CreateTripRequest;
import com.travel.travel.dto.RouteResponse;
import com.travel.travel.dto.TripResponse;
import com.travel.travel.service.RouteSearchService;
import com.travel.travel.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/travels")
public class TripController {

    private final TripService tripService;
    private final RouteSearchService routeSearchService;

    public TripController(TripService tripService, RouteSearchService routeSearchService) {
        this.tripService = tripService;
        this.routeSearchService = routeSearchService;
    }

    @GetMapping
    public List<TripResponse> listAll() {
        return tripService.findAll();
    }

    @GetMapping("/routes/search")
    public List<RouteResponse> searchRoutes(
            @RequestParam String origin,
            @RequestParam String destination
    ) {
        return routeSearchService.search(origin, destination);
    }

    @GetMapping("/{id}")
    public TripResponse getById(@PathVariable UUID id) {
        return tripService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse create(@Valid @RequestBody CreateTripRequest request) {
        return tripService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TripResponse update(@PathVariable UUID id, @Valid @RequestBody CreateTripRequest request) {
        return tripService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tripService.delete(id);
    }
}
