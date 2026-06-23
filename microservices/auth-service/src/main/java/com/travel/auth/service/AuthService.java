package com.travel.auth.service;

import com.travel.auth.client.UserServiceClient;
import com.travel.auth.dto.AuthResponse;
import com.travel.auth.dto.LoginRequest;
import com.travel.auth.dto.RegisterRequest;
import com.travel.auth.model.UserAuth;
import com.travel.auth.repository.UserAuthRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserServiceClient userServiceClient;

    public AuthService(
            UserAuthRepository userAuthRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserServiceClient userServiceClient
    ) {
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userServiceClient = userServiceClient;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userAuthRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
        }

        UUID userId = UUID.randomUUID();
        UserServiceClient.UserProfile profile = userServiceClient.createUser(
                userId, request.email(), request.firstName(), request.lastName(), "USER"
        );

        UserAuth userAuth = new UserAuth();
        userAuth.setId(userId);
        userAuth.setEmail(request.email());
        userAuth.setPasswordHash(passwordEncoder.encode(request.password()));
        userAuthRepository.save(userAuth);

        String token = jwtService.generateToken(userId, request.email(), profile.role());
        return new AuthResponse(token, userId, profile.role());
    }

    public AuthResponse login(LoginRequest request) {
        UserAuth userAuth = userAuthRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.password(), userAuth.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        }

        UserServiceClient.UserProfile profile = userServiceClient.getByEmail(request.email());
        String token = jwtService.generateToken(userAuth.getId(), userAuth.getEmail(), profile.role());
        return new AuthResponse(token, userAuth.getId(), profile.role());
    }
}
