package com.travel.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void filter_generatesCorrelationIdWhenMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/travels").build());

        filter.filter(exchange, capturingChain()).block();

        assertThat((String) exchange.getAttribute("capturedCorrelationId")).isNotBlank();
    }

    @Test
    void filter_preservesExistingCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/travels")
                        .header("X-Correlation-Id", "corr-123")
                        .build());

        filter.filter(exchange, capturingChain()).block();

        assertThat((String) exchange.getAttribute("capturedCorrelationId")).isEqualTo("corr-123");
    }

    private GatewayFilterChain capturingChain() {
        return exchange -> {
            exchange.getAttributes().put("capturedCorrelationId",
                    exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"));
            return Mono.empty();
        };
    }
}
