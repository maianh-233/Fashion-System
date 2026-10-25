package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse;
import com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse.MonthStats;
import com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse.RevenuePoint;
import com.fashionsystem.fashion_system.repository.DashboardQueryRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Trang tổng quan quản trị: chỉ số tháng, biểu đồ doanh thu và các danh sách gần đây theo phạm vi nhân viên. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    static final String VIEW = "DASHBOARD_VIEW";
    static final int LOW_STOCK_THRESHOLD = 5;
    private static final int LIST_SIZE = 5;
    private static final int DAYS = 7;
    private static final int YEARS = 5;

    private final StoreAccessService access;
    private final DashboardQueryRepository queries;
    private final StoreRepository stores;

    public List<StoreAccessService.StoreOption> stores(UUID actor) {
        return access.accessibleStores(actor, List.of(VIEW), "Bạn không có quyền xem trang tổng quan");
    }

    /** {@code storeId} null: nhân viên Global xem toàn hệ thống, nhân viên cửa hàng xem cửa hàng của mình. */
    public DashboardResponse overview(UUID actor, UUID storeId) {
        UUID store = access.requireOrAll(actor, storeId, VIEW);
        Scope scope = new Scope(store == null, store == null ? DashboardQueryRepository.ANY_STORE : store);
        String storeName = store == null ? null : stores.findById(store).map(s -> s.getName()).orElse(null);
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();
        PageRequest top = PageRequest.of(0, LIST_SIZE);
        return new DashboardResponse(today, store, storeName, monthStats(scope, today),
                dailyRevenue(scope, today), yearlyRevenue(scope, today),
                queries.bestSellers(scope.all(), scope.id(), monthStart, tomorrow, top),
                access.allowsScope(actor, store, OrderPermissions.VIEW)
                        ? queries.recentOrders(scope.all(), scope.id(), top) : null,
                access.allowsScope(actor, store, "INVENTORY_VIEW")
                        ? queries.lowStock(scope.all(), scope.id(), LOW_STOCK_THRESHOLD, top) : null,
                access.allowsScope(actor, store, "IMPORT_RECEIPT_VIEW")
                        ? queries.recentImports(scope.all(), scope.id(), top) : null,
                access.allowsScope(actor, store, "EXPORT_RECEIPT_VIEW")
                        ? queries.recentExports(scope.all(), scope.id(), top) : null);
    }

    /** Từ đầu tháng đến hết hôm nay, so với cùng số ngày đầu tháng trước (không vượt quá tháng trước). */
    private MonthStats monthStats(Scope s, LocalDate today) {
        LocalDateTime from = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        LocalDateTime previousFrom = from.minusMonths(1);
        LocalDateTime previousTo = previousFrom.plusDays(today.getDayOfMonth());
        if (previousTo.isAfter(from)) previousTo = from;
        return new MonthStats(
                queries.revenue(s.all(), s.id(), from, to), queries.revenue(s.all(), s.id(), previousFrom, previousTo),
                queries.orderCount(s.all(), s.id(), from, to), queries.orderCount(s.all(), s.id(), previousFrom, previousTo),
                queries.newCustomers(s.all(), s.id(), from, to),
                queries.newCustomers(s.all(), s.id(), previousFrom, previousTo),
                queries.itemsSold(s.all(), s.id(), from, to), queries.itemsSold(s.all(), s.id(), previousFrom, previousTo));
    }

    /** Mỗi ngày một truy vấn theo mốc giờ của ứng dụng (DB lưu UTC), không gom nhóm theo ngày trong SQL. */
    private List<RevenuePoint> dailyRevenue(Scope s, LocalDate today) {
        List<RevenuePoint> points = new ArrayList<>(DAYS);
        for (LocalDate day = today.minusDays(DAYS - 1); !day.isAfter(today); day = day.plusDays(1)) {
            points.add(new RevenuePoint(day.toString(),
                    queries.revenue(s.all(), s.id(), day.atStartOfDay(), day.plusDays(1).atStartOfDay())));
        }
        return points;
    }

    private List<RevenuePoint> yearlyRevenue(Scope s, LocalDate today) {
        List<RevenuePoint> points = new ArrayList<>(YEARS);
        for (int year = today.getYear() - YEARS + 1; year <= today.getYear(); year++) {
            LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
            points.add(new RevenuePoint(String.valueOf(year),
                    queries.revenue(s.all(), s.id(), start, start.plusYears(1))));
        }
        return points;
    }

    private record Scope(boolean all, UUID id) {
    }
}
