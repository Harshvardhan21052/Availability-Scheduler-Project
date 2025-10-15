package com.scheduler;

import com.scheduler.dto.request.BusySlotRequest;
import com.scheduler.dto.response.TimeSlotResponse;
import com.scheduler.entity.BusySlot;
import com.scheduler.entity.User;
import com.scheduler.exception.BadRequestException;
import com.scheduler.repository.BusySlotRepository;
import com.scheduler.repository.UserRepository;
import com.scheduler.service.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock private BusySlotRepository busySlotRepository;
    @Mock private UserRepository     userRepository;

    @InjectMocks private AvailabilityService service;

    private User alice;
    private User bob;
    private LocalDate tomorrow;

    @BeforeEach
    void setUp() {
        alice    = User.builder().id(1L).username("alice").password("hash").build();
        bob      = User.builder().id(2L).username("bob").password("hash").build();
        tomorrow = LocalDate.now().plusDays(1);
    }

    // ── Slot Validation ───────────────────────────────────────────────────────

    @Test
    void createSlot_pastDate_throwsBadRequest() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        BusySlotRequest req = new BusySlotRequest();
        req.setDate(LocalDate.now().minusDays(1));
        req.setStartTime(LocalTime.of(9, 0));
        req.setEndTime(LocalTime.of(10, 0));

        assertThatThrownBy(() -> service.createSlot("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("today or a future date");
    }

    @Test
    void createSlot_endBeforeStart_throwsBadRequest() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        BusySlotRequest req = new BusySlotRequest();
        req.setDate(tomorrow);
        req.setStartTime(LocalTime.of(10, 0));
        req.setEndTime(LocalTime.of(9, 0));

        assertThatThrownBy(() -> service.createSlot("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void createSlot_slotTooShort_throwsBadRequest() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));

        BusySlotRequest req = new BusySlotRequest();
        req.setDate(tomorrow);
        req.setStartTime(LocalTime.of(10, 0));
        req.setEndTime(LocalTime.of(10, 10)); // only 10 minutes

        assertThatThrownBy(() -> service.createSlot("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 15 minutes");
    }

    // ── Common Availability ───────────────────────────────────────────────────

    @Test
    void findCommonAvailability_noSlots_returnsFullDay() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        when(busySlotRepository.findByUsernamesAndDate(List.of("alice"), tomorrow))
                .thenReturn(List.of());

        List<TimeSlotResponse> result =
                service.findCommonAvailability(List.of("alice"), tomorrow);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStartTime()).isEqualTo(LocalTime.of(0, 0));
        assertThat(result.get(0).getEndTime()).isEqualTo(LocalTime.of(23, 59));
    }

    @Test
    void findCommonAvailability_overlappingSlots_mergedCorrectly() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        // alice: 09:00-11:00, bob: 10:00-12:00 → merged busy: 09:00-12:00
        List<BusySlot> slots = List.of(
                busySlot(alice, LocalTime.of(9, 0),  LocalTime.of(11, 0)),
                busySlot(bob,   LocalTime.of(10, 0), LocalTime.of(12, 0))
        );

        when(busySlotRepository.findByUsernamesAndDate(
                List.of("alice", "bob"), tomorrow)).thenReturn(slots);

        List<TimeSlotResponse> result =
                service.findCommonAvailability(List.of("alice", "bob"), tomorrow);

        // Free: 00:00-09:00 and 12:00-23:59
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStartTime()).isEqualTo(LocalTime.of(0, 0));
        assertThat(result.get(0).getEndTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(1).getStartTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(result.get(1).getEndTime()).isEqualTo(LocalTime.of(23, 59));
    }

    @Test
    void findCommonAvailability_adjacentSlots_mergedIntoOne() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        // 08:00-10:00 and 10:00-12:00 → treated as one busy block
        List<BusySlot> slots = List.of(
                busySlot(alice, LocalTime.of(8, 0),  LocalTime.of(10, 0)),
                busySlot(alice, LocalTime.of(10, 0), LocalTime.of(12, 0))
        );
        when(busySlotRepository.findByUsernamesAndDate(List.of("alice"), tomorrow))
                .thenReturn(slots);

        List<TimeSlotResponse> result =
                service.findCommonAvailability(List.of("alice"), tomorrow);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEndTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(result.get(1).getStartTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void findCommonAvailability_pastDate_throwsBadRequest() {
        assertThatThrownBy(() ->
                service.findCommonAvailability(List.of("alice"), LocalDate.now().minusDays(1)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void findCommonAvailability_emptyUsernames_throwsBadRequest() {
        assertThatThrownBy(() ->
                service.findCommonAvailability(List.of(), tomorrow))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("At least one username");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private BusySlot busySlot(User user, LocalTime start, LocalTime end) {
        return BusySlot.builder()
                .user(user)
                .date(tomorrow)
                .startTime(start)
                .endTime(end)
                .build();
    }
}
