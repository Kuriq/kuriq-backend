package com.example.kuriq.controller;

import com.example.kuriq.dto.user.UserResponse;
import com.example.kuriq.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(UserResponse.from(userService.getUser(userId)));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody Map<String, String> body) {
        userService.updateProfile(userId, body.get("name"), body.get("ageGroup"));
        return ResponseEntity.ok(UserResponse.from(userService.getUser(userId)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal String userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/social-accounts")
    public ResponseEntity<List<Map<String, String>>> getSocialAccounts(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(userService.getSocialAccounts(userId));
    }

    @DeleteMapping("/me/social-accounts/{provider}")
    public ResponseEntity<Void> unlinkSocialAccount(
            @PathVariable String provider,
            @AuthenticationPrincipal String userId) {
        userService.unlinkSocialAccount(userId, provider);
        return ResponseEntity.noContent().build();
    }
}