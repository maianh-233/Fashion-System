package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.CustomerProfileDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Hồ sơ của khách đang đăng nhập. */
@RestController
@RequestMapping("/api/customers/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerProfileController {
    private final CustomerProfileService profiles;

    @GetMapping("/profile")
    public CustomerProfileDtos.Profile profile(@AuthenticationPrincipal AuthenticatedCustomer customer) {
        return profiles.profile(customer.customerId());
    }

    @PutMapping("/profile")
    public CustomerProfileDtos.Profile update(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                              @Valid @RequestBody CustomerProfileDtos.UpdateProfile request) {
        try (var ignored = scope(customer)) {
            return profiles.update(customer.customerId(), request);
        }
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthenticatedCustomer customer,
                               @Valid @RequestBody CustomerProfileDtos.ChangePassword request) {
        try (var ignored = scope(customer)) {
            profiles.changePassword(customer.customerId(), request);
        }
    }

    /** Audit nghiệp vụ chỉ có tác nhân nhân viên / hệ thống: thao tác tự phục vụ của khách ghi như tác vụ hệ thống. */
    private static BusinessAuditContextFactory.JobScope scope(AuthenticatedCustomer customer) {
        return BusinessAuditContextFactory.systemJob("CUSTOMER_PROFILE:" + customer.customerId());
    }
}
