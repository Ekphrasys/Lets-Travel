package com.travel.travel.controller;

import com.travel.travel.dto.CreateTripRequest;
import com.travel.travel.dto.ManagerStatsResponse;
import com.travel.travel.dto.RouteResponse;
import com.travel.travel.dto.TripAnalyticsResponse;
import com.travel.travel.dto.TripResponse;
import com.travel.travel.search.TripSearchService;
import com.travel.travel.service.RouteSearchService;
import com.travel.travel.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/travels")
public class TripController {

    private final TripService tripService;
    private final RouteSearchService routeSearchService;
    private final TripSearchService tripSearchService;

    public TripController(TripService tripService, RouteSearchService routeSearchService,
                          TripSearchService tripSearchService) {
        this.tripService = tripService;
        this.routeSearchService = routeSearchService;
        this.tripSearchService = tripSearchService;
    }

    @GetMapping
    public List<TripResponse> listAll() {
        return tripService.findAll();
    }

    @GetMapping("/search")
    public List<TripResponse> search(@RequestParam String q) {
        return tripSearchService.search(q);
    }

    @GetMapping("/autocomplete")
    public List<String> autocomplete(@RequestParam String q) {
        return tripSearchService.autocomplete(q);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAVEL_MANAGER')")
    public List<TripResponse> myTrips(Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        return tripService.findByManager(callerId);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAVEL_MANAGER')")
    public ManagerStatsResponse myStats(Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        return tripService.getManagerStats(callerId);
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAVEL_MANAGER')")
    public List<TripAnalyticsResponse> myAnalytics(Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        return tripService.getManagerAnalytics(callerId);
    }

    @GetMapping("/routes/search")
    public List<RouteResponse> searchRoutes(
            @RequestParam String origin,
            @RequestParam String destination
    ) {
        return routeSearchService.search(origin, destination);
    }

    @GetMapping("/suggestions")
    @PreAuthorize("isAuthenticated()")
    public List<TripResponse> suggestions(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return tripService.getSuggestions(userId);
    }

    @GetMapping("/{id}")
    public TripResponse getById(@PathVariable UUID id) {
        return tripService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAVEL_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse create(@Valid @RequestBody CreateTripRequest request, Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        return tripService.create(request, callerId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAVEL_MANAGER')")
    public TripResponse update(@PathVariable UUID id, @Valid @RequestBody CreateTripRequest request, Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        return tripService.update(id, request, callerId, isAdmin);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAVEL_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        tripService.delete(id, callerId, isAdmin);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
