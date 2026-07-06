package com.travel.travel.service;

import com.travel.travel.model.Trip;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TripGraphService {

    private final Neo4jClient neo4jClient;

    public TripGraphService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public void syncTrip(Trip trip) {
        try {
            neo4jClient.query("""
                    MERGE (t:Trip {id: $id})
                    SET t.title = $title,
                        t.originCity = $originCity,
                        t.destinationCity = $destinationCity,
                        t.price = $price,
                        t.status = $status,
                        t.departureDate = $departureDate
                    """)
                    .bind(trip.getId().toString()).to("id")
                    .bind(trip.getTitle()).to("title")
                    .bind(trip.getOriginCity()).to("originCity")
                    .bind(trip.getDestinationCity()).to("destinationCity")
                    .bind(trip.getPrice().toPlainString()).to("price")
                    .bind(trip.getStatus()).to("status")
                    .bind(trip.getDepartureDate().toString()).to("departureDate")
                    .run();
        } catch (Exception e) {
            System.err.println("Neo4j syncTrip failed: " + e.getMessage());
        }
    }

    public void syncAllTrips(List<Trip> trips) {
        trips.forEach(this::syncTrip);
    }

    public void deleteTrip(UUID id) {
        try {
            neo4jClient.query("""
                    MATCH (t:Trip {id: $id})
                    DETACH DELETE t
                    """)
                    .bind(id.toString()).to("id")
                    .run();
        } catch (Exception e) {
            System.err.println("Neo4j deleteTrip failed: " + e.getMessage());
        }
    }

    public void recordBooking(UUID userId, UUID tripId, boolean cancelled) {
        try {
            neo4jClient.query("""
                    MERGE (u:User {id: $userId})
                    MERGE (t:Trip {id: $tripId})
                    MERGE (u)-[r:PARTICIPATED_IN]->(t)
                    SET r.cancelled = $cancelled
                    """)
                    .bind(userId.toString()).to("userId")
                    .bind(tripId.toString()).to("tripId")
                    .bind(cancelled).to("cancelled")
                    .run();
        } catch (Exception e) {
            System.err.println("Neo4j recordBooking failed: " + e.getMessage());
        }
    }

    public void recordFeedback(UUID userId, UUID tripId, int rating) {
        try {
            neo4jClient.query("""
                    MERGE (u:User {id: $userId})
                    MERGE (t:Trip {id: $tripId})
                    MERGE (u)-[r:RATED]->(t)
                    SET r.rating = $rating
                    """)
                    .bind(userId.toString()).to("userId")
                    .bind(tripId.toString()).to("tripId")
                    .bind(rating).to("rating")
                    .run();
        } catch (Exception e) {
            System.err.println("Neo4j recordFeedback failed: " + e.getMessage());
        }
    }

    /**
     * Up to 10 trip ids scored on shared destination/origin history, price affinity
     * and social proof (other users' ratings). Falls back to popular upcoming trips
     * when the user has no usable history yet.
     */
    public List<String> getSuggestedTripIds(UUID userId) {
        try {
            Collection<Map<String, Object>> rows = neo4jClient.query("""
                    MATCH (u:User {id: $userId})
                    OPTIONAL MATCH (u)-[p:PARTICIPATED_IN {cancelled: false}]->(participated:Trip)
                    WITH u,
                         avg(toFloat(participated.price)) AS avgPrice,
                         collect(DISTINCT participated.destinationCity) AS visitedDestinations,
                         collect(DISTINCT participated.originCity) AS usedOrigins
                    OPTIONAL MATCH (u)-[r:RATED]->(rated:Trip) WHERE r.rating >= 4
                    WITH u, avgPrice, usedOrigins,
                         visitedDestinations + [d IN collect(DISTINCT rated.destinationCity) WHERE NOT d IN visitedDestinations] AS preferredDestinations

                    MATCH (candidate:Trip)
                    WHERE NOT (u)-[:PARTICIPATED_IN {cancelled: false}]->(candidate)
                      AND candidate.status = 'ACTIVE'
                      AND candidate.departureDate > $today
                    OPTIONAL MATCH (other:User)-[otherR:RATED]->(candidate)
                    WITH candidate, avg(otherR.rating) AS avgOtherRating,
                         CASE WHEN candidate.destinationCity IN preferredDestinations THEN 3 ELSE 0 END +
                         CASE WHEN candidate.originCity IN usedOrigins THEN 2 ELSE 0 END +
                         CASE WHEN avgPrice > 0 AND abs(toFloat(candidate.price) - avgPrice) < avgPrice * 0.3 THEN 2 ELSE 0 END +
                         CASE WHEN avgPrice > 0 AND abs(toFloat(candidate.price) - avgPrice) < avgPrice * 0.5 THEN 1 ELSE 0 END AS score
                    WHERE score > 0
                    RETURN candidate.id AS tripId
                    ORDER BY score DESC, coalesce(avgOtherRating, 0.0) DESC, candidate.departureDate ASC
                    LIMIT 10
                    """)
                    .bind(userId.toString()).to("userId")
                    .bind(LocalDate.now().toString()).to("today")
                    .fetch()
                    .all();

            List<String> tripIds = toIdList(rows);
            if (!tripIds.isEmpty()) {
                return tripIds;
            }

            Collection<Map<String, Object>> popular = neo4jClient.query("""
                    MATCH (candidate:Trip)
                    WHERE candidate.status = 'ACTIVE' AND candidate.departureDate > $today
                    OPTIONAL MATCH (other:User)-[otherR:RATED]->(candidate)
                    WITH candidate, avg(otherR.rating) AS avgOtherRating
                    RETURN candidate.id AS tripId
                    ORDER BY coalesce(avgOtherRating, 0.0) DESC, candidate.departureDate ASC
                    LIMIT 10
                    """)
                    .bind(LocalDate.now().toString()).to("today")
                    .fetch()
                    .all();
            return toIdList(popular);
        } catch (Exception e) {
            System.err.println("Neo4j getSuggestedTripIds failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> toIdList(Collection<Map<String, Object>> rows) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ids.add(String.valueOf(row.get("tripId")));
        }
        return ids;
    }
}
