package com.mintstack.finance.controller;

import com.mintstack.finance.dto.response.AdminDashboardResponse;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.UserAdminResponse;
import com.mintstack.finance.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * Get admin dashboard statistics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        AdminDashboardResponse dashboard = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * Get all users with pagination
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserAdminResponse>>> getUsers(Pageable pageable) {
        Page<UserAdminResponse> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Get user by ID
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserAdminResponse>> getUser(@PathVariable UUID id) {
        UserAdminResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * Search users
     */
    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<Page<UserAdminResponse>>> searchUsers(
            @RequestParam String query,
            Pageable pageable) {
        Page<UserAdminResponse> users = adminService.searchUsers(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Activate user
     */
    @PutMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable UUID id) {
        adminService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Kullanıcı aktif edildi"));
    }

    /**
     * Deactivate user
     */
    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable UUID id) {
        adminService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Kullanıcı devre dışı bırakıldı"));
    }
}
