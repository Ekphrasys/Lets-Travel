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

    public BookingService(
            BookingRepository bookingRepository,
            TripService tripService,
            PaymentServiceClient paymentServiceClient
    ) {
        this.bookingRepository = bookingRepository;
        this.tripService = tripService;
        this.paymentServiceClient = paymentServiceClient;
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
                booking.getId(), userId, trip.getPrice()
        );

        if ("COMPLETED".equals(payment.status())) {
            booking.setStatus("CONFIRMED");
            booking.setPaymentId(payment.id());
            trip.setSeatsAvailable(trip.getSeatsAvailable() - 1);
            tripService.saveTrip(trip);
            return toResponse(bookingRepository.save(booking));
        }

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Paiement refusé");
    }

    public List<BookingResponse> findByUser(UUID userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID userId, boolean isAdmin) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable"));

        if (!isAdmin && !booking.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Réservation déjà annulée");
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
