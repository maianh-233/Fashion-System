package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.StatisticsPeriod;
import com.fashionsystem.fashion_system.service.StatisticsService;
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

/** API báo cáo thống kê; phạm vi cửa hàng luôn được backend xác định theo nhân viên. */
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AdminStatisticsController {
    private final StatisticsService service;

    /** Bỏ trống {@code storeId}: Global xem toàn hệ thống; bỏ trống {@code orderType}: mọi loại đơn. */
    @GetMapping
    @PreAuthorize("hasAuthority('STATISTICS_VIEW')")
    public StatisticsResponse report(Authentication auth,
                                     @RequestParam(required = false) UUID storeId,
                                     @RequestParam(defaultValue = "LAST_30_DAYS") StatisticsPeriod period,
                                     @RequestParam(required = false) OrderType orderType) {
        return service.report(userId(auth), storeId, period, orderType);
    }

    @GetMapping("/stores")
    @PreAuthorize("hasAuthority('STATISTICS_VIEW')")
    public List<StoreAccessService.StoreOption> stores(Authentication auth) {
        return service.stores(userId(auth));
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
