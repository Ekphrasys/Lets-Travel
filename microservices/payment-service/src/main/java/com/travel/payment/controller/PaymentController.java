package com.travel.payment.controller;

import com.travel.payment.dto.CreatePaymentRequest;
import com.travel.payment.dto.PaymentIntentResponse;
import com.travel.payment.dto.PaymentResponse;
import com.travel.payment.dto.UpdatePaymentRequest;
import com.travel.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/me")
    public List<PaymentResponse> myPayments(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return paymentService.findByUser(userId);
    }

    @PostMapping("/internal/intent")
    @PreAuthorize("hasRole('INTERNAL')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentIntentResponse createIntent(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createIntent(request);
    }

    @PostMapping("/internal/{id}/confirm")
    @PreAuthorize("hasRole('INTERNAL')")
    public PaymentResponse confirmIntent(@PathVariable UUID id) {
        return paymentService.confirmIntent(id);
    }

    @PostMapping("/internal/{id}/cancel")
    @PreAuthorize("hasRole('INTERNAL')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelIntent(@PathVariable UUID id) {
        paymentService.cancelIntent(id);
    }

    @PostMapping("/internal")
    @PreAuthorize("hasRole('INTERNAL')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createInternal(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @PostMapping("/internal/{id}/refund")
    @PreAuthorize("hasRole('INTERNAL')")
    public PaymentResponse refundInternal(@PathVariable UUID id) {
        return paymentService.refund(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PaymentResponse> listAll() {
        return paymentService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse getById(@PathVariable UUID id) {
        return paymentService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePaymentRequest request) {
        return paymentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        paymentService.delete(id);
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse refund(@PathVariable UUID id) {
        return paymentService.refund(id);
    }
}
