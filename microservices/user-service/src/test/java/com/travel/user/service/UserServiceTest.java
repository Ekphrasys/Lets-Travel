package com.travel.user.service;

import com.travel.user.dto.CreateUserRequest;
import com.travel.user.dto.UpdateUserRequest;
import com.travel.user.model.User;
import com.travel.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_success() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsByEmail("a@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = userService.createUser(new CreateUserRequest(id, "a@test.com", "A", "B", null));

        assertThat(response.email()).isEqualTo("a@test.com");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void createUser_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                new CreateUserRequest(UUID.randomUUID(), "dup@test.com", "A", "B", "USER")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getById_notFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getByEmail_found() {
        UUID id = UUID.randomUUID();
        User user = user(id, "x@test.com");
        when(userRepository.findByEmail("x@test.com")).thenReturn(Optional.of(user));

        assertThat(userService.getByEmail("x@test.com").id()).isEqualTo(id);
    }

    @Test
    void findAll_returnsMappedUsers() {
        User user = user(UUID.randomUUID(), "list@test.com");
        when(userRepository.findAll()).thenReturn(List.of(user));

        assertThat(userService.findAll()).hasSize(1);
    }

    @Test
    void updateUser_emailConflict() {
        UUID id = UUID.randomUUID();
        User user = user(id, "old@test.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(id,
                new UpdateUserRequest("new@test.com", "N", "U", "USER")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateUser_success() {
        UUID id = UUID.randomUUID();
        User user = user(id, "old@test.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = userService.updateUser(id,
                new UpdateUserRequest("old@test.com", "New", "Name", "ADMIN"));

        assertThat(response.firstName()).isEqualTo("New");
        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    void delete_notFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(id))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void delete_success() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);

        userService.delete(id);

        verify(userRepository).deleteById(id);
    }

    private static User user(UUID id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("F");
        user.setLastName("L");
        user.setRole("USER");
        return user;
    }
}
