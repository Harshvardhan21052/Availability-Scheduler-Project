package com.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.dto.request.BusySlotRequest;
import com.scheduler.dto.request.SignupRequest;
import com.scheduler.repository.BusySlotRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvailabilityControllerIntegrationTest {

    @Autowired private MockMvc          mockMvc;
    @Autowired private ObjectMapper     objectMapper;
    @Autowired private UserRepository   userRepository;
    @Autowired private BusySlotRepository busySlotRepository;

    private String aliceToken;
    private String bobToken;
    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() throws Exception {
        busySlotRepository.deleteAll();
        userRepository.deleteAll();
        aliceToken = registerAndLogin("alice", "password123");
        bobToken   = registerAndLogin("bob",   "password123");
    }

    // ── Auth guard ────────────────────────────────────────────────────────────

    @Test
    void getMySlots_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/availability/my"))
                .andExpect(status().isUnauthorized());
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Test
    void createSlot_valid_returns201() throws Exception {
        BusySlotRequest req = slot(tomorrow, "09:00", "10:30");

        mockMvc.perform(post("/availability")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.startTime").value("09:00"))
                .andExpect(jsonPath("$.endTime").value("10:30"));
    }

    @Test
    void createSlot_pastDate_returns400() throws Exception {
        BusySlotRequest req = slot(LocalDate.now().minusDays(1), "09:00", "10:00");

        mockMvc.perform(post("/availability")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date must be today or a future date"));
    }

    @Test
    void createSlot_endBeforeStart_returns400() throws Exception {
        BusySlotRequest req = slot(tomorrow, "10:00", "09:00");

        mockMvc.perform(post("/availability")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End time must be after start time"));
    }

    @Test
    void createSlot_tooShort_returns400() throws Exception {
        BusySlotRequest req = slot(tomorrow, "10:00", "10:10"); // 10 min

        mockMvc.perform(post("/availability")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Slot duration must be at least 15 minutes"));
    }

    @Test
    void updateSlot_ownSlot_returns200() throws Exception {
        long id = createSlotAndGetId(aliceToken, tomorrow, "09:00", "10:00");

        BusySlotRequest update = slot(tomorrow, "11:00", "12:00");

        mockMvc.perform(put("/availability/" + id)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("11:00"))
                .andExpect(jsonPath("$.endTime").value("12:00"));
    }

    @Test
    void updateSlot_anotherUsersSlot_returns403() throws Exception {
        long aliceSlotId = createSlotAndGetId(aliceToken, tomorrow, "09:00", "10:00");

        BusySlotRequest update = slot(tomorrow, "11:00", "12:00");

        mockMvc.perform(put("/availability/" + aliceSlotId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSlot_ownSlot_returns204() throws Exception {
        long id = createSlotAndGetId(aliceToken, tomorrow, "09:00", "10:00");

        mockMvc.perform(delete("/availability/" + id)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());

        // Verify it is gone
        mockMvc.perform(get("/availability/my")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteSlot_anotherUsersSlot_returns403() throws Exception {
        long aliceSlotId = createSlotAndGetId(aliceToken, tomorrow, "09:00", "10:00");

        mockMvc.perform(delete("/availability/" + aliceSlotId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    // ── Common Availability ───────────────────────────────────────────────────

    @Test
    void commonAvailability_noSlots_returnsFullDay() throws Exception {
        mockMvc.perform(get("/availability/common")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("usernames", "alice", "bob")
                        .param("date", tomorrow.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startTime").value("00:00"))
                .andExpect(jsonPath("$[0].endTime").value("23:59"));
    }

    @Test
    void commonAvailability_overlappingSlots_gapIsReturned() throws Exception {
        // alice: 09:00-11:00, bob: 10:00-12:00 → merged busy 09:00-12:00
        createSlotAndGetId(aliceToken, tomorrow, "09:00", "11:00");
        createSlotAndGetId(bobToken,   tomorrow, "10:00", "12:00");

        mockMvc.perform(get("/availability/common")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("usernames", "alice", "bob")
                        .param("date", tomorrow.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].startTime").value("00:00"))
                .andExpect(jsonPath("$[0].endTime").value("09:00"))
                .andExpect(jsonPath("$[1].startTime").value("12:00"))
                .andExpect(jsonPath("$[1].endTime").value("23:59"));
    }

    @Test
    void commonAvailability_unknownUser_returns404() throws Exception {
        mockMvc.perform(get("/availability/common")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("usernames", "alice", "ghost_user_xyz")
                        .param("date", tomorrow.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void commonAvailability_pastDate_returns400() throws Exception {
        mockMvc.perform(get("/availability/common")
                        .header("Authorization", "Bearer " + aliceToken)
                        .param("usernames", "alice")
                        .param("date", LocalDate.now().minusDays(1).toString()))
                .andExpect(status().isBadRequest());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin(String username, String password) throws Exception {
        SignupRequest req = new SignupRequest();
        req.setUsername(username);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private long createSlotAndGetId(String token, LocalDate date,
                                     String start, String end) throws Exception {
        BusySlotRequest req = slot(date, start, end);

        MvcResult result = mockMvc.perform(post("/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();
    }

    private BusySlotRequest slot(LocalDate date, String start, String end) {
        BusySlotRequest req = new BusySlotRequest();
        req.setDate(date);
        req.setStartTime(LocalTime.parse(start));
        req.setEndTime(LocalTime.parse(end));
        return req;
    }
}
