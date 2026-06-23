package com.travel.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "travel-jwt-secret-key-minimum-32-characters-long");
        ReflectionTestUtils.setField(jwtService, "expirationHours", 24L);
    }

    @Test
    void generateToken_returnsValidJwt() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, "test@travel.com", "ROLE_USER");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }
}
