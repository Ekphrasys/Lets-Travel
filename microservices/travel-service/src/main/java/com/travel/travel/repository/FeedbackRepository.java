package com.travel.travel.repository;

import com.travel.travel.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    List<Feedback> findByTripIdOrderByCreatedAtDesc(UUID tripId);

    List<Feedback> findByUserId(UUID userId);

    List<Feedback> findByTripIdIn(List<UUID> tripIds);

    List<Feedback> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByTripIdAndUserId(UUID tripId, UUID userId);

    List<Feedback> findAllByOrderByCreatedAtDesc();

    @Query("SELECT f.trip.id, AVG(f.rating), COUNT(f) FROM Feedback f WHERE f.trip.id IN :tripIds GROUP BY f.trip.id")
    List<Object[]> ratingStatsByTripIds(@Param("tripIds") List<UUID> tripIds);
}
