package com.travel.auth.service;

import com.travel.auth.client.UserServiceClient;
import com.travel.auth.dto.LoginRequest;
import com.travel.auth.dto.RegisterRequest;
import com.travel.auth.model.UserAuth;
import com.travel.auth.repository.UserAuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private AuthService authService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void register_createsUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("new@test.com", "password123", "Jean", "Dupont");
        when(userAuthRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userServiceClient.createUser(any(UUID.class), any(), any(), any(), any()))
                .thenAnswer(inv -> new UserServiceClient.UserProfile(
                        inv.getArgument(0), "new@test.com", "Jean", "Dupont", "USER"));
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userAuthRepository.save(any(UserAuth.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(UUID.class), any(), any())).thenReturn("jwt-token");

        var response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isNotNull();
        assertThat(response.role()).isEqualTo("USER");
        verify(userAuthRepository).save(any(UserAuth.class));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userAuthRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("dup@test.com", "password123", "A", "B")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email déjà utilisé");
    }

    @Test
    void login_validCredentials_returnsToken() {
        UserAuth userAuth = new UserAuth();
        userAuth.setId(userId);
        userAuth.setEmail("user@test.com");
        userAuth.setPasswordHash("hash");

        when(userAuthRepository.findByEmail("user@test.com")).thenReturn(Optional.of(userAuth));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(userServiceClient.getByEmail("user@test.com"))
                .thenReturn(new UserServiceClient.UserProfile(userId, "user@test.com", "U", "S", "ADMIN"));
        when(jwtService.generateToken(userId, "user@test.com", "ADMIN")).thenReturn("jwt-admin");

        var response = authService.login(new LoginRequest("user@test.com", "password123"));

        assertThat(response.token()).isEqualTo("jwt-admin");
        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    void login_unknownEmail_throwsUnauthorized() {
        when(userAuthRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@test.com", "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        UserAuth userAuth = new UserAuth();
        userAuth.setPasswordHash("hash");
        when(userAuthRepository.findByEmail("user@test.com")).thenReturn(Optional.of(userAuth));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@test.com", "wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email ou mot de passe incorrect");
    }
}
