package com.travel.payment.repository;

import com.travel.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Payment> findByBookingId(UUID bookingId);
}
