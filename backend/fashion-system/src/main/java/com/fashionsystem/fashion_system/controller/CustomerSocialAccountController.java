package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.CustomerSocialAccountDto;
import com.fashionsystem.fashion_system.dto.auth.SocialLoginRequest;
import com.fashionsystem.fashion_system.entity.SocialProvider;
import com.fashionsystem.fashion_system.service.CustomerSocialAccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API quản lý liên kết social của khách hàng. */
@RestController
@RequestMapping("/api/customers/{customerId}/social-accounts")
@PreAuthorize("@ownershipSecurity.isCustomerOrEmployee(#customerId)")
@RequiredArgsConstructor
public class CustomerSocialAccountController {
    private final CustomerSocialAccountService socialAccountService;

    /**
     * Lấy các tài khoản social đã liên kết với khách hàng.
     */
    @GetMapping
    public List<CustomerSocialAccountDto> getList(@PathVariable UUID customerId) {
        return socialAccountService.getList(customerId);
    }

    /**
     * Lấy liên kết social theo provider.
     */
    @GetMapping("/{provider}")
    public CustomerSocialAccountDto getByProvider(
            @PathVariable UUID customerId, @PathVariable SocialProvider provider) {
        return socialAccountService.getByProvider(customerId, provider);
    }

    /**
     * Xác minh token rồi liên kết social cho chính khách hàng đang đăng nhập.
     */
    @PostMapping("/link")
    @PreAuthorize("@ownershipSecurity.isCustomer(#customerId)")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerSocialAccountDto link(
            @PathVariable UUID customerId, @Valid @RequestBody SocialLoginRequest request) {
        return socialAccountService.link(customerId, request);
    }

    /**
     * Gỡ liên kết social của chính khách hàng đang đăng nhập.
     */
    @DeleteMapping("/{provider}")
    @PreAuthorize("@ownershipSecurity.isCustomer(#customerId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable UUID customerId, @PathVariable SocialProvider provider) {
        socialAccountService.unlink(customerId, provider);
    }
}
