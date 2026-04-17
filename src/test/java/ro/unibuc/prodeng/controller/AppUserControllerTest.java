package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.exception.GlobalExceptionHandler;
import ro.unibuc.prodeng.exception.InvalidCredentialsException;
import ro.unibuc.prodeng.request.ForgotPasswordRequest;
import ro.unibuc.prodeng.request.LoginRequest;
import ro.unibuc.prodeng.request.RegisterRequest;
import ro.unibuc.prodeng.response.LoginResponse;
import ro.unibuc.prodeng.response.UserProfileResponse;
import ro.unibuc.prodeng.service.AppUserService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

@ExtendWith(SpringExtension.class)
class AppUserControllerTest {

    @Mock
    private AppUserService appUserService;

    @InjectMocks
    private AppUserController appUserController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── POST /api/auth/register ───────────────────────────────────────────────

    @Test
    void testRegister_validRequest_returns201WithToken() throws Exception {
        // Arrange
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "secret", "dev", false);
        LoginResponse resp = new LoginResponse("jwt-token", "Alice", "alice@example.com", false);
        when(appUserService.register(any(RegisterRequest.class))).thenReturn(resp);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is("jwt-token")))
                .andExpect(jsonPath("$.name", is("Alice")))
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.isAdmin", is(false)));

        verify(appUserService).register(any(RegisterRequest.class));
    }

    @Test
    void testRegister_duplicateEmail_returns400() throws Exception {
        // Arrange
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "secret", "dev", false);
        when(appUserService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Email already exists: alice@example.com"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Email already exists: alice@example.com")));
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @Test
    void testLogin_validCredentials_returns200WithToken() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest("alice@example.com", "secret");
        LoginResponse resp = new LoginResponse("jwt-token", "Alice", "alice@example.com", false);
        when(appUserService.login(any(LoginRequest.class))).thenReturn(resp);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt-token")))
                .andExpect(jsonPath("$.name", is("Alice")));

        verify(appUserService).login(any(LoginRequest.class));
    }

    @Test
    void testLogin_invalidCredentials_returns401() throws Exception {
        // Arrange
        LoginRequest req = new LoginRequest("alice@example.com", "wrong");
        when(appUserService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("Invalid credentials")));
    }

    // ── POST /api/auth/forgot-password ────────────────────────────────────────

    @Test
    void testForgotPassword_existingEmail_returns204() throws Exception {
        // Arrange
        ForgotPasswordRequest req = new ForgotPasswordRequest("alice@example.com", "newSecret");
        doNothing().when(appUserService).forgotPassword(any(ForgotPasswordRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(appUserService).forgotPassword(any(ForgotPasswordRequest.class));
    }

    @Test
    void testForgotPassword_emailNotFound_returns404() throws Exception {
        // Arrange
        ForgotPasswordRequest req = new ForgotPasswordRequest("nobody@example.com", "newSecret");
        doThrow(new EntityNotFoundException("nobody@example.com"))
                .when(appUserService).forgotPassword(any(ForgotPasswordRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/auth/me ──────────────────────────────────────────────────────

    @Test
    void testGetMe_authenticatedUser_returns200WithProfile() throws Exception {
        // Arrange
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user-1", null, Collections.emptyList());
        UserProfileResponse profile = new UserProfileResponse("user-1", "Alice", "alice@example.com", "dev", false);
        when(appUserService.getProfile("user-1")).thenReturn(profile);

        // Act & Assert
        mockMvc.perform(get("/api/auth/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("user-1")))
                .andExpect(jsonPath("$.name", is("Alice")))
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.group", is("dev")))
                .andExpect(jsonPath("$.isAdmin", is(false)));

        verify(appUserService).getProfile("user-1");
    }

    @Test
    void testGetMe_userNotFound_returns404() throws Exception {
        // Arrange
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("ghost", null, Collections.emptyList());
        when(appUserService.getProfile("ghost")).thenThrow(new EntityNotFoundException("ghost"));

        // Act & Assert
        mockMvc.perform(get("/api/auth/me").principal(auth))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/auth/{id} ─────────────────────────────────────────────────

    @Test
    void testDeleteUser_existingId_returns204() throws Exception {
        // Arrange
        doNothing().when(appUserService).deleteUser("user-1");

        // Act & Assert
        mockMvc.perform(delete("/api/auth/{id}", "user-1"))
                .andExpect(status().isNoContent());

        verify(appUserService).deleteUser("user-1");
    }

    @Test
    void testDeleteUser_nonExistingId_returns404() throws Exception {
        // Arrange
        doThrow(new EntityNotFoundException("unknown"))
                .when(appUserService).deleteUser("unknown");

        // Act & Assert
        mockMvc.perform(delete("/api/auth/{id}", "unknown"))
                .andExpect(status().isNotFound());
    }
}
