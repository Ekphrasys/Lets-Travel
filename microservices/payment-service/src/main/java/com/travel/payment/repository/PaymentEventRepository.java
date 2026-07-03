package com.travel.payment.repository;

import com.travel.payment.model.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    List<PaymentEvent> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    @Query("SELECT COUNT(e) FROM PaymentEvent e WHERE e.paymentId = :paymentId AND e.status = 'COMPLETED'")
    long countCompletedByPaymentId(@Param("paymentId") UUID paymentId);
}
