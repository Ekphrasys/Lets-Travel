package com.travel.travel.repository;

import com.travel.travel.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByManagerId(UUID managerId);
}
