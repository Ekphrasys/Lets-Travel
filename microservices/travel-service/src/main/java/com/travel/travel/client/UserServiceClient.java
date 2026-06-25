package com.travel.travel.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

@Component
public class UserServiceClient {

    private final WebClient webClient;
    private final String internalApiKey;

    public UserServiceClient(WebClient.Builder webClientBuilder,
                             @Value("${internal.api-key}") String internalApiKey) {
        this.webClient = webClientBuilder.baseUrl("http://user-service").build();
        this.internalApiKey = internalApiKey;
    }

    public ManagerInfo getManagerInfo(UUID managerId) {
        try {
            return webClient.get()
                    .uri("/api/users/internal/{id}", managerId)
                    .header("X-Internal-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(ManagerInfo.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            return null;
        }
    }

    public record ManagerInfo(UUID id, String email, String firstName, String lastName, String role) {}
}
