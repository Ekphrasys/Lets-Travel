package com.travel.travel.search;

import com.travel.travel.dto.TripResponse;
import com.travel.travel.model.Trip;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class TripSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final TripSearchRepository tripSearchRepository;

    public TripSearchService(ElasticsearchOperations elasticsearchOperations,
                             TripSearchRepository tripSearchRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.tripSearchRepository = tripSearchRepository;
    }

    public List<TripResponse> search(String queryText) {
        if (queryText == null || queryText.isBlank()) return List.of();

        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(mm -> mm
                                .query(queryText)
                                .fields(List.of("title^2", "originCity^1.5", "destinationCity^1.5"))
                                .fuzziness("AUTO")
                        )
                )
                .withMaxResults(50)
                .build();

        SearchHits<TripDocument> hits = elasticsearchOperations.search(query, TripDocument.class);
        return hits.stream().map(SearchHit::getContent).map(this::toResponse).toList();
    }

    public List<String> autocomplete(String prefix) {
        if (prefix == null || prefix.isBlank()) return List.of();

        String lowerPrefix = prefix.toLowerCase();

        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .should(s -> s.prefix(p -> p.field("originCity").value(lowerPrefix)))
                                .should(s -> s.prefix(p -> p.field("destinationCity").value(lowerPrefix)))
                                .should(s -> s.prefix(p -> p.field("title").value(lowerPrefix)))
                                .minimumShouldMatch("1")
                        )
                )
                .withMaxResults(20)
                .build();

        SearchHits<TripDocument> hits = elasticsearchOperations.search(query, TripDocument.class);
        return hits.stream()
                .map(SearchHit::getContent)
                .flatMap(doc -> Stream.of(doc.getTitle(), doc.getOriginCity(), doc.getDestinationCity()))
                .filter(s -> s != null && s.toLowerCase().startsWith(lowerPrefix))
                .distinct()
                .sorted()
                .limit(10)
                .toList();
    }

    public void indexTrip(Trip trip) {
        tripSearchRepository.save(toDocument(trip));
    }

    public void removeTrip(UUID id) {
        tripSearchRepository.deleteById(id.toString());
    }

    public void reindexAll(List<Trip> trips) {
        tripSearchRepository.deleteAll();
        tripSearchRepository.saveAll(trips.stream().map(this::toDocument).toList());
    }

    private TripDocument toDocument(Trip trip) {
        TripDocument doc = new TripDocument();
        doc.setId(trip.getId().toString());
        doc.setTitle(trip.getTitle());
        doc.setOriginCity(trip.getOriginCity());
        doc.setDestinationCity(trip.getDestinationCity());
        doc.setDepartureDate(trip.getDepartureDate());
        doc.setPrice(trip.getPrice());
        doc.setSeatsAvailable(trip.getSeatsAvailable());
        doc.setStatus(trip.getStatus());
        doc.setManagerId(trip.getManagerId() != null ? trip.getManagerId().toString() : null);
        return doc;
    }

    private TripResponse toResponse(TripDocument doc) {
        return new TripResponse(
                UUID.fromString(doc.getId()),
                doc.getTitle(),
                doc.getOriginCity(),
                doc.getDestinationCity(),
                doc.getDepartureDate(),
                doc.getPrice(),
                doc.getSeatsAvailable(),
                doc.getStatus(),
                doc.getManagerId() != null ? UUID.fromString(doc.getManagerId()) : null
        );
    }
}
