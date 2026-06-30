package com.travel.travel.service;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import com.travel.travel.client.UserServiceClient;

import java.time.LocalDate;
import java.util.*;

@Service
public class Neo4jRecommendationService {

    private final Neo4jClient neo4jClient;
    private final UserServiceClient userServiceClient;

    public Neo4jRecommendationService(Neo4jClient neo4jClient, UserServiceClient userServiceClient) {
        this.neo4jClient = neo4jClient;
        this.userServiceClient = userServiceClient;
    }

    public void syncTrip(UUID id, String title, String origin, String destination, double price, LocalDate departureDate) {
        try {
            neo4jClient.query("""
                    MERGE (t:Trip {id: $id})
                    SET t.title = $title,
                        t.origin = $origin,
                        t.destination = $destination,
                        t.price = $price,
                        t.departureDate = $departureDate
                    """)
                    .bind(id.toString()).to("id")
                    .bind(title).to("title")
                    .bind(origin).to("origin")
                    .bind(destination).to("destination")
                    .bind(price).to("price")
                    .bind(departureDate.toString()).to("departureDate")
                    .run();
        } catch (Exception e) {
            System.err.println("Neo4j syncTrip failed: " + e.getMessage());
        }
    }

    public void syncBooking(UUID userId, UUID tripId, boolean cancelled) {
        try {
            var profile = userServiceClient.getById(userId);
            neo4jClient.query("""
                    MERGE (u:User {id: $userId})
                    SET u.email = $email,
                        u.firstName = $firstName,
                        u.lastName = $lastName,
                        u.role = $role
                    MERGE (t:Trip {id: $tripId})
                    MERGE (u)-[r:SUBSCRIBED]->(t)
                    SET r.cancelled = $cancelled
                    """)
                    .bind(userId.toString()).to("userId")
                    .bind(profile.email()).to("email")
                    .bind(profile.firstName()).to("firstName")
                    .bind(profile.lastName()).to("lastName")
                    .bind(profile.role()).to("role")
                    .bind(tripId.toString()).to("tripId")
                    .bind(cancelled).to("cancelled")
                    .run();
        } catch (Exception e) {
            System.err.println("Neo4j syncBooking failed: " + e.getMessage());
        }
    }

    public void syncFeedback(UUID userId, UUID tripId, int rating) {
        try {
            var profile = userServiceClient.getById(userId);
            neo4jClient.query("""
                    MERGE (u:User {id: $userId})
                    SET u.email = $email,
                        u.firstName = $firstName,
                        u.lastName = $lastName,
                        u.role = $role
                    MERGE (t:Trip {id: $tripId})
                    MERGE (u)-[r:GAVE_FEEDBACK]->(t)
                    SET r.rating = $rating
                    """)
                    .bind(userId.toString()).to("userId")
                    .bind(profile.email()).to("email")
                    .bind(profile.firstName()).to("firstName")
                    .bind(profile.lastName()).to("lastName")
                    .bind(profile.role()).to("role")
                    .bind(tripId.toString()).to("tripId")
                    .bind(rating).to("rating")
                    .run();
        } catch (Exception e) {
            System.err.println("Neo4j syncFeedback failed: " + e.getMessage());
        }
    }

    public List<UUID> getRecommendations(UUID userId) {
        try {
            Collection<Map<String, Object>> rows = neo4jClient.query("""
                    MATCH (u:User {id: $userId})
                    OPTIONAL MATCH (u)-[f:GAVE_FEEDBACK]->(t1:Trip) WHERE f.rating >= 4
                    WITH u, collect(t1.destination) as likedDestinations, avg(t1.price) as avgLikedPrice
                    
                    MATCH (rec:Trip)
                    WHERE NOT (u)-[:SUBSCRIBED {cancelled: false}]->(rec)
                      AND rec.departureDate > $today
                      AND (
                        rec.destination IN likedDestinations OR
                        (avgLikedPrice IS NOT NULL AND rec.price >= avgLikedPrice * 0.7 AND rec.price <= avgLikedPrice * 1.3)
                      )
                    
                    OPTIONAL MATCH (other:User)-[otherF:GAVE_FEEDBACK]->(rec)
                    WITH rec, avg(otherF.rating) as avgOtherRating
                    RETURN rec.id as tripId
                    ORDER BY coalesce(avgOtherRating, 0.0) DESC, rec.departureDate ASC
                    LIMIT 10
                    """)
                    .bind(userId.toString()).to("userId")
                    .bind(LocalDate.now().toString()).to("today")
                    .fetch()
                    .all();

            List<UUID> recs = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                recs.add(UUID.fromString(String.valueOf(row.get("tripId"))));
            }

            if (recs.isEmpty()) {
                Collection<Map<String, Object>> defaultRows = neo4jClient.query("""
                        MATCH (rec:Trip)
                        WHERE rec.departureDate > $today
                        OPTIONAL MATCH (other:User)-[otherF:GAVE_FEEDBACK]->(rec)
                        WITH rec, avg(otherF.rating) as avgOtherRating
                        RETURN rec.id as tripId
                        ORDER BY coalesce(avgOtherRating, 0.0) DESC, rec.departureDate ASC
                        LIMIT 10
                        """)
                        .bind(LocalDate.now().toString()).to("today")
                        .fetch()
                        .all();
                for (Map<String, Object> row : defaultRows) {
                    recs.add(UUID.fromString(String.valueOf(row.get("tripId"))));
                }
            }

            return recs;
        } catch (Exception e) {
            System.err.println("Neo4j recommendations failed: " + e.getMessage());
            return Collections.emptyList();
        }
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
}
