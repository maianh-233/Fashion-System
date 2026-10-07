package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.auth.CustomerPasswordResetOtpRequest;
import com.fashionsystem.fashion_system.dto.auth.CustomerVerifyPasswordResetOtpRequest;
import com.fashionsystem.fashion_system.dto.auth.MessageResponse;
import com.fashionsystem.fashion_system.dto.auth.ResetPasswordRequest;
import com.fashionsystem.fashion_system.dto.auth.VerifyPasswordResetOtpResponse;
import com.fashionsystem.fashion_system.service.CustomerPasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/customer/password")
@RequiredArgsConstructor
public class CustomerPasswordResetController {
    private final CustomerPasswordResetService service;

    @PostMapping("/request-otp")
    public ResponseEntity<MessageResponse> request(@Valid @RequestBody CustomerPasswordResetOtpRequest request) {
        return ResponseEntity.ok(service.requestOtp(request));
    }
    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resend(@Valid @RequestBody CustomerPasswordResetOtpRequest request) {
        return ResponseEntity.ok(service.requestOtp(request));
    }
    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyPasswordResetOtpResponse> verify(
            @Valid @RequestBody CustomerVerifyPasswordResetOtpRequest request) {
        return ResponseEntity.ok(service.verifyOtp(request));
    }
    @PostMapping("/reset")
    public ResponseEntity<MessageResponse> reset(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(service.resetPassword(request));
    }
}

