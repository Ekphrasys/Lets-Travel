package com.travel.payment.service;

import com.travel.payment.dto.CreatePaymentRequest;
import com.travel.payment.dto.PaymentResponse;
import com.travel.payment.dto.UpdatePaymentRequest;
import com.travel.payment.model.Payment;
import com.travel.payment.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(request.bookingId());
        payment.setUserId(request.userId());
        payment.setAmount(request.amount());
        payment.setStatus(mockPaymentStatus(request.amount()));
        if (request.paymentMethod() != null) {
            payment.setPaymentMethod(request.paymentMethod());
        }
        return toResponse(paymentRepository.save(payment));
    }

    public List<PaymentResponse> findAll() {
        return paymentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public PaymentResponse getById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));
    }

    @Transactional
    public PaymentResponse update(UUID paymentId, UpdatePaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));
        payment.setAmount(request.amount());
        payment.setStatus(request.status());
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(UUID paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable");
        }
        paymentRepository.deleteById(paymentId);
    }

    @Transactional
    public PaymentResponse refund(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));
        if (!"COMPLETED".equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Paiement non remboursable");
        }
        payment.setStatus("REFUNDED");
        return toResponse(paymentRepository.save(payment));
    }

    private String mockPaymentStatus(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) > 0 ? "COMPLETED" : "FAILED";
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getCreatedAt()
        );
    }
}
