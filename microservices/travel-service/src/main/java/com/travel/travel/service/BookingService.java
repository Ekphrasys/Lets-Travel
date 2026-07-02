package com.travel.travel.service;

import com.travel.travel.client.PaymentServiceClient;
import com.travel.travel.dto.BookingResponse;
import com.travel.travel.dto.ConfirmBookingPaymentRequest;
import com.travel.travel.dto.CreateBookingRequest;
import com.travel.travel.model.Booking;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripService tripService;
    private final PaymentServiceClient paymentServiceClient;
    private final TripGraphService tripGraphService;
    private final Neo4jRecommendationService neo4jRecommendationService;

    public BookingService(
            BookingRepository bookingRepository,
            TripService tripService,
            PaymentServiceClient paymentServiceClient,
            TripGraphService tripGraphService,
            Neo4jRecommendationService neo4jRecommendationService
    ) {
        this.bookingRepository = bookingRepository;
        this.tripService = tripService;
        this.paymentServiceClient = paymentServiceClient;
        this.tripGraphService = tripGraphService;
        this.neo4jRecommendationService = neo4jRecommendationService;
    }

    @Transactional
    public BookingResponse createBooking(UUID userId, CreateBookingRequest request) {
        Trip trip = tripService.getTripEntity(request.tripId());
        if (!"ACTIVE".equals(trip.getStatus()) || trip.getSeatsAvailable() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Plus de places disponibles");
        }
        if (ChronoUnit.DAYS.between(LocalDate.now(), trip.getDepartureDate()) <= 3) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Réservation impossible : le départ est dans moins de 3 jours");
        }

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setTrip(trip);
        booking.setUserId(userId);
        booking.setStatus("PENDING");

        PaymentServiceClient.IntentResult intent = paymentServiceClient.createIntent(
                booking.getId(), userId, trip.getPrice(), request.paymentMethod()
        );

        booking.setPaymentId(intent.paymentId());
        bookingRepository.save(booking);

        return toResponse(booking, intent.clientSecret());
    }

    @Transactional
    public BookingResponse confirmBookingPayment(UUID bookingId, ConfirmBookingPaymentRequest request, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));

        if (!booking.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        if (!"PENDING".equals(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Réservation déjà traitée");
        }

        String expectedSecret = "pi_mock_" + booking.getPaymentId();
        if (!expectedSecret.equals(request.clientSecret())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Secret de paiement invalide");
        }

        PaymentServiceClient.PaymentResult payment = paymentServiceClient.confirmPayment(booking.getPaymentId());

        if ("COMPLETED".equals(payment.status())) {
            booking.setStatus("CONFIRMED");
            Trip trip = booking.getTrip();
            trip.setSeatsAvailable(trip.getSeatsAvailable() - 1);
            tripService.saveTrip(trip);
            BookingResponse response = toResponse(bookingRepository.save(booking), null);
            tripGraphService.recordParticipation(userId, trip);
            neo4jRecommendationService.syncBooking(userId, trip.getId(), false);
            return response;
        }

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Paiement refusé");
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findByUser(UUID userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(b -> toResponse(b, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findByTrip(UUID tripId, UUID callerId, boolean isAdmin) {
        Trip trip = tripService.getTripEntity(tripId);
        if (!isAdmin && !callerId.equals(trip.getManagerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return bookingRepository.findByTrip_Id(tripId).stream().map(b -> toResponse(b, null)).toList();
    }

    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID callerId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));

        boolean isManager = booking.getTrip().getManagerId() != null
                && booking.getTrip().getManagerId().equals(callerId);
        if (!isAdmin && !isManager && !booking.getUserId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Réservation déjà annulée");
        }
        boolean isSelfCancel = booking.getUserId().equals(callerId) && !isAdmin && !isManager;
        if (isSelfCancel && ChronoUnit.DAYS.between(LocalDate.now(), booking.getTrip().getDepartureDate()) <= 3) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Annulation impossible : le départ est dans moins de 3 jours");
        }

        if ("CONFIRMED".equals(booking.getStatus())) {
            if (booking.getPaymentId() != null) {
                paymentServiceClient.refund(booking.getPaymentId());
            }
            Trip trip = booking.getTrip();
            trip.setSeatsAvailable(trip.getSeatsAvailable() + 1);
            tripService.saveTrip(trip);
        } else if ("PENDING".equals(booking.getStatus()) && booking.getPaymentId() != null) {
            paymentServiceClient.cancelIntent(booking.getPaymentId());
        }

        booking.setStatus("CANCELLED");
        neo4jRecommendationService.syncBooking(booking.getUserId(), booking.getTrip().getId(), true);
        return toResponse(bookingRepository.save(booking), null);
    }

    private BookingResponse toResponse(Booking booking, String clientSecret) {
        return new BookingResponse(
                booking.getId(),
                booking.getTrip().getId(),
                booking.getTrip().getTitle(),
                booking.getUserId(),
                booking.getStatus(),
                booking.getPaymentId(),
                clientSecret,
                booking.getCreatedAt(),
                booking.getTrip().getDepartureDate()
        );
    }
}
