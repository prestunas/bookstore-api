package com.ibm.bookstore.service;

import com.ibm.bookstore.config.JwtProperties;
import com.ibm.bookstore.dto.AuthResponse;
import com.ibm.bookstore.dto.LoginRequest;
import com.ibm.bookstore.dto.RegisterRequest;
import com.ibm.bookstore.entity.User;
import com.ibm.bookstore.exception.EmailAlreadyExistsException;
import com.ibm.bookstore.exception.InvalidCredentialsException;
import com.ibm.bookstore.exception.UsernameAlreadyExistsException;
import com.ibm.bookstore.repository.UserRepository;
import com.ibm.bookstore.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    PasswordEncoder passwordEncoder;
    JwtUtils jwtUtils;
    AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-minimum-32-bytes!!");
        props.setExpirationMs(86_400_000L);
        jwtUtils = new JwtUtils(props);

        authService = new AuthService(userRepository, passwordEncoder, jwtUtils);
    }

    // ---- register ----

    @Test
    @DisplayName("register: should create user and return JWT token")
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "Secret1!", "Alice Smith");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo("ROLE_CUSTOMER");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.token()).isNotBlank();
    }

    @Test
    @DisplayName("register: should throw when username is already taken")
    void shouldThrowWhenUsernameExists() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "Secret1!", "Alice Smith");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("alice");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: should throw when email is already registered")
    void shouldThrowWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "Secret1!", "Alice Smith");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());
    }

    // ---- login ----

    @Test
    @DisplayName("login: should return JWT token for valid credentials")
    void shouldLoginWithValidCredentials() {
        String rawPassword = "Secret1!";
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@example.com")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName("Alice Smith")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("alice", rawPassword));

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.token()).isNotBlank();
    }

    @Test
    @DisplayName("login: should throw for unknown username")
    void shouldThrowForUnknownUsername() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login: should throw for wrong password")
    void shouldThrowForWrongPassword() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@example.com")
                .passwordHash(passwordEncoder.encode("correct-password"))
                .fullName("Alice Smith")
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
