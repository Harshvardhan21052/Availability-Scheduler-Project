package com.scheduler.service;

import com.scheduler.dto.request.BusySlotRequest;
import com.scheduler.dto.response.BusySlotResponse;
import com.scheduler.dto.response.TimeSlotResponse;
import com.scheduler.entity.BusySlot;
import com.scheduler.entity.User;
import com.scheduler.exception.BadRequestException;
import com.scheduler.exception.ForbiddenException;
import com.scheduler.exception.ResourceNotFoundException;
import com.scheduler.repository.BusySlotRepository;
import com.scheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final LocalTime DAY_START = LocalTime.of(0, 0);
    private static final LocalTime DAY_END   = LocalTime.of(23, 59);
    private static final int MIN_SLOT_MINUTES = 15;

    private final BusySlotRepository busySlotRepository;
    private final UserRepository     userRepository;

    @Transactional
    public BusySlotResponse createSlot(String username, BusySlotRequest request) {
        validateSlotRequest(request);

        User user = resolveUser(username);

        BusySlot slot = BusySlot.builder()
                .user(user)
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        return BusySlotResponse.from(busySlotRepository.save(slot));
    }

    @Transactional(readOnly = true)
    public List<BusySlotResponse> getMySlots(String username) {
        User user = resolveUser(username);
        return busySlotRepository.findByUserIdOrderByDateAscStartTimeAsc(user.getId())
                .stream()
                .map(BusySlotResponse::from)
                .toList();
    }

    @Transactional
    public BusySlotResponse updateSlot(String username, Long slotId, BusySlotRequest request) {
        validateSlotRequest(request);

        BusySlot slot = resolveSlotOwnedBy(username, slotId);
        slot.setDate(request.getDate());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());

        return BusySlotResponse.from(busySlotRepository.save(slot));
    }

    @Transactional
    public void deleteSlot(String username, Long slotId) {
        BusySlot slot = resolveSlotOwnedBy(username, slotId);
        busySlotRepository.delete(slot);
    }

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> findCommonAvailability(List<String> usernames, LocalDate date) {

        if (usernames == null || usernames.isEmpty()) {
            throw new BadRequestException("At least one username must be provided");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Date must be today or a future date");
        }

        List<String> cleanUsernames = usernames.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        List<Long> userIds = new ArrayList<>();
        for (String username : cleanUsernames) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found: '" + username + "'"));
            userIds.add(user.getId());
        }

        List<LocalTime[]> allIntervals = new ArrayList<>();
        for (int i = 0; i < cleanUsernames.size(); i++) {
            List<BusySlot> userSlots =
                    busySlotRepository.findByUserIdAndDateOrderByStartTimeAsc(userIds.get(i), date);
            for (BusySlot slot : userSlots) {
                allIntervals.add(new LocalTime[]{slot.getStartTime(), slot.getEndTime()});
            }
        }

        List<LocalTime[]> mergedBusy = mergeIntervals(allIntervals);
        return invertToFreeSlots(mergedBusy);
    }


    private List<LocalTime[]> mergeIntervals(List<LocalTime[]> intervals) {
        if (intervals.isEmpty()) return List.of();

        List<LocalTime[]> sorted = intervals.stream()
                .sorted(Comparator.comparing(a -> a[0]))
                .collect(java.util.stream.Collectors.toList());

        List<LocalTime[]> merged = new ArrayList<>();

        LocalTime mergeStart = sorted.get(0)[0];
        LocalTime mergeEnd   = sorted.get(0)[1];

        for (int i = 1; i < sorted.size(); i++) {
            LocalTime nextStart = sorted.get(i)[0];
            LocalTime nextEnd   = sorted.get(i)[1];

            if (!nextStart.isAfter(mergeEnd)) {
                if (nextEnd.isAfter(mergeEnd)) {
                    mergeEnd = nextEnd;
                }
            } else {
                merged.add(new LocalTime[]{mergeStart, mergeEnd});
                mergeStart = nextStart;
                mergeEnd   = nextEnd;
            }
        }
        merged.add(new LocalTime[]{mergeStart, mergeEnd});
        return merged;
    }


    private List<TimeSlotResponse> invertToFreeSlots(List<LocalTime[]> busySlots) {
        List<TimeSlotResponse> free = new ArrayList<>();
        LocalTime cursor = DAY_START;

        for (LocalTime[] busy : busySlots) {
            if (cursor.isBefore(busy[0])) {
                addIfLongEnough(free, cursor, busy[0]);
            }
            if (cursor.isBefore(busy[1])) {
                cursor = busy[1];
            }
        }

        if (cursor.isBefore(DAY_END)) {
            addIfLongEnough(free, cursor, DAY_END);
        }

        return free;
    }

    private void addIfLongEnough(List<TimeSlotResponse> list,
                                  LocalTime start, LocalTime end) {
        long minutes = java.time.Duration.between(start, end).toMinutes();
        if (minutes >= MIN_SLOT_MINUTES) {
            list.add(new TimeSlotResponse(start, end));
        }
    }


    private void validateSlotRequest(BusySlotRequest request) {
        if (!request.getDate().isAfter(LocalDate.now().minusDays(1))) {
            throw new BadRequestException("Date must be today or a future date");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        long minutes = java.time.Duration.between(
                request.getStartTime(), request.getEndTime()).toMinutes();
        if (minutes < MIN_SLOT_MINUTES) {
            throw new BadRequestException(
                    "Slot duration must be at least " + MIN_SLOT_MINUTES + " minutes");
        }
    }

    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private BusySlot resolveSlotOwnedBy(String username, Long slotId) {
        BusySlot slot = busySlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Busy slot not found with id: " + slotId));

        if (!slot.getUser().getUsername().equals(username)) {
            throw new ForbiddenException("You do not have permission to modify this slot");
        }
        return slot;
    }
}
