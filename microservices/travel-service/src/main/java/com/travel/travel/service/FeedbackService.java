package com.travel.travel.service;

import com.travel.travel.dto.CreateFeedbackRequest;
import com.travel.travel.dto.FeedbackResponse;
import com.travel.travel.model.Feedback;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.BookingRepository;
import com.travel.travel.repository.FeedbackRepository;
import com.travel.travel.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, TripRepository tripRepository,
                           BookingRepository bookingRepository) {
        this.feedbackRepository = feedbackRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public FeedbackResponse create(UUID userId, CreateFeedbackRequest request) {
        Trip trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));

        if (!bookingRepository.existsByTripIdAndUserIdAndStatus(trip.getId(), userId, "CONFIRMED")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous devez avoir participé à ce voyage pour laisser un avis");
        }

        if (feedbackRepository.existsByTripIdAndUserId(trip.getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vous avez déjà laissé un avis pour ce voyage");
        }

        Feedback feedback = new Feedback();
        feedback.setId(UUID.randomUUID());
        feedback.setTrip(trip);
        feedback.setUserId(userId);
        feedback.setRating(request.rating());
        feedback.setComment(request.comment());
        return toResponse(feedbackRepository.save(feedback));
    }

    public List<FeedbackResponse> findByUser(UUID userId) {
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<FeedbackResponse> findByTrip(UUID tripId, UUID callerId, boolean isAdmin) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable"));

        if (!isAdmin && !callerId.equals(trip.getManagerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return feedbackRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<FeedbackResponse> findByManager(UUID managerId) {
        List<UUID> tripIds = tripRepository.findByManagerId(managerId).stream()
                .map(Trip::getId)
                .toList();
        if (tripIds.isEmpty()) return List.of();
        return feedbackRepository.findByTripIdIn(tripIds).stream()
                .map(this::toResponse)
                .toList();
    }

    private FeedbackResponse toResponse(Feedback f) {
        return new FeedbackResponse(
                f.getId(),
                f.getTrip().getId(),
                f.getUserId(),
                f.getRating(),
                f.getComment(),
                f.getCreatedAt()
        );
    }
}
