package com.travel.travel.repository;

import com.travel.travel.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByTripIdInAndStatus(List<UUID> tripIds, String status);

    @Query("SELECT COALESCE(SUM(b.trip.price), 0) FROM Booking b WHERE b.trip.id IN :tripIds AND b.status = :status")
    BigDecimal sumPriceByTripIdInAndStatus(@Param("tripIds") List<UUID> tripIds, @Param("status") String status);
}
