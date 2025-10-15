package com.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired private MockMvc        mockMvc;
    @Autowired private ObjectMapper   objectMapper;
    @Autowired private UserRepository userRepository;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        token = registerAndGetToken("alice_smith");
        registerAndGetToken("alice_jones");
        registerAndGetToken("bob");
    }

    @Test
    void searchUsers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/users/search").param("query", "alice"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchUsers_partialMatch_returnsMatchingUsers() throws Exception {
        mockMvc.perform(get("/users/search")
                        .header("Authorization", "Bearer " + token)
                        .param("query", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("alice_smith"))
                .andExpect(jsonPath("$[1].username").value("alice_jones"));
    }

    @Test
    void searchUsers_caseInsensitive_returnsResults() throws Exception {
        mockMvc.perform(get("/users/search")
                        .header("Authorization", "Bearer " + token)
                        .param("query", "ALICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void searchUsers_noMatch_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/users/search")
                        .header("Authorization", "Bearer " + token)
                        .param("query", "zzzz_nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchUsers_responseDoesNotExposePassword() throws Exception {
        mockMvc.perform(get("/users/search")
                        .header("Authorization", "Bearer " + token)
                        .param("query", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String registerAndGetToken(String username) throws Exception {
        SignupRequest req = new SignupRequest();
        req.setUsername(username);
        req.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }
}
