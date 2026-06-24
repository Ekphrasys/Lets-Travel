package com.travel.travel.repository;

import com.travel.travel.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    List<Feedback> findByTripIdOrderByCreatedAtDesc(UUID tripId);

    List<Feedback> findByTripIdIn(List<UUID> tripIds);
}
