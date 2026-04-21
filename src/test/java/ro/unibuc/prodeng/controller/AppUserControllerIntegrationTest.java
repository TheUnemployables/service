package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.repository.AppUserRepository;
import ro.unibuc.prodeng.request.LoginRequest;
import ro.unibuc.prodeng.request.RegisterRequest;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AppUser Login Flow Integration Tests")
class AppUserControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        appUserRepository.deleteAll();
    }

    private String register(String name, String email, String password) throws Exception {
        RegisterRequest req = new RegisterRequest(name, email, password, "group1", false);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    // --- Register ---

    @Test
    void testRegister_validRequest_returnsTokenAndUserInfo() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "password123", "group1", false);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.isAdmin").value(false));
    }

    @Test
    void testRegister_duplicateEmail_returnsBadRequest() throws Exception {
        register("Alice", "alice@example.com", "password123");

        RegisterRequest duplicate = new RegisterRequest("Alice2", "alice@example.com", "other", null, false);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_invalidEmailFormat_returnsBadRequest() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "not-an-email", "password123", null, false);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_missingPassword_returnsBadRequest() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "alice@example.com", "", null, false);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- Login ---

    @Test
    void testLogin_validCredentials_returnsToken() throws Exception {
        register("Alice", "alice@example.com", "password123");

        LoginRequest req = new LoginRequest("alice@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void testLogin_wrongPassword_returnsUnauthorized() throws Exception {
        register("Alice", "alice@example.com", "password123");

        LoginRequest req = new LoginRequest("alice@example.com", "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_nonExistentEmail_returnsUnauthorized() throws Exception {
        LoginRequest req = new LoginRequest("nobody@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_invalidEmailFormat_returnsBadRequest() throws Exception {
        LoginRequest req = new LoginRequest("not-an-email", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- /me (token usage) ---

    @Test
    void testGetMe_withValidToken_returnsProfile() throws Exception {
        String token = register("Alice", "alice@example.com", "password123");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void testGetMe_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetMe_withInvalidToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer this.is.invalid"))
                .andExpect(status().isUnauthorized());
    }
}
