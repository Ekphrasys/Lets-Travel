package com.travel.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.user.dto.CreateUserRequest;
import com.travel.user.dto.UpdateUserRequest;
import com.travel.user.dto.UserResponse;
import com.travel.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void listAll_returnsUsers() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findAll()).thenReturn(List.of(
                new UserResponse(id, "admin@test.com", "Admin", "User", "ADMIN")));

        mockMvc.perform(get("/api/users")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@test.com"));
    }

    @Test
    void createUser_returnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(new UserResponse(id, "new@test.com", "N", "U", "USER"));

        mockMvc.perform(post("/api/users")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateUserRequest(UUID.randomUUID(), "new@test.com", "N", "U", "USER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@test.com"));
    }

    @Test
    void updateAndDelete_adminOperations() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.updateUser(eq(id), any(UpdateUserRequest.class)))
                .thenReturn(new UserResponse(id, "upd@test.com", "U", "P", "USER"));

        mockMvc.perform(put("/api/users/{id}", id)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateUserRequest("upd@test.com", "U", "P", "USER"))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/{id}", id)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(userService).delete(id);
    }
}
