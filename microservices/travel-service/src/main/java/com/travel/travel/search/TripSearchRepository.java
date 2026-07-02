package com.travel.travel.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TripSearchRepository extends ElasticsearchRepository<TripDocument, String> {
}
