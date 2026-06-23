package com.travel.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
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

    public UserProfile createUser(UUID id, String email, String firstName, String lastName, String role) {
        return webClient.post()
                .uri("/api/users/internal")
                .header("X-Internal-Key", internalApiKey)
                .bodyValue(Map.of(
                        "id", id,
                        "email", email,
                        "firstName", firstName,
                        "lastName", lastName,
                        "role", role
                ))
                .retrieve()
                .bodyToMono(UserProfile.class)
                .block();
    }

    public UserProfile getByEmail(String email) {
        return webClient.get()
                .uri("/api/users/internal/by-email/{email}", email)
                .header("X-Internal-Key", internalApiKey)
                .retrieve()
                .bodyToMono(UserProfile.class)
                .block();
    }

    public record UserProfile(UUID id, String email, String firstName, String lastName, String role) {
    }
}
