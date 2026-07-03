package com.travel.payment.service;

import com.travel.payment.dto.CreatePaymentRequest;
import com.travel.payment.dto.PaymentResponse;
import com.travel.payment.dto.UpdatePaymentRequest;
import com.travel.payment.model.Payment;
import com.travel.payment.model.PaymentEvent;
import com.travel.payment.provider.CreditCardProvider;
import com.travel.payment.provider.PayPalProvider;
import com.travel.payment.provider.PaymentProvider;
import com.travel.payment.repository.PaymentEventRepository;
import com.travel.payment.repository.PaymentRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Primary
public class PaymentService {

    private static final Map<String, PaymentProvider> PROVIDERS = Map.of(
            PaymentProvider.TYPE_CARD, new CreditCardProvider(),
            PaymentProvider.TYPE_PAYPAL, new PayPalProvider()
    );

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventRepository paymentEventRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(request.bookingId());
        payment.setUserId(request.userId());
        payment.setAmount(request.amount());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setStatus("PROCESSING");
        paymentRepository.save(payment);

        recordEvent(payment.getId(), PaymentEvent.EventType.CREATED, null, null, null, null);

        PaymentProvider provider = PROVIDERS.getOrDefault(request.paymentMethod(), PROVIDERS.get(PaymentProvider.TYPE_CARD));
        PaymentProvider.PaymentContext context = new PaymentProvider.PaymentContext(
                request.bookingId().toString(), request.userId().toString(), request.amount()
        );

        PaymentProvider.PaymentResult result = provider.process(context);

        payment.setProviderTransactionId(result.providerTransactionId());
        payment.setProviderStatus(result.providerStatus());

        if (result.success()) {
            payment.setStatus("COMPLETED");
            recordEvent(payment.getId(), PaymentEvent.EventType.COMPLETED, result.providerTransactionId(),
                    provider.getClass().getSimpleName(), 200, result.providerStatus());
        } else {
            payment.setStatus("FAILED");
            payment.setFailedReason(result.failedReason());
            recordEvent(payment.getId(), PaymentEvent.EventType.FAILED, result.providerTransactionId(),
                    provider.getClass().getSimpleName(), 402, result.failedReason());
        }

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse capture(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));
        if (!"PENDING".equals(payment.getStatus())) {
            return toResponse(payment);
        }
        PaymentProvider provider = PROVIDERS.getOrDefault(payment.getPaymentMethod(), PROVIDERS.get(PaymentProvider.TYPE_CARD));
        PaymentProvider.PaymentResult result = provider.capture(payment.getProviderTransactionId(), payment.getAmount());
        if (result.success()) {
            payment.setStatus("COMPLETED");
            payment.setProviderStatus(result.providerStatus());
            recordEvent(payment.getId(), PaymentEvent.EventType.COMPLETED, result.providerTransactionId(),
                    provider.getClass().getSimpleName(), 200, result.providerStatus());
        }
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse refund(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable"));
        if (!"COMPLETED".equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Paiement non remboursable");
        }
        PaymentProvider provider = PROVIDERS.getOrDefault(payment.getPaymentMethod(), PROVIDERS.get(PaymentProvider.TYPE_CARD));
        PaymentProvider.PaymentResult result = provider.refund(payment.getProviderTransactionId(), payment.getAmount());
        if (result.success()) {
            payment.setStatus("REFUNDED");
            payment.setProviderStatus(result.providerStatus());
            recordEvent(payment.getId(), PaymentEvent.EventType.REFUNDED, result.providerTransactionId(),
                    provider.getClass().getSimpleName(), 200, "Refund accepted");
        } else {
            payment.setFailedReason(result.failedReason());
            recordEvent(payment.getId(), PaymentEvent.EventType.FAILED, result.providerTransactionId(),
                    provider.getClass().getSimpleName(), 402, result.failedReason());
        }
        return toResponse(paymentRepository.save(payment));
    }

    public List<PaymentResponse> findAll() {
        return paymentRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<PaymentResponse> findByUser(UUID userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
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
        if (request.amount() != null) {
            payment.setAmount(request.amount());
        }
        if (request.status() != null) {
            payment.setStatus(request.status());
        }
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(UUID paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paiement introuvable");
        }
        paymentRepository.deleteById(paymentId);
    }

    private void recordEvent(UUID paymentId, PaymentEvent.EventType status, String providerTxnId,
                             String providerType, Integer httpStatus, String payload) {
        PaymentEvent event = new PaymentEvent();
        event.setId(UUID.randomUUID());
        event.setPaymentId(paymentId);
        event.setStatus(status);
        event.setProviderTransactionId(providerTxnId);
        event.setProviderType(providerType);
        event.setHttpStatus(httpStatus);
        event.setPayload(payload);
        paymentEventRepository.save(event);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getProviderTransactionId(),
                payment.getProviderStatus(),
                payment.getFailedReason(),
                payment.getCreatedAt()
        );
    }
}
