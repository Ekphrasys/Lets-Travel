package com.travel.travel.service;

import com.travel.travel.client.PaymentServiceClient;
import com.travel.travel.dto.CreateBookingRequest;
import com.travel.travel.dto.ConfirmBookingPaymentRequest;
import com.travel.travel.model.Booking;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TripService tripService;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private Neo4jRecommendationService neo4jRecommendationService;

    @Mock
    private TripGraphService tripGraphService;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void createBooking_createsPendingBookingWithIntent() {
        UUID userId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Trip trip = activeTrip(tripId, 5, new BigDecimal("50.00"));

        when(tripService.getTripEntity(tripId)).thenReturn(trip);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentServiceClient.createIntent(any(), any(), any(), any()))
                .thenReturn(new PaymentServiceClient.IntentResult(
                        paymentId, "pi_mock_" + paymentId, new BigDecimal("50.00"), "EUR", "REQUIRES_PAYMENT_METHOD"));

        var response = bookingService.createBooking(userId, new CreateBookingRequest(tripId, "CARD"));

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.clientSecret()).isEqualTo("pi_mock_" + paymentId);
        verify(tripService, never()).saveTrip(any());
        assertThat(trip.getSeatsAvailable()).isEqualTo(5);
    }

    @Test
    void createBooking_noSeats_throwsUnprocessable() {
        UUID tripId = UUID.randomUUID();
        Trip trip = activeTrip(tripId, 0, BigDecimal.TEN);
        when(tripService.getTripEntity(tripId)).thenReturn(trip);

        assertThatThrownBy(() -> bookingService.createBooking(UUID.randomUUID(), new CreateBookingRequest(tripId, "CARD")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Plus de places");
    }

    @Test
    void confirmBookingPayment_completed_confirmsBooking() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Trip trip = activeTrip(UUID.randomUUID(), 5, new BigDecimal("50.00"));
        Booking booking = booking(userId, "PENDING");
        booking.setId(bookingId);
        booking.setTrip(trip);
        booking.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentServiceClient.confirmPayment(paymentId))
                .thenReturn(new PaymentServiceClient.PaymentResult(
                        paymentId, bookingId, userId, new BigDecimal("50.00"), "COMPLETED", Instant.now()));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = bookingService.confirmBookingPayment(
                bookingId, new ConfirmBookingPaymentRequest("pi_mock_" + paymentId), userId
        );

        assertThat(response.status()).isEqualTo("CONFIRMED");
        verify(tripService).saveTrip(trip);
        assertThat(trip.getSeatsAvailable()).isEqualTo(4);
    }

    @Test
    void confirmBookingPayment_failed_cancelsBooking() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Trip trip = activeTrip(UUID.randomUUID(), 5, new BigDecimal("50.00"));
        Booking booking = booking(userId, "PENDING");
        booking.setId(bookingId);
        booking.setTrip(trip);
        booking.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentServiceClient.confirmPayment(paymentId))
                .thenReturn(new PaymentServiceClient.PaymentResult(
                        paymentId, bookingId, userId, new BigDecimal("50.00"), "FAILED", Instant.now()));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> bookingService.confirmBookingPayment(
                bookingId, new ConfirmBookingPaymentRequest("pi_mock_" + paymentId), userId
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Paiement refusé");

        assertThat(booking.getStatus()).isEqualTo("CANCELLED");
    }

    // @Test
    // void cancelBooking_forbiddenForOtherUser() {
    //     UUID bookingId = UUID.randomUUID();
    //     Booking booking = booking(UUID.randomUUID(), "CONFIRMED");
    //     booking.setTrip(activeTrip(UUID.randomUUID(), 5, BigDecimal.TEN));
    //     when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

    //     assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, UUID.randomUUID(), false))
    //             .isInstanceOf(ResponseStatusException.class)
    //             .hasMessageContaining("Accès refusé");
    // }

    @Test
    void cancelBooking_confirmed_restoresSeatAndRefunds() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Trip trip = activeTrip(UUID.randomUUID(), 4, BigDecimal.TEN);
        Booking booking = booking(userId, "CONFIRMED");
        booking.setId(bookingId);
        booking.setTrip(trip);
        booking.setPaymentId(paymentId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = bookingService.cancelBooking(bookingId, userId, false);

        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(paymentServiceClient).refund(paymentId);
        verify(tripService).saveTrip(trip);
        assertThat(trip.getSeatsAvailable()).isEqualTo(5);
    }

    // @Test
    // void cancelBooking_alreadyCancelled_throws() {
    //     UUID bookingId = UUID.randomUUID();
    //     Booking booking = booking(UUID.randomUUID(), "CANCELLED");
    //     booking.setId(bookingId);
    //     booking.setTrip(activeTrip(UUID.randomUUID(), 5, BigDecimal.TEN));
    //     when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

    //     assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, booking.getUserId(), false))
    //             .isInstanceOf(ResponseStatusException.class);
    // }

    @Test
    void findByUser_returnsBookings() {
        UUID userId = UUID.randomUUID();
        Booking booking = booking(userId, "PENDING");
        booking.setTrip(activeTrip(UUID.randomUUID(), 1, BigDecimal.ONE));
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(booking));

        assertThat(bookingService.findByUser(userId)).hasSize(1);
    }

    @Test
    void cancelBooking_pending_skipsRefund() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Booking booking = booking(userId, "PENDING");
        booking.setId(bookingId);
        booking.setTrip(activeTrip(UUID.randomUUID(), 2, BigDecimal.TEN));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBooking(bookingId, userId, false);

        verify(paymentServiceClient, never()).refund(any());
    }

    private static Trip activeTrip(UUID id, int seats, BigDecimal price) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setTitle("Trip");
        trip.setOriginCity("A");
        trip.setDestinationCity("B");
        trip.setDepartureDate(LocalDate.now().plusDays(5));
        trip.setPrice(price);
        trip.setSeatsAvailable(seats);
        trip.setStatus("ACTIVE");
        return trip;
    }

    private static Booking booking(UUID userId, String status) {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setUserId(userId);
        booking.setStatus(status);
        return booking;
    }
}
