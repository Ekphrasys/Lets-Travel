package com.travel.payment.service;

import com.travel.payment.dto.CreatePaymentRequest;
import com.travel.payment.dto.PaymentResponse;
import com.travel.payment.model.Payment;
import com.travel.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPayment_nonPositiveAmount_marksFailed() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.createPayment(
                new CreatePaymentRequest(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO, "CARD")
        );

        assertThat(response.status()).isEqualTo("FAILED");
    }

    @Test
    void getById_notFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getById(paymentId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void update_notFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.update(paymentId,
                new com.travel.payment.dto.UpdatePaymentRequest(BigDecimal.TEN, "COMPLETED")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void delete_notFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.existsById(paymentId)).thenReturn(false);

        assertThatThrownBy(() -> paymentService.delete(paymentId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void refund_nonCompleted_throwsUnprocessable() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus("FAILED");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refund(paymentId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void findAll_returnsPayments() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(UUID.randomUUID());
        payment.setUserId(UUID.randomUUID());
        payment.setAmount(BigDecimal.TEN);
        payment.setStatus("COMPLETED");
        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        assertThat(paymentService.findAll()).hasSize(1);
    }

    @Test
    void update_success() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setAmount(new BigDecimal("10.00"));
        payment.setStatus("COMPLETED");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.update(paymentId,
                new com.travel.payment.dto.UpdatePaymentRequest(new BigDecimal("25.00"), "COMPLETED"));

        assertThat(response.amount()).isEqualByComparingTo("25.00");
    }

    @Test
    void delete_success() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.existsById(paymentId)).thenReturn(true);

        paymentService.delete(paymentId);

        verify(paymentRepository).deleteById(paymentId);
    }

    @Test
    void getById_success() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setBookingId(UUID.randomUUID());
        payment.setUserId(UUID.randomUUID());
        payment.setAmount(BigDecimal.TEN);
        payment.setStatus("COMPLETED");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThat(paymentService.getById(paymentId).id()).isEqualTo(paymentId);
    }

    @Test
    void createPayment_marksPositiveAmountAsCompleted() {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.createPayment(
                new CreatePaymentRequest(bookingId, userId, new BigDecimal("199.99"), "CARD")
        );

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.bookingId()).isEqualTo(bookingId);
    }

    @Test
    void refund_completedPayment_updatesStatus() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setBookingId(UUID.randomUUID());
        payment.setUserId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("50.00"));
        payment.setStatus("COMPLETED");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.refund(paymentId);

        assertThat(response.status()).isEqualTo("REFUNDED");
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    void refund_unknownPayment_throwsNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refund(paymentId))
                .isInstanceOf(ResponseStatusException.class);
    }
}
