package com.travel.travel.service;

import com.travel.travel.model.Trip;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
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
        neo4jClient.query("""
                MERGE (t:Trip {id: $id})
                SET t.title = $title,
                    t.originCity = $originCity,
                    t.destinationCity = $destinationCity,
                    t.price = $price,
                    t.status = $status
                """)
                .bind(trip.getId().toString()).to("id")
                .bind(trip.getTitle()).to("title")
                .bind(trip.getOriginCity()).to("originCity")
                .bind(trip.getDestinationCity()).to("destinationCity")
                .bind(trip.getPrice().toPlainString()).to("price")
                .bind(trip.getStatus()).to("status")
                .run();
    }

    public void syncAllTrips(List<Trip> trips) {
        trips.forEach(this::syncTrip);
    }

    public void recordParticipation(UUID userId, Trip trip) {
        neo4jClient.query("""
                MERGE (u:User {id: $userId})
                MERGE (t:Trip {id: $tripId})
                SET t.title = $title,
                    t.originCity = $originCity,
                    t.destinationCity = $destinationCity,
                    t.price = $price,
                    t.status = $status
                MERGE (u)-[:PARTICIPATED_IN]->(t)
                """)
                .bind(userId.toString()).to("userId")
                .bind(trip.getId().toString()).to("tripId")
                .bind(trip.getTitle()).to("title")
                .bind(trip.getOriginCity()).to("originCity")
                .bind(trip.getDestinationCity()).to("destinationCity")
                .bind(trip.getPrice().toPlainString()).to("price")
                .bind(trip.getStatus()).to("status")
                .run();
    }

    public void recordFeedback(UUID userId, UUID tripId, int rating) {
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
    }

    /**
     * Returns up to 10 trip IDs ordered by personalization score, based on:
     * - destinationCity: preferred destinations from past participation and high ratings (>=4)
     * - originCity: familiar departure cities from past trips
     * - price: similarity to the user's average spend across confirmed trips
     */
    public List<String> getSuggestedTripIds(UUID userId) {
        Collection<Map<String, Object>> rows = neo4jClient.query("""
                MATCH (u:User {id: $userId})-[:PARTICIPATED_IN]->(participated:Trip)
                WITH u,
                     avg(toFloat(participated.price)) AS avgPrice,
                     collect(DISTINCT participated.destinationCity) AS visitedDestinations,
                     collect(DISTINCT participated.originCity) AS usedOrigins
                OPTIONAL MATCH (u)-[r:RATED]->(rated:Trip)
                WHERE r.rating >= 4
                WITH u, avgPrice, visitedDestinations, usedOrigins,
                     collect(DISTINCT rated.destinationCity) AS likedDestinations
                WITH u, avgPrice, usedOrigins,
                     visitedDestinations + [d IN likedDestinations WHERE NOT d IN visitedDestinations] AS preferredDestinations
                MATCH (candidate:Trip)
                WHERE NOT (u)-[:PARTICIPATED_IN]->(candidate)
                  AND candidate.status = 'ACTIVE'
                WITH candidate, preferredDestinations, usedOrigins, avgPrice,
                     CASE WHEN candidate.destinationCity IN preferredDestinations THEN 3 ELSE 0 END +
                     CASE WHEN candidate.originCity IN usedOrigins THEN 2 ELSE 0 END +
                     CASE WHEN avgPrice > 0 AND abs(toFloat(candidate.price) - avgPrice) < avgPrice * 0.3 THEN 2 ELSE 0 END +
                     CASE WHEN avgPrice > 0 AND abs(toFloat(candidate.price) - avgPrice) < avgPrice * 0.5 THEN 1 ELSE 0 END AS score
                WHERE score > 0
                RETURN candidate.id AS tripId, score
                ORDER BY score DESC
                LIMIT 10
                """)
                .bind(userId.toString()).to("userId")
                .fetch()
                .all();

        List<String> tripIds = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            tripIds.add(String.valueOf(row.get("tripId")));
        }
        return tripIds;
    }
}
