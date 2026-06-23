package com.travel.travel.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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

    public UserProfile getById(UUID id) {
        try {
            return webClient.get()
                    .uri("/api/users/internal/{id}", id)
                    .header("X-Internal-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(UserProfile.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to fetch user profile: " + e.getMessage());
            return new UserProfile(id, "unknown@travel.com", "Unknown", "User", "TRAVELER");
        }
    }

    public record UserProfile(UUID id, String email, String firstName, String lastName, String role) {
    }
}
