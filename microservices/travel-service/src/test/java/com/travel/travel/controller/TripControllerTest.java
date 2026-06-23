package com.travel.travel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.travel.config.SecurityConfig;
import com.travel.travel.dto.CreateTripRequest;
import com.travel.travel.dto.RouteResponse;
import com.travel.travel.dto.TripResponse;
import com.travel.travel.service.RouteSearchService;
import com.travel.travel.service.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TripService tripService;

    @MockBean
    private RouteSearchService routeSearchService;

    @Test
    void listAll_isPublic() throws Exception {
        UUID id = UUID.randomUUID();
        when(tripService.findAll()).thenReturn(List.of(
                new TripResponse(id, "Paris-Rome", "Paris", "Rome",
                        LocalDate.of(2026, 7, 1), BigDecimal.valueOf(199), 10, "OPEN")));

        mockMvc.perform(get("/api/travels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Paris-Rome"));
    }

    @Test
    void searchRoutes_isPublic() throws Exception {
        when(routeSearchService.search("Paris", "Rome"))
                .thenReturn(List.of(new RouteResponse(List.of("Paris", "Rome"), 120, BigDecimal.valueOf(199))));

        mockMvc.perform(get("/api/travels/routes/search")
                        .param("origin", "Paris")
                        .param("destination", "Rome"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalDurationMin").value(120));
    }

    @Test
    void getById_isPublic() throws Exception {
        UUID id = UUID.randomUUID();
        when(tripService.getById(id))
                .thenReturn(new TripResponse(id, "Trip", "A", "B",
                        LocalDate.of(2026, 8, 1), BigDecimal.TEN, 5, "OPEN"));

        mockMvc.perform(get("/api/travels/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_requiresAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        when(tripService.create(any(CreateTripRequest.class)))
                .thenReturn(new TripResponse(id, "New", "Paris", "Lyon",
                        LocalDate.of(2026, 9, 1), BigDecimal.valueOf(50), 20, "OPEN"));

        mockMvc.perform(post("/api/travels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTripRequest("New", "Paris", "Lyon",
                                        LocalDate.of(2026, 9, 1), BigDecimal.valueOf(50), 20))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_andDelete_adminOnly() throws Exception {
        UUID id = UUID.randomUUID();
        when(tripService.update(eq(id), any(CreateTripRequest.class)))
                .thenReturn(new TripResponse(id, "Updated", "Paris", "Lyon",
                        LocalDate.of(2026, 9, 1), BigDecimal.valueOf(60), 15, "OPEN"));

        mockMvc.perform(put("/api/travels/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTripRequest("Updated", "Paris", "Lyon",
                                        LocalDate.of(2026, 9, 1), BigDecimal.valueOf(60), 15))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/travels/{id}", id))
                .andExpect(status().isNoContent());

        verify(tripService).delete(id);
    }
}
