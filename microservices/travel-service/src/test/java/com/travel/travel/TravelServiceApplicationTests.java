package com.travel.travel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TravelServiceApplicationTests {

    @MockBean
    private Neo4jClient neo4jClient;

    @MockBean
    private com.travel.travel.search.TripSearchRepository tripSearchRepository;

    @Test
    void contextLoads() {
    }
}
