package com.scheduler.controller;

import com.scheduler.dto.response.UserResponse;
import com.scheduler.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User search")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Search users by username (partial match)")
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }
}
