package com.travel.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @Test
    void filter_allowsRequestsWithinCapacity() {
        RateLimitFilter filter = new RateLimitFilter(true, 2, 1, 60);
        AtomicInteger forwarded = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            filter.filter(exchangeFrom("10.0.0.1"), countingChain(forwarded)).block();
        }

        assertThat(forwarded.get()).isEqualTo(2);
    }

    @Test
    void filter_rejectsRequestsOverCapacityWith429() {
        RateLimitFilter filter = new RateLimitFilter(true, 1, 1, 60);
        AtomicInteger forwarded = new AtomicInteger();

        MockServerWebExchange first = exchangeFrom("10.0.0.2");
        filter.filter(first, countingChain(forwarded)).block();

        MockServerWebExchange second = exchangeFrom("10.0.0.2");
        filter.filter(second, countingChain(forwarded)).block();

        assertThat(forwarded.get()).isEqualTo(1);
        assertThat(second.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(second.getResponse().getHeaders().getFirst("Retry-After")).isNotBlank();
    }

    @Test
    void filter_tracksClientsIndependently() {
        RateLimitFilter filter = new RateLimitFilter(true, 1, 1, 60);
        AtomicInteger forwarded = new AtomicInteger();

        filter.filter(exchangeFrom("10.0.0.3"), countingChain(forwarded)).block();
        filter.filter(exchangeFrom("10.0.0.4"), countingChain(forwarded)).block();

        assertThat(forwarded.get()).isEqualTo(2);
    }

    @Test
    void filter_bypassesActuatorEndpoints() {
        RateLimitFilter filter = new RateLimitFilter(true, 1, 1, 60);
        AtomicInteger forwarded = new AtomicInteger();

        for (int i = 0; i < 5; i++) {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/actuator/health")
                            .remoteAddress(new InetSocketAddress("10.0.0.5", 12345))
                            .build());
            filter.filter(exchange, countingChain(forwarded)).block();
        }

        assertThat(forwarded.get()).isEqualTo(5);
    }

    private MockServerWebExchange exchangeFrom(String ip) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/travels")
                        .remoteAddress(new InetSocketAddress(ip, 12345))
                        .build());
    }

    private GatewayFilterChain countingChain(AtomicInteger counter) {
        return exchange -> {
            counter.incrementAndGet();
            return Mono.empty();
        };
    }
}
