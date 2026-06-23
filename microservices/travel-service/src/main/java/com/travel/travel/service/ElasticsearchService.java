package com.travel.travel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.travel.model.Trip;
import com.travel.travel.repository.TripRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class ElasticsearchService {

    private final WebClient webClient;
    private final TripRepository tripRepository;

    public ElasticsearchService(
            WebClient.Builder webClientBuilder,
            @Value("${ELASTICSEARCH_HOST:http://localhost:9200}") String esHost,
            TripRepository tripRepository
    ) {
        this.webClient = webClientBuilder.baseUrl(esHost).build();
        this.tripRepository = tripRepository;
    }

    public void indexTrip(Trip trip) {
        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("id", trip.getId().toString());
            doc.put("title", trip.getTitle());
            doc.put("originCity", trip.getOriginCity());
            doc.put("destinationCity", trip.getDestinationCity());
            doc.put("departureDate", trip.getDepartureDate().toString());
            doc.put("price", trip.getPrice().doubleValue());
            doc.put("seatsAvailable", trip.getSeatsAvailable());
            doc.put("status", trip.getStatus());
            doc.put("managerId", trip.getManagerId() != null ? trip.getManagerId().toString() : null);

            webClient.put()
                    .uri("/trips/_doc/{id}", trip.getId().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(doc)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe();
        } catch (Exception e) {
            System.err.println("Elasticsearch index failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<UUID> search(String query) {
        try {
            Map<String, Object> reqBody = new HashMap<>();
            Map<String, Object> multiMatch = new HashMap<>();
            multiMatch.put("query", query);
            multiMatch.put("fields", List.of("title", "originCity", "destinationCity"));
            multiMatch.put("type", "phrase_prefix");
            
            Map<String, Object> q = new HashMap<>();
            q.put("multi_match", multiMatch);
            reqBody.put("query", q);
            reqBody.put("size", 20);

            Map<String, Object> response = webClient.post()
                    .uri("/trips/_search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(reqBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseSearchIds(response);
        } catch (Exception e) {
            System.err.println("Elasticsearch search failed: " + e.getMessage() + ". Falling back to DB search.");
            return fallbackSearch(query);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> autocomplete(String prefix) {
        try {
            Map<String, Object> reqBody = new HashMap<>();
            Map<String, Object> multiMatch = new HashMap<>();
            multiMatch.put("query", prefix);
            multiMatch.put("fields", List.of("title", "originCity", "destinationCity"));
            multiMatch.put("type", "bool_prefix");

            Map<String, Object> q = new HashMap<>();
            q.put("multi_match", multiMatch);
            reqBody.put("query", q);
            reqBody.put("size", 10);

            Map<String, Object> response = webClient.post()
                    .uri("/trips/_search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(reqBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseAutocompleteSuggestions(response, prefix);
        } catch (Exception e) {
            System.err.println("Elasticsearch autocomplete failed: " + e.getMessage() + ". Falling back to DB autocomplete.");
            return fallbackAutocomplete(prefix);
        }
    }

    @SuppressWarnings("unchecked")
    private List<UUID> parseSearchIds(Map<String, Object> response) {
        List<UUID> ids = new ArrayList<>();
        if (response != null && response.containsKey("hits")) {
            Map<String, Object> hitsMap = (Map<String, Object>) response.get("hits");
            if (hitsMap.containsKey("hits")) {
                List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hitsMap.get("hits");
                for (Map<String, Object> hit : hitsList) {
                    Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                    if (source != null && source.containsKey("id")) {
                        ids.add(UUID.fromString(String.valueOf(source.get("id"))));
                    }
                }
            }
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseAutocompleteSuggestions(Map<String, Object> response, String prefix) {
        Set<String> suggestions = new LinkedHashSet<>();
        if (response != null && response.containsKey("hits")) {
            Map<String, Object> hitsMap = (Map<String, Object>) response.get("hits");
            if (hitsMap.containsKey("hits")) {
                List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hitsMap.get("hits");
                for (Map<String, Object> hit : hitsList) {
                    Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                    if (source != null) {
                        String title = String.valueOf(source.get("title"));
                        String origin = String.valueOf(source.get("originCity"));
                        String dest = String.valueOf(source.get("destinationCity"));
                        
                        if (title.toLowerCase().contains(prefix.toLowerCase())) suggestions.add(title);
                        if (origin.toLowerCase().contains(prefix.toLowerCase())) suggestions.add(origin);
                        if (dest.toLowerCase().contains(prefix.toLowerCase())) suggestions.add(dest);
                    }
                }
            }
        }
        return new ArrayList<>(suggestions);
    }

    private List<UUID> fallbackSearch(String query) {
        return tripRepository.findAll().stream()
                .filter(t -> t.getTitle().toLowerCase().contains(query.toLowerCase())
                        || t.getOriginCity().toLowerCase().contains(query.toLowerCase())
                        || t.getDestinationCity().toLowerCase().contains(query.toLowerCase()))
                .map(Trip::getId)
                .toList();
    }

    private List<String> fallbackAutocomplete(String prefix) {
        Set<String> suggestions = new LinkedHashSet<>();
        tripRepository.findAll().stream()
                .filter(t -> t.getTitle().toLowerCase().contains(prefix.toLowerCase())
                        || t.getOriginCity().toLowerCase().contains(prefix.toLowerCase())
                        || t.getDestinationCity().toLowerCase().contains(prefix.toLowerCase()))
                .forEach(t -> {
                    if (t.getTitle().toLowerCase().contains(prefix.toLowerCase())) suggestions.add(t.getTitle());
                    if (t.getOriginCity().toLowerCase().contains(prefix.toLowerCase())) suggestions.add(t.getOriginCity());
                    if (t.getDestinationCity().toLowerCase().contains(prefix.toLowerCase())) suggestions.add(t.getDestinationCity());
                });
        return new ArrayList<>(suggestions);
    }
}
