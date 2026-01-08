package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.UpdateProfileRequest;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.UserResponse;
import com.mintstack.finance.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "Kullanıcı yönetimi API'leri")
@SecurityRequirement(name = "bearer")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Kullanıcı profilini getir")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal Jwt jwt) {
        
        // Ensure user exists in database
        userService.getOrCreateUser(jwt);
        
        UserResponse profile = userService.getUserProfile(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    @Operation(summary = "Kullanıcı profilini güncelle")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        UserResponse profile = userService.updateProfile(jwt.getSubject(), request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Profil başarıyla güncellendi"));
    }
}
