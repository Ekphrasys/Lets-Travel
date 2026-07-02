package com.travel.user.controller;

import com.travel.user.dto.*;
import com.travel.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
                request.id(), request.email(), request.firstName(), request.lastName(), request.role() != null ? request.role() : "TRAVELER");
        return userService.createUser(internalUser);
    }

    @GetMapping("/internal/by-email/{email}")
    @PreAuthorize("hasRole('INTERNAL')")
    public UserResponse byEmailInternal(@PathVariable String email) {
        return userService.getByEmail(email);
    }

    @GetMapping("/internal/{id}")
    @PreAuthorize("hasRole('INTERNAL')")
    public UserResponse getByIdInternal(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        userService.audit("USER_ME", userId, userId.toString(), "OK");
        return userService.getById(userId);
    }

    @PutMapping("/me")
    public UserResponse updateMe(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        UserResponse response = userService.updateMe(userId, request);
        userService.audit("PROFILE_UPDATE", userId, userId.toString(), "OK");
        return response;
    }

    @PostMapping("/me/consent")
    @ResponseStatus(HttpStatus.CREATED)
    public UserConsentResponse recordConsent(Authentication authentication, @Valid @RequestBody UserConsentRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return userService.recordConsent(userId, request);
    }

    @GetMapping("/me/consent")
    public List<UserConsentResponse> myConsents(Authentication authentication) {
        return userService.listConsents(UUID.fromString(authentication.getName()));
    }

    @GetMapping(value = "/me/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDataExportResponse exportMyData(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserDataExportResponse response = userService.exportUserData(userId);
        userService.audit("DATA_EXPORT", userId, userId.toString(), "OK");
        return response;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> listAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAVEL_MANAGER') or #id.toString() == authentication.name")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        userService.anonymizeAndDelete(userId);
        userService.audit("ACCOUNT_DELETION", userId, userId.toString(), "ANONYMIZED");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        userService.audit("ADMIN_DELETE", id, id.toString(), "ANONYMIZED");
        userService.delete(id);
    }

    @GetMapping("/managers")
    public List<UserResponse> listManagers() {
        return userService.findManagers();
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse report(@Valid @RequestBody CreateReportRequest request, Authentication authentication) {
        UUID reporterId = UUID.fromString(authentication.getName());
        return userService.createReport(reporterId, request);
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReportResponse> reports() {
        return userService.findAllReports();
    }

    @PutMapping("/reports/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportResponse resolveReport(@PathVariable UUID id) {
        return userService.resolveReport(id);
    }

    @GetMapping("/{id}/report-counts")
    public ReportCountsResponse getReportCounts(@PathVariable UUID id) {
        return userService.getReportCounts(id);
    }

    @GetMapping("/{id}/stats")
    public UserStatsResponse getUserStats(@PathVariable UUID id) {
        return userService.getUserStats(id);
    }
}

