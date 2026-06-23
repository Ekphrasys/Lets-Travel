package com.travel.travel.repository;

import com.travel.travel.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    List<Feedback> findByTripIdOrderByCreatedAtDesc(UUID tripId);
    List<Feedback> findByUserId(UUID userId);
    List<Feedback> findByTripIdIn(List<UUID> tripIds);
}
