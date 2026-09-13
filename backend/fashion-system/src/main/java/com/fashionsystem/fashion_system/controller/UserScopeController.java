package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.UserScopeResponse;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.UserScope;
import com.fashionsystem.fashion_system.service.UserScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes display context for the current internal user without accepting a user id. */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class UserScopeController {
    private final UserScopeService userScopeService;

    /** Returns the Global or Store context resolved from server-side assignments. */
    @GetMapping("/scope")
    public UserScopeResponse getCurrentScope(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        UserScope scope = userScopeService.resolve(principal.userId());
        return new UserScopeResponse(
                scope.kind().name(), scope.storeId(), scope.storeCode(), scope.storeName());
    }
}
