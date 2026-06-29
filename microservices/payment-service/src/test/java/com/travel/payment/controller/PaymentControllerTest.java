package com.travel.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.payment.config.SecurityConfig;
import com.travel.payment.dto.CreatePaymentRequest;
import com.travel.payment.dto.PaymentResponse;
import com.travel.payment.dto.UpdatePaymentRequest;
import com.travel.payment.service.PaymentService;
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
import java.time.Instant;
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

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.createPayment(any(CreatePaymentRequest.class)))
                .thenReturn(new PaymentResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "COMPLETED", "CARD", Instant.now()));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePaymentRequest(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "CARD"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAll_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.findAll()).thenReturn(List.of(
                new PaymentResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "COMPLETED", "CARD", Instant.now())));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.getById(id))
                .thenReturn(new PaymentResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "COMPLETED", "CARD", Instant.now()));

        mockMvc.perform(get("/api/payments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.update(eq(id), any(UpdatePaymentRequest.class)))
                .thenReturn(new PaymentResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                        new BigDecimal("25.00"), "COMPLETED", "CARD", Instant.now()));

        mockMvc.perform(put("/api/payments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdatePaymentRequest(new BigDecimal("25.00"), "COMPLETED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(25.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/payments/{id}", id))
                .andExpect(status().isNoContent());

        verify(paymentService).delete(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void refund_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.refund(id))
                .thenReturn(new PaymentResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "REFUNDED", "CARD", Instant.now()));

        mockMvc.perform(post("/api/payments/{id}/refund", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }
}
