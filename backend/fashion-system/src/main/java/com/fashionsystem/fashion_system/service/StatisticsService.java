package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse;
import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse.MonthCount;
import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse.Overview;
import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse.Retention;
import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse.SeriesPoint;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.repository.DashboardQueryRepository;
import com.fashionsystem.fashion_system.repository.StatisticsQueryRepository;
import com.fashionsystem.fashion_system.repository.StatisticsQueryRepository.Scope;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Báo cáo thống kê theo kỳ, cửa hàng và loại đơn; phạm vi cửa hàng do backend xác định theo nhân viên. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
    static final String VIEW = "STATISTICS_VIEW";
    private static final int TOP_PRODUCTS = 10;
    private static final int VIP_CUSTOMERS = 10;
    private static final int LOW_STOCK_ROWS = 50;
    private static final int CUSTOMER_MONTHS = 6;

    private final StoreAccessService access;
    private final StatisticsQueryRepository queries;
    private final DashboardQueryRepository dashboardQueries;
    private final StoreRepository stores;

    public List<StoreAccessService.StoreOption> stores(UUID actor) {
        return access.accessibleStores(actor, List.of(VIEW), "Bạn không có quyền xem thống kê");
    }

    /** {@code storeId} null: Global xem toàn hệ thống, nhân viên cửa hàng xem cửa hàng của mình. */
    public StatisticsResponse report(UUID actor, UUID storeId, StatisticsPeriod period, OrderType orderType) {
        UUID store = access.requireOrAll(actor, storeId, VIEW);
        String storeName = store == null ? null : stores.findById(store).map(s -> s.getName()).orElse(null);
        StatisticsPeriod selected = period == null ? StatisticsPeriod.LAST_30_DAYS : period;
        StatisticsPeriod.Range range = selected.resolve(LocalDate.now());
        Scope scope = new Scope(store, orderType);
        LocalDate from = range.from();
        LocalDate to = range.to();

        long[] retention = queries.retention(scope, from, to);
        long[] previousRetention = queries.retention(scope, range.previousFrom(), range.previousTo());
        return new StatisticsResponse(
                selected, range.granularity(),
                from, to, range.previousFrom(), range.previousTo(), store, storeName, orderType,
                overview(scope, range),
                series(scope, range),
                queries.statusCounts(scope, from, to),
                queries.breakdown(scope, from, to, false),
                queries.breakdown(scope, from, to, true),
                queries.topProducts(scope, from, to, TOP_PRODUCTS),
                dashboardQueries.lowStock(store == null, store == null ? DashboardQueryRepository.ANY_STORE : store,
                        DashboardService.LOW_STOCK_THRESHOLD, PageRequest.of(0, LOW_STOCK_ROWS)),
                newCustomersByMonth(store, to.minusDays(1)),
                new Retention(retention[0], retention[1], rate(retention), rate(previousRetention)),
                queries.vipCustomers(scope, from, to, VIP_CUSTOMERS),
                queries.inventory(store, from, to));
    }

    private Overview overview(Scope scope, StatisticsPeriod.Range range) {
        StatisticsQueryRepository.Totals current = queries.totals(scope, range.from(), range.to());
        StatisticsQueryRepository.Totals previous = queries.totals(scope, range.previousFrom(), range.previousTo());
        StatisticsQueryRepository.Cogs cogs = queries.cogs(scope, range.from(), range.to());
        StatisticsQueryRepository.Cogs previousCogs = queries.cogs(scope, range.previousFrom(), range.previousTo());
        return new Overview(current.revenue(), previous.revenue(), current.orders(), previous.orders(),
                current.goodsRevenue().subtract(cogs.cost()), previous.goodsRevenue().subtract(previousCogs.cost()),
                cogs.uncostedQuantity(),
                queries.newCustomers(scope.storeId(), range.from(), range.to()),
                queries.newCustomers(scope.storeId(), range.previousFrom(), range.previousTo()));
    }

    /** Mọi ngày (hoặc tháng) trong kỳ đều có mặt, kể cả khi không có đơn. */
    private List<SeriesPoint> series(Scope scope, StatisticsPeriod.Range range) {
        boolean monthly = range.granularity() == StatisticsPeriod.Granularity.MONTH;
        Map<LocalDate, Object[]> rows = queries.series(scope, range.from(), range.to(), monthly);
        List<SeriesPoint> points = new ArrayList<>();
        for (LocalDate bucket = range.from(); bucket.isBefore(range.to());
             bucket = monthly ? bucket.plusMonths(1) : bucket.plusDays(1)) {
            Object[] row = rows.get(bucket);
            String label = monthly ? YearMonth.from(bucket).toString() : bucket.toString();
            points.add(row == null ? new SeriesPoint(label, BigDecimal.ZERO, 0)
                    : new SeriesPoint(label, (BigDecimal) row[0], (Long) row[1]));
        }
        return points;
    }

    /** Khách mới của {@value #CUSTOMER_MONTHS} tháng gần nhất tính đến tháng chứa {@code lastDay}. */
    private List<MonthCount> newCustomersByMonth(UUID store, LocalDate lastDay) {
        YearMonth last = YearMonth.from(lastDay);
        YearMonth first = last.minusMonths(CUSTOMER_MONTHS - 1);
        Map<LocalDate, Long> rows = queries.newCustomersByMonth(store, first.atDay(1), last.plusMonths(1).atDay(1));
        List<MonthCount> months = new ArrayList<>(CUSTOMER_MONTHS);
        for (YearMonth month = first; !month.isAfter(last); month = month.plusMonths(1)) {
            months.add(new MonthCount(month.toString(), rows.getOrDefault(month.atDay(1), 0L)));
        }
        return months;
    }

    private static Double rate(long[] retention) {
        return retention[0] == 0 ? null : Math.round(retention[1] * 1000.0 / retention[0]) / 10.0;
    }
}
