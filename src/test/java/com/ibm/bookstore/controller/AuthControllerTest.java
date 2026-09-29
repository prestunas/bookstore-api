package com.ibm.bookstore.controller;

import com.ibm.bookstore.config.JwtProperties;
import com.ibm.bookstore.config.SecurityConfig;
import com.ibm.bookstore.dto.AuthResponse;
import com.ibm.bookstore.dto.LoginRequest;
import com.ibm.bookstore.dto.RegisterRequest;
import com.ibm.bookstore.dto.UserProfileResponse;
import com.ibm.bookstore.exception.GlobalExceptionHandler;
import com.ibm.bookstore.exception.InvalidCredentialsException;
import com.ibm.bookstore.exception.UsernameAlreadyExistsException;
import com.ibm.bookstore.security.JwtAuthenticationFilter;
import com.ibm.bookstore.security.JwtUtils;
import com.ibm.bookstore.service.AuthService;
import com.ibm.bookstore.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtils.class, JwtProperties.class,
        GlobalExceptionHandler.class})
@org.springframework.test.context.TestPropertySource(properties = {
        "bookstore.security.jwt.secret=test-secret-key-minimum-32-bytes!!",
        "bookstore.security.jwt.expiration-ms=86400000"
})
class AuthControllerTest {

    static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    UserService userService;

    @Autowired
    JwtProperties jwtProperties;


    // ---- POST /api/v1/auth/register ----

    @Test
    @DisplayName("POST /register: 201 with token for valid input")
    void shouldReturn201OnValidRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "Secret1!", "Alice Smith");
        AuthResponse stubResponse = new AuthResponse(
                "jwt-token-stub", UUID.randomUUID(), "alice", "alice@example.com", "ROLE_CUSTOMER"
        );

        when(authService.register(any(RegisterRequest.class))).thenReturn(stubResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token-stub"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("POST /register: 400 when username is blank")
    void shouldReturn400WhenUsernameBlank() throws Exception {
        RegisterRequest request = new RegisterRequest("", "alice@example.com", "Secret1!", "Alice Smith");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register: 400 when email is invalid")
    void shouldReturn400WhenEmailInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "not-an-email", "Secret1!", "Alice Smith");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register: 409 when username already taken")
    void shouldReturn409WhenUsernameTaken() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "Secret1!", "Alice Smith");
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UsernameAlreadyExistsException("alice"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ---- POST /api/v1/auth/login ----

    @Test
    @DisplayName("POST /login: 200 with token for valid credentials")
    void shouldReturn200OnValidLogin() throws Exception {
        LoginRequest request = new LoginRequest("alice", "Secret1!");
        AuthResponse stubResponse = new AuthResponse(
                "jwt-token-stub", UUID.randomUUID(), "alice", "alice@example.com", "ROLE_CUSTOMER"
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(stubResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-stub"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("POST /login: 401 for invalid credentials")
    void shouldReturn401OnInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("alice", "wrong-password");
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ---- GET /api/v1/users/me (protected endpoint) ----

    @Test
    @DisplayName("GET /users/me: 401 without Authorization header")
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/me: 200 with valid Bearer token")
    void shouldReturn200WithValidToken() throws Exception {
        JwtUtils utils = new JwtUtils(jwtProperties);
        String token = utils.generateToken(UUID.randomUUID(), "alice", "ROLE_CUSTOMER");

        UserProfileResponse profile = new UserProfileResponse(
                UUID.randomUUID(), "alice", "alice@example.com", "Alice Smith", "ROLE_CUSTOMER", 0
        );
        when(userService.getProfile("alice")).thenReturn(profile);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("GET /users/me/addresses: 200 with address list")
    void shouldReturnAddressesWithValidToken() throws Exception {
        JwtUtils utils = new JwtUtils(jwtProperties);
        String token = utils.generateToken(UUID.randomUUID(), "alice", "ROLE_CUSTOMER");

        com.ibm.bookstore.dto.AddressDto address = new com.ibm.bookstore.dto.AddressDto(
                UUID.randomUUID(), "Alice Smith", "+1-555-0199", "123 Orchard Lane",
                "New York", "NY", "10001", "USA", true
        );
        when(userService.getUserAddresses("alice")).thenReturn(java.util.List.of(address));

        mockMvc.perform(get("/api/v1/users/me/addresses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recipientName").value("Alice Smith"))
                .andExpect(jsonPath("$[0].city").value("New York"));
    }

    @Test
    @DisplayName("POST /users/me/addresses: 201 when adding new address")
    void shouldCreateAddressWithValidToken() throws Exception {
        JwtUtils utils = new JwtUtils(jwtProperties);
        String token = utils.generateToken(UUID.randomUUID(), "alice", "ROLE_CUSTOMER");

        com.ibm.bookstore.dto.CreateAddressRequest request = new com.ibm.bookstore.dto.CreateAddressRequest(
                "Alice Smith", "+1-555-0199", "123 Orchard Lane",
                "New York", "NY", "10001", "USA", true
        );
        com.ibm.bookstore.dto.AddressDto address = new com.ibm.bookstore.dto.AddressDto(
                UUID.randomUUID(), "Alice Smith", "+1-555-0199", "123 Orchard Lane",
                "New York", "NY", "10001", "USA", true
        );
        when(userService.addUserAddress(eq("alice"), any(com.ibm.bookstore.dto.CreateAddressRequest.class)))
                .thenReturn(address);

        mockMvc.perform(post("/api/v1/users/me/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientName").value("Alice Smith"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("GET /users/me: 401 with tampered Bearer token")
    void shouldReturn401WithTamperedToken() throws Exception {
        JwtUtils utils = new JwtUtils(jwtProperties);
        String token = utils.generateToken(UUID.randomUUID(), "alice", "ROLE_CUSTOMER");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }
}
