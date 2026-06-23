package com.travel.travel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteSearchServiceTest {

    @Mock
    private Neo4jClient neo4jClient;

    private RouteSearchService routeSearchService;

    @BeforeEach
    void setUp() {
        routeSearchService = new RouteSearchService(neo4jClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_mapsNeo4jRows() {
        Neo4jClient.UnboundRunnableSpec unbound = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.OngoingBindSpec<Object, Neo4jClient.RunnableSpec> bindOrigin = mock(Neo4jClient.OngoingBindSpec.class);
        Neo4jClient.OngoingBindSpec<Object, Neo4jClient.RunnableSpec> bindDestination = mock(Neo4jClient.OngoingBindSpec.class);
        Neo4jClient.RunnableSpec runnable = mock(Neo4jClient.RunnableSpec.class);
        Neo4jClient.RecordFetchSpec<Map<String, Object>> fetchSpec = mock(Neo4jClient.RecordFetchSpec.class);

        when(neo4jClient.query(anyString())).thenReturn(unbound);
        when(unbound.bind(any())).thenReturn(bindOrigin);
        when(bindOrigin.to("origin")).thenReturn(runnable);
        when(runnable.bind(any())).thenReturn(bindDestination);
        when(bindDestination.to("destination")).thenReturn(runnable);
        when(runnable.fetch()).thenReturn(fetchSpec);
        when(fetchSpec.all()).thenReturn(List.of(
                Map.of("cities", List.of("Paris", "Lyon"), "totalDuration", 120, "totalPrice", 45.5)
        ));

        var routes = routeSearchService.search("Paris", "Lyon");

        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).cities()).containsExactly("Paris", "Lyon");
        assertThat(routes.get(0).totalDurationMin()).isEqualTo(120);
        assertThat(routes.get(0).totalPrice()).isEqualByComparingTo(new BigDecimal("45.5"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_emptyResults() {
        Neo4jClient.UnboundRunnableSpec unbound = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.OngoingBindSpec<Object, Neo4jClient.RunnableSpec> bindOrigin = mock(Neo4jClient.OngoingBindSpec.class);
        Neo4jClient.OngoingBindSpec<Object, Neo4jClient.RunnableSpec> bindDestination = mock(Neo4jClient.OngoingBindSpec.class);
        Neo4jClient.RunnableSpec runnable = mock(Neo4jClient.RunnableSpec.class);
        Neo4jClient.RecordFetchSpec<Map<String, Object>> fetchSpec = mock(Neo4jClient.RecordFetchSpec.class);

        when(neo4jClient.query(anyString())).thenReturn(unbound);
        when(unbound.bind(any())).thenReturn(bindOrigin);
        when(bindOrigin.to(anyString())).thenReturn(runnable);
        when(runnable.bind(any())).thenReturn(bindDestination);
        when(bindDestination.to(anyString())).thenReturn(runnable);
        when(runnable.fetch()).thenReturn(fetchSpec);
        when(fetchSpec.all()).thenReturn(List.of());

        assertThat(routeSearchService.search("X", "Y")).isEmpty();
    }
}
