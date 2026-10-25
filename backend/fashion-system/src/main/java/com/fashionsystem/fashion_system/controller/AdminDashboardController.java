package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.DashboardService;
import com.fashionsystem.fashion_system.service.StoreAccessService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API trang tổng quan quản trị; phạm vi cửa hàng luôn được backend xác định theo nhân viên. */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AdminDashboardController {
    private final DashboardService service;

    /** Bỏ trống {@code storeId}: Global xem toàn hệ thống, nhân viên cửa hàng xem cửa hàng của mình. */
    @GetMapping
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public DashboardResponse overview(Authentication auth, @RequestParam(required = false) UUID storeId) {
        return service.overview(userId(auth), storeId);
    }

    @GetMapping("/stores")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public List<StoreAccessService.StoreOption> stores(Authentication auth) {
        return service.stores(userId(auth));
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
