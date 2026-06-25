package com.travel.travel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
class TripControllerTest {

    private static final String ADMIN_ID = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TripService tripService;

    @MockBean
    private RouteSearchService routeSearchService;

    private static MockHttpServletRequestBuilder withAdmin(MockHttpServletRequestBuilder builder) {
        return builder.with(request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                    ADMIN_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            return request;
        });
    }

    private static TripResponse sampleResponse(UUID id, String title) {
        return new TripResponse(id, title, "Paris", "Rome",
                LocalDate.of(2026, 7, 1), BigDecimal.valueOf(199), 10, "ACTIVE", null);
    }

    @Test
    void listAll_isPublic() throws Exception {
        UUID id = UUID.randomUUID();
        when(tripService.findAll()).thenReturn(List.of(sampleResponse(id, "Paris-Rome")));

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
        when(tripService.getById(id)).thenReturn(sampleResponse(id, "Trip"));

        mockMvc.perform(get("/api/travels/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void create_requiresAdminOrManager() throws Exception {
        UUID id = UUID.randomUUID();
        when(tripService.create(any(CreateTripRequest.class), any(UUID.class)))
                .thenReturn(sampleResponse(id, "New"));

        mockMvc.perform(withAdmin(post("/api/travels"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTripRequest("New", "Paris", "Lyon",
                                        LocalDate.of(2026, 9, 1), BigDecimal.valueOf(50), 20))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    void update_andDelete_adminOrManager() throws Exception {
        UUID id = UUID.randomUUID();
        when(tripService.update(eq(id), any(CreateTripRequest.class), any(UUID.class), anyBoolean()))
                .thenReturn(sampleResponse(id, "Updated"));

        mockMvc.perform(withAdmin(put("/api/travels/{id}", id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTripRequest("Updated", "Paris", "Lyon",
                                        LocalDate.of(2026, 9, 1), BigDecimal.valueOf(60), 15))))
                .andExpect(status().isOk());

        mockMvc.perform(withAdmin(delete("/api/travels/{id}", id)))
                .andExpect(status().isNoContent());

        verify(tripService).delete(eq(id), any(UUID.class), anyBoolean());
    }
}
