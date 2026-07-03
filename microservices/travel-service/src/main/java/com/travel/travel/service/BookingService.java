package com.travel.travel.service;

import com.travel.travel.client.PaymentServiceClient;
import com.travel.travel.dto.BookingResponse;
import com.travel.travel.dto.CreateBookingRequest;
import com.travel.travel.model.Booking;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripService tripService;
    private final PaymentServiceClient paymentServiceClient;
    private final Neo4jRecommendationService neo4jRecommendationService;

    public BookingService(
            BookingRepository bookingRepository,
            TripService tripService,
            PaymentServiceClient paymentServiceClient,
            Neo4jRecommendationService neo4jRecommendationService
    ) {
        this.bookingRepository = bookingRepository;
        this.tripService = tripService;
        this.paymentServiceClient = paymentServiceClient;
        this.neo4jRecommendationService = neo4jRecommendationService;
    }

    @Transactional
    public BookingResponse createBooking(UUID userId, CreateBookingRequest request) {
        Trip trip = tripService.getTripEntity(request.tripId());
        if (!"ACTIVE".equals(trip.getStatus()) || trip.getSeatsAvailable() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Plus de places disponibles");
        }

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setTrip(trip);
        booking.setUserId(userId);
        booking.setStatus("PENDING");
        bookingRepository.save(booking);

        PaymentServiceClient.PaymentResult payment = paymentServiceClient.createPayment(
                booking.getId(), userId, trip.getPrice(), request.paymentMethod()
        );

        if ("PROCESSING".equals(payment.status()) || "PENDING".equals(payment.status())) {
            booking.setPaymentId(payment.id());
            bookingRepository.save(booking);
            return toResponse(booking);
        }

        if ("COMPLETED".equals(payment.status())) {
            confirmBooking(booking, trip, payment.id());
            return toResponse(bookingRepository.save(booking));
        }

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Paiement refusé" + (payment.failedReason() != null ? ": " + payment.failedReason() : ""));
    }

    @Transactional
    public BookingResponse processPaymentCallback(UUID bookingId, String paymentStatus, String providerTransactionId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));

        if ("COMPLETED".equalsIgnoreCase(paymentStatus) && "PENDING".equals(booking.getStatus())) {
            confirmBooking(booking, booking.getTrip(), booking.getPaymentId());
            bookingRepository.save(booking);
        } else if ("FAILED".equalsIgnoreCase(paymentStatus)) {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
        }
        return toResponse(booking);
    }

    private void confirmBooking(Booking booking, Trip trip, UUID paymentId) {
        booking.setStatus("CONFIRMED");
        booking.setPaymentId(paymentId);
        trip.setSeatsAvailable(trip.getSeatsAvailable() - 1);
        tripService.saveTrip(trip);
        neo4jRecommendationService.syncBooking(booking.getUserId(), trip.getId(), false);
    }

    public List<BookingResponse> findByUser(UUID userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BookingResponse> findByTrip(UUID tripId, UUID callerId, boolean isAdmin) {
        Trip trip = tripService.getTripEntity(tripId);
        if (!isAdmin && !callerId.equals(trip.getManagerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return bookingRepository.findByTrip_Id(tripId).stream().map(this::toResponse).toList();
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

        if (!isAdmin && java.time.LocalDate.now().plusDays(3).isAfter(booking.getTrip().getDepartureDate())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Annulation impossible moins de 3 jours avant le départ");
        }

        if ("CONFIRMED".equals(booking.getStatus())) {
            if (booking.getPaymentId() != null) {
                paymentServiceClient.refund(booking.getPaymentId());
            }
            Trip trip = booking.getTrip();
            trip.setSeatsAvailable(trip.getSeatsAvailable() + 1);
            tripService.saveTrip(trip);
        }

        booking.setStatus("CANCELLED");

        neo4jRecommendationService.syncBooking(booking.getUserId(), booking.getTrip().getId(), true);

        return toResponse(bookingRepository.save(booking));
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getTrip().getId(),
                booking.getUserId(),
                booking.getStatus(),
                booking.getPaymentId(),
                booking.getCreatedAt()
        );
    }
}
