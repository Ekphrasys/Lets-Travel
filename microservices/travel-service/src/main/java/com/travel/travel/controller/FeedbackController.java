package com.travel.travel.controller;

import com.travel.travel.dto.CreateFeedbackRequest;
import com.travel.travel.dto.FeedbackResponse;
import com.travel.travel.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse create(@Valid @RequestBody CreateFeedbackRequest request, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return feedbackService.create(userId, request);
    }

    @GetMapping("/trip/{tripId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAVEL_MANAGER')")
    public List<FeedbackResponse> byTrip(@PathVariable UUID tripId, Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        boolean isAdmin = isAdmin(authentication);
        return feedbackService.findByTrip(tripId, callerId, isAdmin);
    }

    @GetMapping("/my")
    public List<FeedbackResponse> myFeedbacks(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return feedbackService.findByUser(userId);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<FeedbackResponse> allFeedbacks() {
        return feedbackService.findAll();
    }

    @GetMapping("/my-trips")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAVEL_MANAGER')")
    public List<FeedbackResponse> myTripsFeedback(Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        return feedbackService.findByManager(callerId);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
