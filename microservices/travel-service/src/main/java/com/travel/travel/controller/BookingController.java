package com.travel.travel.controller;

import com.travel.travel.dto.BookingResponse;
import com.travel.travel.dto.CreateBookingRequest;
import com.travel.travel.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return bookingService.createBooking(userId, request);
    }

    @GetMapping("/me")
    public List<BookingResponse> myBookings(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return bookingService.findByUser(userId);
    }

    @GetMapping("/trip/{tripId}")
    public List<BookingResponse> byTrip(@PathVariable UUID tripId, Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        return bookingService.findByTrip(tripId, callerId, isAdmin);
    }

    @DeleteMapping("/{id}")
    public BookingResponse cancel(@PathVariable UUID id, Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        return bookingService.cancelBooking(id, callerId, isAdmin);
    }
}
