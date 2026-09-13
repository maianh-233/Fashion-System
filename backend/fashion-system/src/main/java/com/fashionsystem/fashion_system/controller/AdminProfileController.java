package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.profile.AdminProfileResponse;
import com.fashionsystem.fashion_system.dto.profile.ChangeMyPasswordRequest;
import com.fashionsystem.fashion_system.dto.profile.UpdateAdminProfileRequest;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.AdminProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API hồ sơ dành cho chính tài khoản nhân viên/admin đang đăng nhập. */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AdminProfileController {
    private final AdminProfileService adminProfileService;

    @GetMapping
    public AdminProfileResponse getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return adminProfileService.getProfile(user.userId());
    }

    @PatchMapping
    public AdminProfileResponse updateProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateAdminProfileRequest request) {
        return adminProfileService.updateProfile(user.userId(), request);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangeMyPasswordRequest request) {
        adminProfileService.changePassword(user.userId(), request);
    }
}
