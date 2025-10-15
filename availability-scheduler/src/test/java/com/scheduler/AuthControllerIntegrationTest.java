package com.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.dto.request.LoginRequest;
import com.scheduler.dto.request.SignupRequest;
import com.scheduler.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc       mockMvc;
    @Autowired private ObjectMapper  objectMapper;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    // ── Signup ────────────────────────────────────────────────────────────────

    @Test
    void signup_validRequest_returns201WithToken() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setUsername("alice");
        req.setPassword("password123");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.message").value("Signup successful"));
    }

    @Test
    void signup_duplicateUsername_returns409() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setUsername("alice");
        req.setPassword("password123");

        // First signup
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second signup with same username
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'alice' is already taken"));
    }

    @Test
    void signup_blankUsername_returns400WithFieldErrors() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setUsername("");
        req.setPassword("password123");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").isNotEmpty());
    }

    @Test
    void signup_passwordTooShort_returns400() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setUsername("alice");
        req.setPassword("abc");   // < 6 chars

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
    }

    @Test
    void signup_invalidUsernameChars_returns400() throws Exception {
        SignupRequest req = new SignupRequest();
        req.setUsername("alice@domain");   // @ not allowed
        req.setPassword("password123");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").isNotEmpty());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        // Register first
        SignupRequest signup = new SignupRequest();
        signup.setUsername("bob");
        signup.setPassword("secure456");
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated());

        // Login
        LoginRequest login = new LoginRequest();
        login.setUsername("bob");
        login.setPassword("secure456");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("bob"))
                .andReturn();

        // Token must be a non-empty string
        String body = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asText();
        assertThat(token).isNotBlank().contains(".");
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        SignupRequest signup = new SignupRequest();
        signup.setUsername("charlie");
        signup.setPassword("realpass");
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setUsername("charlie");
        login.setPassword("wrongpass");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownUser_returns401() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername("nobody");
        login.setPassword("doesntmatter");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }
}
