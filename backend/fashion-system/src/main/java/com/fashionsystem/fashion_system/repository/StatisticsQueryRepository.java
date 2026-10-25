package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse.Breakdown;
import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse.Inventory;
import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse.TopProduct;
import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse.VipCustomer;
import com.fashionsystem.fashion_system.entity.OrderType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Truy vấn tổng hợp chỉ đọc cho trang thống kê, viết bằng SQL để gom nhóm theo ngày/tháng.
 * DB lưu thời gian theo UTC (hibernate.jdbc.time_zone) còn ứng dụng làm việc theo giờ máy chủ: mốc lọc được đổi
 * sang UTC trước khi so sánh, và cột thời gian được đổi về giờ máy chủ trước khi gom nhóm.
 * Chỉ tính đơn cấp cửa hàng (bỏ đơn cha của đơn tách) và bỏ đơn trả qua cổng chưa thanh toán.
 */
@Repository
@RequiredArgsConstructor
public class StatisticsQueryRepository {
    private static final UUID ANY_STORE = new UUID(0, 0);

    /** Phạm vi lọc: {@code storeId} null là toàn hệ thống, {@code orderType} null là mọi loại đơn. */
    public record Scope(UUID storeId, OrderType orderType) {
    }

    private static final String ORDER_WHERE = """
            o.store_id is not null and (:all or o.store_id = :storeId)
              and (:orderType = '' or o.order_type = :orderType)
              and coalesce(o.payment_status, '') not in ('AWAITING_PAYMENT', 'ABANDONED')
              and o.created_at >= :from and o.created_at < :to
            """;

    /** Giá nhập bình quân gia quyền của mỗi biến thể từ các phiếu nhập đã hoàn thành. */
    private static final String UNIT_COST = """
            left join (select ri.product_variant_id, sum(ri.total) / nullif(sum(ri.quantity), 0) as unit_cost
                       from goods_receipt_items ri join goods_receipts gr on gr.id = ri.receipt_id
                       where gr.status = 'COMPLETED' group by ri.product_variant_id) cost
              on cost.product_variant_id =\s""";

    private final NamedParameterJdbcTemplate jdbc;

    private String localTime(String column) {
        return "(" + column + " at time zone 'UTC' at time zone :zone)";
    }

    private MapSqlParameterSource params(Scope scope, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource()
                .addValue("all", scope.storeId() == null)
                .addValue("storeId", scope.storeId() == null ? ANY_STORE : scope.storeId())
                .addValue("orderType", scope.orderType() == null ? "" : scope.orderType().name())
                .addValue("from", utc(from.atStartOfDay()))
                .addValue("to", utc(to.atStartOfDay()))
                .addValue("zone", ZoneId.systemDefault().getId());
    }

    private static LocalDateTime utc(LocalDateTime local) {
        return local.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Doanh thu (đơn hoàn thành), doanh thu hàng (trừ phí vận chuyển) và số đơn không hủy. */
    public record Totals(BigDecimal revenue, BigDecimal goodsRevenue, long orders) {
    }

    public Totals totals(Scope scope, LocalDate from, LocalDate to) {
        return jdbc.queryForObject("""
                select coalesce(sum(o.total_amount) filter (where o.status = 'COMPLETED'), 0) as revenue,
                       coalesce(sum(o.total_amount - coalesce(o.shipping_fee, 0)) filter (where o.status = 'COMPLETED'), 0)
                           as goods_revenue,
                       count(*) filter (where o.status <> 'CANCELLED') as orders
                from orders o where
                """ + ORDER_WHERE, params(scope, from, to),
                (rs, i) -> new Totals(rs.getBigDecimal("revenue"), rs.getBigDecimal("goods_revenue"), rs.getLong("orders")));
    }

    /** Giá vốn hàng đã bán của đơn hoàn thành và số sản phẩm chưa có giá nhập. */
    public record Cogs(BigDecimal cost, long uncostedQuantity) {
    }

    public Cogs cogs(Scope scope, LocalDate from, LocalDate to) {
        return jdbc.queryForObject("""
                select round(coalesce(sum(i.quantity * cost.unit_cost), 0)) as cost,
                       coalesce(sum(i.quantity) filter (where cost.unit_cost is null), 0) as uncosted
                from order_items i join orders o on o.id = i.order_id
                """ + UNIT_COST + "i.product_variant_id\nwhere o.status = 'COMPLETED' and " + ORDER_WHERE,
                params(scope, from, to), (rs, i) -> new Cogs(rs.getBigDecimal("cost"), rs.getLong("uncosted")));
    }

    /** Doanh thu và số đơn theo ngày ({@code monthly} false) hoặc theo tháng; chỉ trả các mốc có đơn. */
    public Map<LocalDate, Object[]> series(Scope scope, LocalDate from, LocalDate to, boolean monthly) {
        String bucket = monthly ? "date_trunc('month', " + localTime("o.created_at") + ")::date"
                : localTime("o.created_at") + "::date";
        Map<LocalDate, Object[]> points = new LinkedHashMap<>();
        jdbc.query("select " + bucket + """
                 as bucket,
                       coalesce(sum(o.total_amount) filter (where o.status = 'COMPLETED'), 0) as revenue,
                       count(*) filter (where o.status <> 'CANCELLED') as orders
                from orders o where
                """ + ORDER_WHERE + " group by 1", params(scope, from, to), rs -> {
            points.put(rs.getObject("bucket", LocalDate.class),
                    new Object[] {rs.getBigDecimal("revenue"), rs.getLong("orders")});
        });
        return points;
    }

    public Map<String, Long> statusCounts(Scope scope, LocalDate from, LocalDate to) {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query("select o.status, count(*) as total from orders o where " + ORDER_WHERE + " group by o.status",
                params(scope, from, to), rs -> {
                    counts.put(rs.getString("status"), rs.getLong("total"));
                });
        return counts;
    }

    /** Số đơn không hủy và doanh thu đơn hoàn thành theo một cột ({@code order_type} hoặc phương thức thanh toán). */
    public List<Breakdown> breakdown(Scope scope, LocalDate from, LocalDate to, boolean byPaymentMethod) {
        String key = byPaymentMethod ? "coalesce(o.payment_method, 'OTHER')" : "o.order_type";
        return jdbc.query("select " + key + """
                 as key,
                       count(*) filter (where o.status <> 'CANCELLED') as orders,
                       coalesce(sum(o.total_amount) filter (where o.status = 'COMPLETED'), 0) as revenue
                from orders o where
                """ + ORDER_WHERE + " group by 1 order by orders desc, key",
                params(scope, from, to),
                (rs, i) -> new Breakdown(rs.getString("key"), rs.getLong("orders"), rs.getBigDecimal("revenue")));
    }

    public List<TopProduct> topProducts(Scope scope, LocalDate from, LocalDate to, int limit) {
        return jdbc.query("""
                select i.product_id, max(i.product_name) as product_name, sum(i.quantity) as sold, sum(i.total) as revenue,
                       (select coalesce(sum(b.available_quantity + b.online_quantity), 0)
                        from inventory_balances b join product_variants v on v.id = b.product_variant_id
                        where v.product_id = i.product_id and (:all or b.store_id = :storeId)) as stock
                from order_items i join orders o on o.id = i.order_id
                where o.status = 'COMPLETED' and
                """ + ORDER_WHERE + """
                group by i.product_id
                order by sold desc, revenue desc
                limit :limit
                """, params(scope, from, to).addValue("limit", limit),
                (rs, i) -> new TopProduct(rs.getObject("product_id", UUID.class), rs.getString("product_name"),
                        rs.getLong("sold"), rs.getBigDecimal("revenue"), rs.getLong("stock")));
    }

    /** Khách mới (theo cửa hàng nguồn khi xem một cửa hàng) gom theo tháng. */
    public Map<LocalDate, Long> newCustomersByMonth(UUID storeId, LocalDate from, LocalDate to) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        jdbc.query("select date_trunc('month', " + localTime("c.created_at") + """
                )::date as month, count(*) as total
                from customers c
                where c.created_at >= :from and c.created_at < :to and (:all or c.origin_store_id = :storeId)
                group by 1
                """, params(new Scope(storeId, null), from, to), rs -> {
            counts.put(rs.getObject("month", LocalDate.class), rs.getLong("total"));
        });
        return counts;
    }

    public long newCustomers(UUID storeId, LocalDate from, LocalDate to) {
        Long count = jdbc.queryForObject("""
                select count(*) from customers c
                where c.created_at >= :from and c.created_at < :to and (:all or c.origin_store_id = :storeId)
                """, params(new Scope(storeId, null), from, to), Long.class);
        return count == null ? 0 : count;
    }

    /**
     * [khách có đơn không hủy trong kỳ, trong đó số khách đã có từ 2 đơn trở lên tính đến hết kỳ].
     * Đơn tách nhiều cửa hàng tính là một đơn của khách.
     */
    public long[] retention(Scope scope, LocalDate from, LocalDate to) {
        return jdbc.queryForObject("""
                with active as (
                    select distinct o.customer_id from orders o
                    where o.customer_id is not null and o.status <> 'CANCELLED' and
                """ + ORDER_WHERE + """
                )
                select count(*) as active,
                       count(*) filter (where (
                           select count(distinct coalesce(p.parent_order_id, p.id)) from orders p
                           where p.customer_id = a.customer_id and p.store_id is not null and p.status <> 'CANCELLED'
                             and (:all or p.store_id = :storeId) and (:orderType = '' or p.order_type = :orderType)
                             and coalesce(p.payment_status, '') not in ('AWAITING_PAYMENT', 'ABANDONED')
                             and p.created_at < :to) >= 2) as returning
                from active a
                """, params(scope, from, to), (rs, i) -> new long[] {rs.getLong("active"), rs.getLong("returning")});
    }

    /** Khách chi tiêu nhiều nhất trong kỳ (đơn hoàn thành), kèm hạng hiện tại. */
    public List<VipCustomer> vipCustomers(Scope scope, LocalDate from, LocalDate to, int limit) {
        return jdbc.query("""
                select c.id, c.full_name, c.customer_code, t.name as tier,
                       count(distinct coalesce(o.parent_order_id, o.id)) as orders, sum(o.total_amount) as spend
                from orders o
                join customers c on c.id = o.customer_id
                left join customer_tier_assignments a on a.customer_id = c.id and a.expires_at is null
                left join customer_tiers t on t.id = a.tier_id
                where o.status = 'COMPLETED' and
                """ + ORDER_WHERE + """
                group by c.id, c.full_name, c.customer_code, t.name
                order by spend desc, orders desc
                limit :limit
                """, params(scope, from, to).addValue("limit", limit),
                (rs, i) -> new VipCustomer(rs.getObject("id", UUID.class), rs.getString("full_name"),
                        rs.getString("customer_code"), rs.getString("tier"), rs.getLong("orders"),
                        rs.getBigDecimal("spend")));
    }

    public Inventory inventory(UUID storeId, LocalDate from, LocalDate to) {
        MapSqlParameterSource params = params(new Scope(storeId, null), from, to);
        String doneInRange = """
                status = 'COMPLETED' and coalesce(completed_at, updated_at, created_at) >= :from
                  and coalesce(completed_at, updated_at, created_at) < :to and (:all or store_id = :storeId)
                """;
        Long imported = jdbc.queryForObject("select coalesce(sum(total_quantity), 0) from goods_receipts where "
                + doneInRange, params, Long.class);
        Long exported = jdbc.queryForObject("select coalesce(sum(total_quantity), 0) from goods_issues where "
                + doneInRange, params, Long.class);
        return jdbc.queryForObject("""
                select coalesce(sum(b.qty), 0) as quantity,
                       round(coalesce(sum(b.qty * cost.unit_cost), 0)) as value,
                       count(*) filter (where b.qty > 0 and cost.unit_cost is null) as uncosted,
                       count(*) filter (where b.qty > 0 and not exists (
                           select 1 from order_items i join orders o on o.id = i.order_id
                           where i.product_variant_id = b.product_variant_id and o.store_id = b.store_id
                             and o.status = 'COMPLETED' and o.created_at >= :from and o.created_at < :to)) as unsold
                from (select ib.store_id, ib.product_variant_id, ib.available_quantity + ib.online_quantity as qty
                      from inventory_balances ib join stores s on s.id = ib.store_id
                      where s.active = true and (:all or ib.store_id = :storeId)) b
                """ + UNIT_COST + "b.product_variant_id", params,
                (rs, i) -> new Inventory(imported == null ? 0 : imported, exported == null ? 0 : exported,
                        rs.getLong("quantity"), rs.getBigDecimal("value"), rs.getLong("uncosted"), rs.getLong("unsold")));
    }
}
