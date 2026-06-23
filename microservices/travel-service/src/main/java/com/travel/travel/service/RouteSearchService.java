package com.travel.travel.service;

import com.travel.travel.dto.RouteResponse;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class RouteSearchService {

    private final Neo4jClient neo4jClient;

    public RouteSearchService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @SuppressWarnings("unchecked")
    public List<RouteResponse> search(String origin, String destination) {
        Collection<Map<String, Object>> rows = neo4jClient.query("""
                        MATCH path = (o:City {name: $origin})-[:CONNECTS_TO*1..3]->(d:City {name: $destination})
                        WITH path,
                             [n IN nodes(path) | n.name] AS cities,
                             reduce(t = 0, r IN relationships(path) | t + r.duration_min) AS totalDuration,
                             reduce(p = 0.0, r IN relationships(path) | p + r.price) AS totalPrice
                        RETURN cities, totalDuration, totalPrice
                        ORDER BY totalPrice ASC
                        LIMIT 5
                        """)
                .bind(origin).to("origin")
                .bind(destination).to("destination")
                .fetch()
                .all();

        List<RouteResponse> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<String> cities = new ArrayList<>();
            Object citiesObj = row.get("cities");
            if (citiesObj instanceof List<?> list) {
                list.forEach(c -> cities.add(String.valueOf(c)));
            }
            int duration = ((Number) row.get("totalDuration")).intValue();
            BigDecimal price = new BigDecimal(String.valueOf(row.get("totalPrice")));
            results.add(new RouteResponse(cities, duration, price));
        }
        return results;
    }
}
