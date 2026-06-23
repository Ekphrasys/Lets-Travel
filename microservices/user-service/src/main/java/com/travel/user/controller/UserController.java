package com.travel.user.controller;

import com.travel.user.dto.CreateUserRequest;
import com.travel.user.dto.UpdateUserRequest;
import com.travel.user.dto.UserResponse;
import com.travel.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/internal")
    @PreAuthorize("hasRole('INTERNAL')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createInternal(@Valid @RequestBody CreateUserRequest request) {
        CreateUserRequest internalUser = new CreateUserRequest(
                request.id(), request.email(), request.firstName(), request.lastName(), "USER");
        return userService.createUser(internalUser);
    }

    @GetMapping("/internal/by-email/{email}")
    @PreAuthorize("hasRole('INTERNAL')")
    public UserResponse byEmailInternal(@PathVariable String email) {
        return userService.getByEmail(email);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getById(UUID.fromString(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> listAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        userService.delete(id);
    }
}
