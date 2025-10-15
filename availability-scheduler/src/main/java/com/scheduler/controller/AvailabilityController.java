package com.scheduler.controller;

import com.scheduler.dto.request.BusySlotRequest;
import com.scheduler.dto.response.BusySlotResponse;
import com.scheduler.dto.response.TimeSlotResponse;
import com.scheduler.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Manage busy slots and find common availability")
@SecurityRequirement(name = "BearerAuth")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @Operation(summary = "Mark a busy time slot")
    @PostMapping
    public ResponseEntity<BusySlotResponse> createSlot(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BusySlotRequest request) {

        BusySlotResponse response =
                availabilityService.createSlot(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all your busy slots")
    @GetMapping("/my")
    public ResponseEntity<List<BusySlotResponse>> getMySlots(
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                availabilityService.getMySlots(userDetails.getUsername()));
    }

    @Operation(summary = "Update an existing busy slot")
    @PutMapping("/{id}")
    public ResponseEntity<BusySlotResponse> updateSlot(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody BusySlotRequest request) {

        return ResponseEntity.ok(
                availabilityService.updateSlot(userDetails.getUsername(), id, request));
    }

    @Operation(summary = "Delete a busy slot")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        availabilityService.deleteSlot(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Find common free time slots for multiple users on a given date")
    @GetMapping("/common")
    public ResponseEntity<List<TimeSlotResponse>> findCommonAvailability(
            @Parameter(description = "Comma-separated list of usernames")
            @RequestParam List<String> usernames,

            @Parameter(description = "Date in yyyy-MM-dd format")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(
                availabilityService.findCommonAvailability(usernames, date));
    }
}
