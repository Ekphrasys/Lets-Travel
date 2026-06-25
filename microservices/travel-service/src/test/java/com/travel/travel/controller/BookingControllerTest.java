package com.travel.travel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.travel.dto.BookingResponse;
import com.travel.travel.dto.CreateBookingRequest;
import com.travel.travel.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTest {

    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    /** Sets the request principal directly so Spring MVC injects it as Authentication. */
    private static MockHttpServletRequestBuilder withUser(MockHttpServletRequestBuilder builder) {
        return builder.with(request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            return request;
        });
    }

    @Test
    void createBooking_returnsCreated() throws Exception {
        UUID userId = UUID.fromString(USER_ID);
        UUID bookingId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        when(bookingService.createBooking(eq(userId), any(CreateBookingRequest.class)))
                .thenReturn(new BookingResponse(bookingId, tripId, userId, "CONFIRMED",
                        UUID.randomUUID(), Instant.now(), null));

        mockMvc.perform(withUser(post("/api/bookings"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(tripId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void myBookings_returnsList() throws Exception {
        UUID userId = UUID.fromString(USER_ID);
        when(bookingService.findByUser(userId)).thenReturn(List.of());

        mockMvc.perform(withUser(get("/api/bookings/me")))
                .andExpect(status().isOk());
    }

    @Test
    void cancelBooking_returnsOk() throws Exception {
        UUID userId = UUID.fromString(USER_ID);
        UUID bookingId = UUID.randomUUID();
        when(bookingService.cancelBooking(eq(bookingId), eq(userId), eq(false)))
                .thenReturn(new BookingResponse(bookingId, UUID.randomUUID(), userId, "CANCELLED",
                        null, Instant.now(), null));

        mockMvc.perform(withUser(delete("/api/bookings/{id}", bookingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
