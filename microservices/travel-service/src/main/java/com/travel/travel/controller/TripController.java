package com.travel.travel.controller;

import com.travel.travel.client.UserServiceClient;
import com.travel.travel.dto.*;
import com.travel.travel.model.Feedback;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.BookingRepository;
import com.travel.travel.repository.FeedbackRepository;
import com.travel.travel.search.TripSearchService;
import com.travel.travel.service.BookingService;
import com.travel.travel.service.Neo4jRecommendationService;
import com.travel.travel.service.RouteSearchService;
import com.travel.travel.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/travels")
public class TripController {

    private final TripService tripService;
    private final RouteSearchService routeSearchService;
    private final TripSearchService tripSearchService;
    private final Neo4jRecommendationService neo4jRecommendationService;
    private final BookingRepository bookingRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserServiceClient userServiceClient;
    private final BookingService bookingService;

    public TripController(TripService tripService, RouteSearchService routeSearchService,
                          TripSearchService tripSearchService,
                          Neo4jRecommendationService neo4jRecommendationService,
                          BookingRepository bookingRepository,
                          FeedbackRepository feedbackRepository,
                          UserServiceClient userServiceClient,
                          BookingService bookingService) {
        this.tripService = tripService;
        this.routeSearchService = routeSearchService;
        this.tripSearchService = tripSearchService;
        this.neo4jRecommendationService = neo4jRecommendationService;
        this.bookingRepository = bookingRepository;
        this.feedbackRepository = feedbackRepository;
        this.userServiceClient = userServiceClient;
        this.bookingService = bookingService;
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

    @GetMapping("/recommendations")
    public List<TripResponse> recommendations(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<UUID> ids = neo4jRecommendationService.getRecommendations(userId);
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        Set<UUID> alreadyBooked = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()) || "PENDING".equals(b.getStatus()))
                .map(b -> b.getTrip().getId())
                .collect(java.util.stream.Collectors.toSet());
        return tripService.findAll().stream()
                .filter(t -> ids.contains(t.id()) && !alreadyBooked.contains(t.id()))
                .toList();
    }

    @GetMapping("/{id}")
    public TripResponse getById(@PathVariable UUID id) {
        return tripService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRAVEL_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse create(@Valid @RequestBody CreateTripRequest request, Authentication authentication) {
        UUID managerId = UUID.fromString(authentication.getName());
        return tripService.create(request, managerId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRAVEL_MANAGER')")
    public TripResponse update(@PathVariable UUID id, @Valid @RequestBody CreateTripRequest request, Authentication authentication) {
        UUID updaterId = UUID.fromString(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        return tripService.update(id, request, updaterId, isAdmin);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRAVEL_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        UUID updaterId = UUID.fromString(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        tripService.delete(id, updaterId, isAdmin);
    }

    @PostMapping("/{id}/feedback")
    @Transactional
    public FeedbackResponse leaveFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody FeedbackRequest request,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        boolean hasBooking = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .anyMatch(b -> b.getTrip().getId().equals(id) && "CONFIRMED".equals(b.getStatus()));
        if (!hasBooking) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez laisser un avis que sur un voyage réservé");
        }

        Trip trip = tripService.getTripEntity(id);

        Feedback feedback = new Feedback();
        feedback.setId(UUID.randomUUID());
        feedback.setTrip(trip);
        feedback.setUserId(userId);
        feedback.setRating(request.rating());
        feedback.setComment(request.comment());
        feedbackRepository.save(feedback);

        neo4jRecommendationService.syncFeedback(userId, id, request.rating());

        UserServiceClient.UserProfile profile = userServiceClient.getById(userId);
        return new FeedbackResponse(
                feedback.getId(),
                id,
                trip.getTitle(),
                feedback.getUserId(),
                profile.email(),
                profile.firstName(),
                profile.lastName(),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt()
        );
    }

    @GetMapping("/{id}/feedbacks")
    public List<FeedbackResponse> feedbacks(@PathVariable UUID id) {
        return feedbackRepository.findByTripIdOrderByCreatedAtDesc(id).stream()
                .map(f -> {
                    UserServiceClient.UserProfile profile = userServiceClient.getById(f.getUserId());
                    return new FeedbackResponse(
                            f.getId(),
                            f.getTrip().getId(),
                            f.getTrip().getTitle(),
                            f.getUserId(),
                            profile.email(),
                            profile.firstName(),
                            profile.lastName(),
                            f.getRating(),
                            f.getComment(),
                            f.getCreatedAt()
                    );
                })
                .toList();
    }

    @GetMapping("/admin/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<TripAnalyticsResponse> adminTravelHistory() {
        return tripService.getAllAnalytics();
    }

    @GetMapping("/admin/managers")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<AdminDashboardResponse.ManagerPerformance> adminManagersRanking() {
        List<Trip> allTrips = tripService.getAllTripEntities();
        List<com.travel.travel.model.Booking> allBookings = bookingRepository.findAll();
        return buildManagersPerformance(allTrips, allBookings);
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminDashboardResponse adminDashboard() {
        List<Trip> allTrips = tripService.getAllTripEntities();
        List<com.travel.travel.model.Booking> allBookings = bookingRepository.findAll();
        List<Feedback> allFeedbacks = feedbackRepository.findAll();

        BigDecimal totalIncome = BigDecimal.ZERO;
        Map<String, BigDecimal> incomeByMonth = new TreeMap<>();
        java.time.format.DateTimeFormatter monthFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");

        for (com.travel.travel.model.Booking b : allBookings) {
            if ("CONFIRMED".equals(b.getStatus())) {
                BigDecimal price = b.getTrip().getPrice();
                totalIncome = totalIncome.add(price);
                String month = b.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).format(monthFormatter);
                incomeByMonth.put(month, incomeByMonth.getOrDefault(month, BigDecimal.ZERO).add(price));
            }
        }

        Map<UUID, Long> bookingCounts = new HashMap<>();
        for (com.travel.travel.model.Booking b : allBookings) {
            if ("CONFIRMED".equals(b.getStatus())) {
                bookingCounts.put(b.getTrip().getId(), bookingCounts.getOrDefault(b.getTrip().getId(), 0L) + 1);
            }
        }
        List<TripResponse> topTrips = tripService.findAll().stream()
                .sorted((t1, t2) -> Long.compare(bookingCounts.getOrDefault(t2.id(), 0L), bookingCounts.getOrDefault(t1.id(), 0L)))
                .limit(5)
                .toList();

        List<FeedbackResponse> recentFeedbacks = allFeedbacks.stream()
                .sorted(Comparator.comparing(Feedback::getCreatedAt).reversed())
                .limit(5)
                .map(f -> {
                    UserServiceClient.UserProfile p = userServiceClient.getById(f.getUserId());
                    return new FeedbackResponse(
                            f.getId(), f.getTrip().getId(), f.getTrip().getTitle(), f.getUserId(),
                            p.email(), p.firstName(), p.lastName(),
                            f.getRating(), f.getComment(), f.getCreatedAt()
                    );
                })
                .toList();

        List<AdminDashboardResponse.ManagerPerformance> perfList = buildManagersPerformance(allTrips, allBookings);

        return new AdminDashboardResponse(
                incomeByMonth, totalIncome, allTrips.size(), topTrips, recentFeedbacks, perfList
        );
    }

    private List<AdminDashboardResponse.ManagerPerformance> buildManagersPerformance(
            List<Trip> allTrips,
            List<com.travel.travel.model.Booking> allBookings) {

        Map<UUID, List<Trip>> tripsByManager = new HashMap<>();
        for (Trip t : allTrips) {
            if (t.getManagerId() != null) {
                tripsByManager.computeIfAbsent(t.getManagerId(), k -> new ArrayList<>()).add(t);
            }
        }

        List<AdminDashboardResponse.ManagerPerformance> perfList = new ArrayList<>();
        for (Map.Entry<UUID, List<Trip>> entry : tripsByManager.entrySet()) {
            UUID managerId = entry.getKey();
            List<Trip> mTrips = entry.getValue();
            List<UUID> mTripIds = mTrips.stream().map(Trip::getId).toList();

            BigDecimal mIncome = BigDecimal.ZERO;
            for (com.travel.travel.model.Booking b : allBookings) {
                if (mTripIds.contains(b.getTrip().getId()) && "CONFIRMED".equals(b.getStatus())) {
                    mIncome = mIncome.add(b.getTrip().getPrice());
                }
            }

            List<Feedback> mFeedbacks = feedbackRepository.findByTripIdIn(mTripIds);
            double avgRating = mFeedbacks.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
            long feedbackCount = mFeedbacks.size();
            double score = (avgRating * 20.0) + (mTrips.size() * 5.0) + (mIncome.doubleValue() / 100.0);

            UserServiceClient.UserProfile p = userServiceClient.getById(managerId);
            perfList.add(new AdminDashboardResponse.ManagerPerformance(
                    managerId, p.firstName() + " " + p.lastName(), p.email(),
                    mTrips.size(), mIncome, avgRating, feedbackCount, score
            ));
        }

        perfList.sort((p1, p2) -> Double.compare(p2.performanceScore(), p1.performanceScore()));
        return perfList;
    }

    @GetMapping("/managers/{managerId}/dashboard")
    @Transactional(readOnly = true)
    public ManagerDashboardResponse managerDashboard(@PathVariable UUID managerId) {
        List<Trip> mTrips = tripService.getAllTripEntities().stream()
                .filter(t -> managerId.equals(t.getManagerId()))
                .toList();

        List<UUID> mTripIds = mTrips.stream().map(Trip::getId).toList();
        List<com.travel.travel.model.Booking> allBookings = bookingRepository.findAll();

        long bookingsCount = 0;
        BigDecimal totalIncome = BigDecimal.ZERO;
        for (com.travel.travel.model.Booking b : allBookings) {
            if (mTripIds.contains(b.getTrip().getId()) && "CONFIRMED".equals(b.getStatus())) {
                bookingsCount++;
                totalIncome = totalIncome.add(b.getTrip().getPrice());
            }
        }

        List<Feedback> mFeedbacks = feedbackRepository.findByTripIdIn(mTripIds);
        List<FeedbackResponse> feedbacks = mFeedbacks.stream()
                .sorted(Comparator.comparing(Feedback::getCreatedAt).reversed())
                .map(f -> {
                    UserServiceClient.UserProfile p = userServiceClient.getById(f.getUserId());
                    return new FeedbackResponse(
                            f.getId(), f.getTrip().getId(), f.getTrip().getTitle(), f.getUserId(),
                            p.email(), p.firstName(), p.lastName(),
                            f.getRating(), f.getComment(), f.getCreatedAt()
                    );
                })
                .toList();

        return new ManagerDashboardResponse(mTrips.size(), bookingsCount, totalIncome, feedbacks);
    }

    @GetMapping("/{id}/subscribers")
    @Transactional(readOnly = true)
    public List<UserServiceClient.UserProfile> getSubscribers(@PathVariable UUID id, Authentication authentication) {
        Trip trip = tripService.getTripEntity(id);
        UUID currentUserId = UUID.fromString(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUserId.equals(trip.getManagerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        List<com.travel.travel.model.Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getTrip().getId().equals(id) && "CONFIRMED".equals(b.getStatus()))
                .toList();

        return bookings.stream()
                .map(b -> userServiceClient.getById(b.getUserId()))
                .toList();
    }

    @PostMapping("/{id}/unsubscribe/{userId}")
    public void unsubscribeTraveler(@PathVariable UUID id, @PathVariable UUID userId, Authentication authentication) {
        Trip trip = tripService.getTripEntity(id);
        UUID currentUserId = UUID.fromString(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !currentUserId.equals(trip.getManagerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        com.travel.travel.model.Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getTrip().getId().equals(id) && b.getUserId().equals(userId) && "CONFIRMED".equals(b.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation active introuvable"));

        bookingService.cancelBooking(booking.getId(), userId, true);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
