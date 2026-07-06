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

    public BookingService(
            BookingRepository bookingRepository,
            TripService tripService,
            PaymentServiceClient paymentServiceClient,
            TripGraphService tripGraphService
    ) {
        this.bookingRepository = bookingRepository;
        this.tripService = tripService;
        this.paymentServiceClient = paymentServiceClient;
        this.tripGraphService = tripGraphService;
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
        bookingRepository.save(booking);

        PaymentServiceClient.PaymentResult payment = paymentServiceClient.createPayment(
                booking.getId(), userId, trip.getPrice(), request.paymentMethod()
        );

        if ("PROCESSING".equals(payment.status()) || "PENDING".equals(payment.status())) {
            booking.setPaymentId(payment.id());
            return toResponse(bookingRepository.save(booking), null);
        }

        if ("COMPLETED".equals(payment.status())) {
            confirmBooking(booking, trip, payment.id());
            bookingRepository.save(booking);
            return toResponse(booking, null);
        }

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Paiement refusé" + (payment.failedReason() != null ? ": " + payment.failedReason() : ""));
    }

    @Transactional
    public BookingResponse confirmBookingPayment(UUID bookingId, com.travel.travel.dto.ConfirmBookingPaymentRequest request, UUID userId) {
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Confirmation de paiement non supportée dans ce flux");
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
        return toResponse(booking, null);
    }

    private void confirmBooking(Booking booking, Trip trip, UUID paymentId) {
        booking.setStatus("CONFIRMED");
        booking.setPaymentId(paymentId);
        trip.setSeatsAvailable(trip.getSeatsAvailable() - 1);
        tripService.saveTrip(trip);
        tripGraphService.recordBooking(booking.getUserId(), trip.getId(), false);
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(UUID bookingId, UUID callerId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));

        boolean isManager = booking.getTrip().getManagerId() != null
                && booking.getTrip().getManagerId().equals(callerId);
        if (!isAdmin && !isManager && !booking.getUserId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return toResponse(booking, null);
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
        }

        booking.setStatus("CANCELLED");
        tripGraphService.recordBooking(booking.getUserId(), booking.getTrip().getId(), true);
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
