package com.fashionsystem.fashion_system.marketing;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.helper.excel.ExcelAlignment;
import com.fashionsystem.fashion_system.helper.excel.ExcelCellFormat;
import com.fashionsystem.fashion_system.helper.excel.ExcelColumn;
import com.fashionsystem.fashion_system.helper.excel.ExcelHelper;
import com.fashionsystem.fashion_system.helper.excel.ExcelSheet;
import com.fashionsystem.fashion_system.helper.excel.ExcelWorkbookData;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Campaign;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Offer;
import com.fashionsystem.fashion_system.marketing.MarketingReportDtos.CalendarEvent;
import com.fashionsystem.fashion_system.marketing.MarketingReportDtos.CompareRow;
import com.fashionsystem.fashion_system.marketing.MarketingReportDtos.DayRow;
import com.fashionsystem.fashion_system.marketing.MarketingReportDtos.Kpi;
import com.fashionsystem.fashion_system.marketing.MarketingReportDtos.OfferRow;
import com.fashionsystem.fashion_system.marketing.MarketingReportDtos.Overlap;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo cáo hiệu quả, so sánh và lịch chiến dịch — chỉ đọc, tính trực tiếp từ dữ liệu đơn hàng có sẵn, không lưu sẵn.
 * <p>
 * Đơn được tính cho chiến dịch C (xem {@link #attributed}): đơn gốc (đơn tách theo cửa hàng quy về đơn cha) có dùng ít nhất
 * một ưu đãi đã liên kết — khuyến mãi / mẫu voucher qua {@code order_promotions}, flash sale qua {@code flash_sale_usages}
 * (cả hai đều ghi vào đơn gốc) — tạo từ lúc chiến dịch bắt đầu, không bị hủy / trả về / bỏ dở thanh toán. Mỗi đơn chỉ tính
 * một lần cho một chiến dịch; một đơn dùng ưu đãi của hai chiến dịch có mặt ở cả hai báo cáo, còn tổng khi so sánh tính
 * trên tập đơn khác nhau.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketingReportService {
    static final int MAX_COMPARE = 5;
    static final int MAX_CALENDAR_DAYS = 93;
    private static final int MAX_CONFLICTS = 20;
    private static final String REVENUE = "(o.total_amount - coalesce(o.shipping_fee, 0))";

    private final NamedParameterJdbcTemplate jdbc;
    private final MarketingCampaignAccess access;
    private final MarketingCampaignService campaigns;
    private final ExcelHelper excel;

    @Value("${marketing-dispatch.frequency-cap-hours:24}")
    private int frequencyCapHours = 24;

    /**
     * Id các đơn gốc được tính cho chiến dịch có id ở tham số {@code :param}. Chiến dịch của một cửa hàng chỉ tính đơn có
     * phần thuộc cửa hàng đó (đơn tại quầy của cửa hàng, hoặc đơn online có phần giao từ cửa hàng).
     */
    static String attributed(String param) {
        String p = ":" + param;
        return "select o.id from orders o join marketing_campaigns mc on mc.id = " + p
                + " where o.parent_order_id is null and o.status not in ('CANCELLED', 'RETURNED')"
                + " and coalesce(o.payment_status, '') <> 'ABANDONED' and o.created_at >= mc.starts_at"
                + " and (mc.store_id is null or exists (select 1 from orders sx where coalesce(sx.parent_order_id, sx.id) = o.id"
                + " and sx.store_id = mc.store_id))"
                + " and o.id in (select coalesce(x.parent_order_id, x.id) from orders x where x.id in ("
                + " select op.order_id from order_promotions op join marketing_campaign_offers mo on mo.promotion_id = op.promotion_id"
                + " where mo.campaign_id = " + p
                + " union select op.order_id from order_promotions op join voucher_campaigns vc on vc.promotion_id = op.promotion_id"
                + " join marketing_campaign_offers mo on mo.voucher_campaign_id = vc.id where mo.campaign_id = " + p
                + " union select u.order_id from flash_sale_usages u join flash_sale_items i on i.id = u.flash_sale_item_id"
                + " join marketing_campaign_offers mo on mo.flash_sale_id = i.flash_sale_id where mo.campaign_id = " + p + "))";
    }

    private static String sql(String template) {
        return template.replace("{ATTR}", attributed("c"));
    }

    // ------------------------------------------------------------------ báo cáo một chiến dịch

    /** Nhân viên cấp chuỗi xem mọi chiến dịch; quản lý cửa hàng xem chiến dịch của cửa hàng mình. */
    public MarketingReportDtos.Performance performance(UUID actor, UUID id) {
        Campaign c = campaigns.campaignForReport(id);
        access.requireFinancials(actor, c.storeId());
        MapSqlParameterSource p = new MapSqlParameterSource("c", id);
        Kpi kpi = kpi(c, p);
        List<DayRow> daily = jdbc.query(sql("""
                select (o.created_at at time zone 'UTC' at time zone 'Asia/Ho_Chi_Minh')::date as day, count(*) as orders,
                       sum(o.total_amount - coalesce(o.shipping_fee, 0)) as revenue
                from orders o where o.id in ({ATTR}) group by 1 order by 1"""), p,
                (rs, i) -> new DayRow(rs.getObject("day", LocalDate.class), rs.getLong("orders"), money(rs.getBigDecimal("revenue"))));
        List<MarketingReportDtos.Experiment> experiments = experiments(c);
        return new MarketingReportDtos.Performance(c.id(), c.code(), c.name(), c.status(), c.startsAt(), c.endsAt(), kpi, daily,
                offerRows(p), experiments, notes(p, kpi, c, experiments), LocalDateTime.now(), c.storeId(), c.storeName());
    }

    // ------------------------------------------------------------------ nhóm đối chứng

    private static final double Z_95 = 1.96;
    private static final int MIN_GROUP = 30;

    /**
     * Kết quả các đợt gửi có nhóm đối chứng. Mua = có đơn gốc hợp lệ bất kỳ (không chỉ đơn dùng ưu đãi, vì nhóm đối chứng
     * không biết tới ưu đãi) trong [lúc chốt danh sách, min(kết thúc chiến dịch, hiện tại)); chiến dịch cửa hàng chỉ tính đơn
     * có phần thuộc cửa hàng đó.
     */
    private List<MarketingReportDtos.Experiment> experiments(Campaign c) {
        List<Map<String, Object>> sends = jdbc.queryForList("""
                select id, code, channel, holdout_percent, snapshot_at from marketing_dispatches
                where campaign_id = :c and holdout_percent > 0 and snapshot_at is not null order by snapshot_at""",
                new MapSqlParameterSource("c", c.id()));
        List<MarketingReportDtos.Experiment> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = c.endsAt().isBefore(now) ? c.endsAt() : now;
        for (Map<String, Object> d : sends) {
            LocalDateTime from = DbTimes.fromDb(d.get("snapshot_at"));
            MapSqlParameterSource p = new MapSqlParameterSource("d", d.get("id")).addValue("from", DbTimes.toDb(from))
                    .addValue("until", DbTimes.toDb(until)).addValue("store", c.storeId(), java.sql.Types.OTHER);
            Map<String, long[]> counts = new java.util.HashMap<>();
            Map<String, BigDecimal> revenue = new java.util.HashMap<>();
            jdbc.query("""
                    with ord as (
                        select o.customer_id, count(*) as n, sum(o.total_amount - coalesce(o.shipping_fee, 0)) as rev
                        from orders o
                        where o.parent_order_id is null and o.status not in ('CANCELLED', 'RETURNED')
                          and coalesce(o.payment_status, '') <> 'ABANDONED' and o.created_at >= :from and o.created_at < :until
                          and (cast(:store as uuid) is null or exists (select 1 from orders sx
                               where coalesce(sx.parent_order_id, sx.id) = o.id and sx.store_id = cast(:store as uuid)))
                        group by o.customer_id)
                    select r.status, count(*) as people, count(ord.customer_id) as buyers, coalesce(sum(ord.n), 0) as orders,
                           coalesce(sum(ord.rev), 0) as revenue
                    from marketing_dispatch_recipients r left join ord on ord.customer_id = r.customer_id
                    where r.dispatch_id = :d and r.status in ('SENT', 'HOLDOUT') group by r.status""", p,
                    (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                        counts.put(rs.getString("status"), new long[] {rs.getLong("people"), rs.getLong("buyers"), rs.getLong("orders")});
                        revenue.put(rs.getString("status"), rs.getBigDecimal("revenue"));
                    });
            long[] t = counts.getOrDefault("SENT", new long[3]);
            long[] h = counts.getOrDefault("HOLDOUT", new long[3]);
            results.add(experiment((UUID) d.get("id"), (String) d.get("code"), (String) d.get("channel"),
                    ((Number) d.get("holdout_percent")).intValue(), t, h, revenue.getOrDefault("SENT", BigDecimal.ZERO),
                    revenue.getOrDefault("HOLDOUT", BigDecimal.ZERO), from, until));
        }
        return results;
    }

    /** Tỷ lệ, chênh lệch, kiểm định z hai tỷ lệ và ước tính phần tăng thêm (t, h = {người, người mua, số đơn}). */
    static MarketingReportDtos.Experiment experiment(UUID id, String code, String channel, int percent, long[] t, long[] h,
                                                     BigDecimal treatedRevenue, BigDecimal holdoutRevenue,
                                                     LocalDateTime from, LocalDateTime until) {
        long nT = t[0];
        long nH = h[0];
        Double rateT = nT == 0 ? null : (double) t[1] / nT;
        Double rateH = nH == 0 ? null : (double) h[1] / nH;
        Double uplift = rateT == null || rateH == null ? null : round((rateT - rateH) * 100, 2);
        Double z = null;
        if (rateT != null && rateH != null) {
            double pooled = (double) (t[1] + h[1]) / (nT + nH);
            double se = Math.sqrt(pooled * (1 - pooled) * (1.0 / nT + 1.0 / nH));
            z = se == 0 ? null : round((rateT - rateH) / se, 2);
        }
        boolean significant = z != null && Math.abs(z) >= Z_95 && nT >= MIN_GROUP && nH >= MIN_GROUP;
        BigDecimal orders = null;
        BigDecimal revenue = null;
        if (nT > 0 && nH > 0) {
            double perPersonOrders = (double) t[2] / nT - (double) h[2] / nH;
            orders = BigDecimal.valueOf(perPersonOrders * nT).setScale(1, RoundingMode.HALF_UP);
            BigDecimal perT = treatedRevenue.divide(BigDecimal.valueOf(nT), 6, RoundingMode.HALF_UP);
            BigDecimal perH = holdoutRevenue.divide(BigDecimal.valueOf(nH), 6, RoundingMode.HALF_UP);
            revenue = perT.subtract(perH).multiply(BigDecimal.valueOf(nT)).setScale(0, RoundingMode.HALF_UP);
        }
        return new MarketingReportDtos.Experiment(id, code, channel, percent, nT, t[1], nH, h[1],
                rateT == null ? null : round(rateT * 100, 2), rateH == null ? null : round(rateH * 100, 2), uplift, z, significant,
                orders, revenue, from, until);
    }

    private static double round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    private Kpi kpi(Campaign c, MapSqlParameterSource p) {
        Map<String, Object> sales = jdbc.queryForMap(sql("""
                select count(*) as orders,
                       coalesce(sum(o.total_amount - coalesce(o.shipping_fee, 0)), 0) as revenue,
                       coalesce(sum(o.total_amount - coalesce(o.shipping_fee, 0)) filter (where o.status = 'COMPLETED'), 0) as completed,
                       count(distinct o.customer_id) as buyers,
                       count(*) filter (where o.order_type = 'ONLINE') as online,
                       count(*) filter (where o.order_type = 'OFFLINE') as offline
                from orders o where o.id in ({ATTR})"""), p);
        // Yêu cầu trả hàng có thể trỏ vào đơn con: quy về đơn gốc như ReturnRequestService.
        BigDecimal refunds = decimal(jdbc.queryForObject(sql("""
                select coalesce(sum(rr.refund_amount), 0) from return_requests rr join orders x on x.id = rr.order_id
                where rr.status = 'COMPLETED' and coalesce(x.parent_order_id, x.id) in ({ATTR})"""), p, BigDecimal.class));
        long firstTime = number(jdbc.queryForObject(sql("""
                select count(*) from (
                    select o.customer_id, min(o.created_at) as first_at from orders o
                    where o.parent_order_id is null and o.status not in ('CANCELLED', 'RETURNED')
                      and coalesce(o.payment_status, '') <> 'ABANDONED'
                      and o.customer_id in (select a.customer_id from orders a where a.id in ({ATTR}))
                    group by o.customer_id) f
                where exists (select 1 from orders b where b.id in ({ATTR}) and b.customer_id = f.customer_id
                              and b.created_at = f.first_at)"""), p, Long.class));
        BigDecimal discount = decimal(jdbc.queryForObject(sql("""
                select coalesce(sum(op.discount_amount), 0) from order_promotions op
                where op.order_id in ({ATTR})
                  and (op.promotion_id in (select mo.promotion_id from marketing_campaign_offers mo
                                           where mo.campaign_id = :c and mo.promotion_id is not null)
                       or op.promotion_id in (select vc.promotion_id from voucher_campaigns vc
                                              join marketing_campaign_offers mo on mo.voucher_campaign_id = vc.id
                                              where mo.campaign_id = :c))"""), p, BigDecimal.class));
        BigDecimal flash = decimal(jdbc.queryForObject(sql("""
                select coalesce(sum(u.quantity * greatest(coalesce((
                           select max(oi.price) from order_items oi join orders x on x.id = oi.order_id
                           where coalesce(x.parent_order_id, x.id) = u.order_id and oi.product_variant_id = i.product_variant_id),
                           i.sale_price) - i.sale_price, 0)), 0)
                from flash_sale_usages u join flash_sale_items i on i.id = u.flash_sale_item_id
                join marketing_campaign_offers mo on mo.flash_sale_id = i.flash_sale_id
                where mo.campaign_id = :c and u.order_id in ({ATTR})"""), p, BigDecimal.class));
        BigDecimal expenses = decimal(jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from marketing_campaign_expenses where campaign_id = :c", p, BigDecimal.class));
        Map<String, Object> vouchers = jdbc.queryForMap("""
                select count(*) as issued, count(cp.used_order_id) as used from customer_promotions cp
                join marketing_campaign_offers mo on mo.voucher_campaign_id = cp.campaign_id
                where mo.campaign_id = :c and cp.source = 'CAMPAIGN'""", p);
        Map<String, Object> reach = jdbc.queryForMap(sql("""
                select count(*) filter (where r.status = 'SENT') as sent,
                       count(*) filter (where r.status = 'SENT' and exists (
                           select 1 from notifications n where n.event_id = r.event_id and n.recipient_type = 'CUSTOMER'
                             and n.recipient_id = r.customer_id and n.read_at is not null)) as read_count,
                       count(distinct r.customer_id) filter (where r.status = 'SENT' and exists (
                           select 1 from orders o where o.id in ({ATTR}) and o.customer_id = r.customer_id
                             and o.created_at >= r.sent_at)) as buyers_after
                from marketing_dispatch_recipients r join marketing_dispatches d on d.id = r.dispatch_id
                where d.campaign_id = :c"""), p);

        long orders = number(sales.get("orders"));
        BigDecimal revenue = money(decimal(sales.get("revenue")));
        BigDecimal completed = money(decimal(sales.get("completed")));
        BigDecimal net = money(completed.subtract(refunds).max(BigDecimal.ZERO));
        BigDecimal totalCost = money(discount.add(flash).add(expenses));
        BigDecimal aov = orders == 0 ? null : revenue.divide(BigDecimal.valueOf(orders), 0, RoundingMode.HALF_UP);
        Integer usage = c.plannedBudget() == null || c.plannedBudget().signum() == 0 ? null
                : totalCost.multiply(BigDecimal.valueOf(100)).divide(c.plannedBudget(), 0, RoundingMode.HALF_UP).intValue();
        BigDecimal roas = totalCost.signum() == 0 ? null : net.divide(totalCost, 2, RoundingMode.HALF_UP);
        // Giá vốn của đơn đã hoàn tất: giá nhập bình quân gia quyền từ phiếu nhập đã hoàn thành (cùng định nghĩa với
        // trang Thống kê); món chưa có giá nhập được đếm riêng để báo độ phủ.
        Map<String, Object> cost = jdbc.queryForMap(sql("""
                select round(coalesce(sum(i.quantity * unit.unit_cost), 0)) as cogs,
                       coalesce(sum(i.quantity) filter (where unit.unit_cost is null), 0) as uncosted
                from order_items i join orders x on x.id = i.order_id
                left join (select ri.product_variant_id, sum(ri.total) / nullif(sum(ri.quantity), 0) as unit_cost
                           from goods_receipt_items ri join goods_receipts gr on gr.id = ri.receipt_id
                           where gr.status = 'COMPLETED' group by ri.product_variant_id) unit
                  on unit.product_variant_id = i.product_variant_id
                where coalesce(x.parent_order_id, x.id) in (select o.id from orders o where o.id in ({ATTR}) and o.status = 'COMPLETED')"""),
                p);
        BigDecimal cogs = money(decimal(cost.get("cogs")));
        BigDecimal grossProfit = net.subtract(cogs);
        BigDecimal roi = expenses.signum() == 0 ? null
                : grossProfit.subtract(expenses).divide(expenses, 2, RoundingMode.HALF_UP);
        return new Kpi(orders, revenue, completed, money(refunds), net, number(sales.get("buyers")), firstTime, aov,
                number(sales.get("online")), number(sales.get("offline")), money(discount), money(flash), money(expenses), totalCost,
                usage, roas, number(vouchers.get("issued")), number(vouchers.get("used")), number(reach.get("sent")),
                number(reach.get("read_count")), number(reach.get("buyers_after")), c.plannedBudget(), c.targetRevenue(),
                c.targetOrders(), c.targetBuyers(), cogs, number(cost.get("uncosted")), money(grossProfit), roi);
    }

    /** Đóng góp của từng ưu đãi: số đơn (trong tập đơn của chiến dịch) và tiền giảm. */
    private List<OfferRow> offerRows(MapSqlParameterSource p) {
        List<OfferRow> rows = new ArrayList<>();
        rows.addAll(jdbc.query(sql("""
                select p.code, p.name,
                       count(distinct op.order_id) filter (where op.order_id in ({ATTR})) as orders,
                       coalesce(sum(op.discount_amount) filter (where op.order_id in ({ATTR})), 0) as discount
                from marketing_campaign_offers mo join promotions p on p.id = mo.promotion_id
                left join order_promotions op on op.promotion_id = p.id
                where mo.campaign_id = :c group by mo.id, p.code, p.name order by 3 desc, p.code"""), p,
                (rs, i) -> new OfferRow("PROMOTION", rs.getString("code"), rs.getString("name"), rs.getLong("orders"),
                        money(rs.getBigDecimal("discount")))));
        rows.addAll(jdbc.query(sql("""
                select vc.code, vc.name,
                       count(distinct op.order_id) filter (where op.order_id in ({ATTR})) as orders,
                       coalesce(sum(op.discount_amount) filter (where op.order_id in ({ATTR})), 0) as discount
                from marketing_campaign_offers mo join voucher_campaigns vc on vc.id = mo.voucher_campaign_id
                left join order_promotions op on op.promotion_id = vc.promotion_id
                where mo.campaign_id = :c group by mo.id, vc.code, vc.name order by 3 desc, vc.code"""), p,
                (rs, i) -> new OfferRow("VOUCHER_CAMPAIGN", rs.getString("code"), rs.getString("name"), rs.getLong("orders"),
                        money(rs.getBigDecimal("discount")))));
        rows.addAll(jdbc.query(sql("""
                select fs.name,
                       count(distinct u.order_id) filter (where u.order_id in ({ATTR})) as orders,
                       coalesce(sum(u.quantity * greatest(coalesce((
                           select max(oi.price) from order_items oi join orders x on x.id = oi.order_id
                           where coalesce(x.parent_order_id, x.id) = u.order_id and oi.product_variant_id = i.product_variant_id),
                           i.sale_price) - i.sale_price, 0)) filter (where u.order_id in ({ATTR})), 0) as discount
                from marketing_campaign_offers mo join flash_sales fs on fs.id = mo.flash_sale_id
                left join flash_sale_items i on i.flash_sale_id = fs.id
                left join flash_sale_usages u on u.flash_sale_item_id = i.id
                where mo.campaign_id = :c group by mo.id, fs.name order by 2 desc, fs.name"""), p,
                (rs, i) -> new OfferRow("FLASH_SALE", null, rs.getString("name"), rs.getLong("orders"), money(rs.getBigDecimal("discount")))));
        return rows;
    }

    /** Giới hạn của dữ liệu, hiển thị cùng báo cáo để người xem không hiểu sai con số. */
    private List<String> notes(MapSqlParameterSource p, Kpi kpi, Campaign c, List<MarketingReportDtos.Experiment> experiments) {
        List<String> notes = new ArrayList<>(List.of(
                "Doanh thu ở đây là của các đơn có dùng ưu đãi của chiến dịch, KHÔNG phải doanh thu tăng thêm nhờ chiến dịch: "
                        + "khách có thể đã mua dù không có ưu đãi. Chỉ phần \"Nhóm đối chứng\" mới ước tính được mức tăng thêm.",
                "Tính đơn từ lúc chiến dịch bắt đầu; đơn tách theo cửa hàng tính theo đơn gốc; bỏ đơn đã hủy, trả về hoặc bỏ dở thanh toán.",
                "ROAS = doanh thu thuần / tổng chi phí, chưa trừ giá vốn. ROI = (doanh thu thuần − giá vốn − chi phí thực tế) / chi phí "
                        + "thực tế; doanh thu đã là sau giảm giá nên không trừ giảm giá lần nữa. Giá vốn = giá nhập bình quân gia quyền "
                        + "từ phiếu nhập đã hoàn thành (cùng cách tính với trang Thống kê); hàng trả lại vẫn tính giá vốn.",
                "Hoàn hàng một phần không hoàn lại tiền giảm giá đã ghi, nên chi phí giảm giá có thể cao hơn thực tế một chút."));
        Boolean hasFlash = jdbc.queryForObject("""
                select exists (select 1 from marketing_campaign_offers where campaign_id = :c and flash_sale_id is not null)""", p, Boolean.class);
        if (Boolean.TRUE.equals(hasFlash)) {
            notes.add("Chi phí flash sale là ước tính: (giá bán thường trên đơn − giá flash hiện tại) × số món, vì hệ thống không lưu "
                    + "mức giảm của từng lượt mua flash.");
            Boolean edited = jdbc.queryForObject("""
                    select exists (select 1 from marketing_campaign_offers mo join flash_sales fs on fs.id = mo.flash_sale_id
                                   join flash_sale_items i on i.flash_sale_id = fs.id
                                   join flash_sale_usages u on u.flash_sale_item_id = i.id
                                   where mo.campaign_id = :c and fs.updated_at is not null and u.created_at < fs.updated_at)""",
                    p, Boolean.class);
            if (Boolean.TRUE.equals(edited)) notes.add("Flash sale đã được sửa sau khi có đơn nên chi phí flash ước tính có thể lệch.");
        }
        if (kpi.uncostedQuantity() > 0) {
            notes.add(kpi.uncostedQuantity() + " món chưa có giá nhập nên không có trong giá vốn; lợi nhuận gộp và ROI có thể cao hơn thực tế.");
        }
        if (c.storeId() != null) {
            notes.add("Chiến dịch của cửa hàng " + c.storeName() + ": chỉ tính đơn có phần thuộc cửa hàng này; đơn online tách nhiều "
                    + "cửa hàng được tính cả đơn.");
        }
        if (!experiments.isEmpty()) {
            notes.add("Nhóm đối chứng: khách đủ điều kiện được chia ngẫu nhiên, một phần không nhận tin. Mức tăng thêm = chênh lệch "
                    + "giữa người nhận và nhóm đối chứng (đơn bất kỳ trong thời gian đo). Chỉ kết luận khi kết quả có ý nghĩa thống kê "
                    + "(mức tin cậy 95%, mỗi nhóm ít nhất " + MIN_GROUP + " người).");
        }
        if (kpi.notificationsSent() > 0) {
            notes.add("\"Đã đọc\" chỉ cho biết khách đã mở hoặc đánh dấu đã đọc thông báo, không phải đã bấm vào liên kết. "
                    + "\"Khách mua sau khi nhận tin\" là tương quan, không chứng minh thông báo khiến khách mua.");
        }
        return notes;
    }

    // ------------------------------------------------------------------ so sánh

    public MarketingReportDtos.Comparison compare(UUID actor, List<UUID> ids) {
        requireGlobal(actor);
        List<UUID> unique = new ArrayList<>(new LinkedHashSet<>(ids == null ? List.of() : ids));
        if (unique.size() < 2 || unique.size() > MAX_COMPARE)
            throw BusinessException.badRequest("Chọn từ 2 tới " + MAX_COMPARE + " chiến dịch để so sánh");
        List<CompareRow> rows = new ArrayList<>();
        MapSqlParameterSource all = new MapSqlParameterSource();
        List<String> sets = new ArrayList<>();
        for (int i = 0; i < unique.size(); i++) {
            UUID id = unique.get(i);
            Campaign c = campaigns.campaignForReport(id);
            Kpi k = kpi(c, new MapSqlParameterSource("c", id));
            rows.add(new CompareRow(c.id(), c.code(), c.name(), c.status(), c.startsAt(), c.endsAt(), k.orders(), k.revenue(),
                    k.netRevenue(), k.totalCost(), k.roas(), k.buyers()));
            all.addValue("c" + i, id);
            sets.add(attributed("c" + i));
        }
        Map<String, Object> union = jdbc.queryForMap("select count(*) as orders, coalesce(sum(" + REVENUE + "), 0) as revenue"
                + " from orders o where o.id in (" + String.join(" union ", sets) + ")", all);
        List<Overlap> overlaps = new ArrayList<>();
        for (int i = 0; i < unique.size(); i++) {
            for (int j = i + 1; j < unique.size(); j++) {
                Long shared = jdbc.queryForObject("select count(*) from (" + sets.get(i) + ") a where a.id in (" + sets.get(j) + ")",
                        all, Long.class);
                if (shared != null && shared > 0) overlaps.add(new Overlap(unique.get(i), unique.get(j), shared));
            }
        }
        List<String> notes = new ArrayList<>(List.of(
                "Mỗi cột là doanh thu của đơn có dùng ưu đãi của chiến dịch đó, không phải doanh thu tăng thêm nhờ chiến dịch.",
                "Một đơn dùng ưu đãi của hai chiến dịch có mặt ở cả hai cột; dòng tổng tính mỗi đơn một lần nên nhỏ hơn tổng các cột."));
        return new MarketingReportDtos.Comparison(rows, number(union.get("orders")), money(decimal(union.get("revenue"))), overlaps, notes);
    }

    // ------------------------------------------------------------------ lịch

    /**
     * Ba loại mốc thời gian tách riêng: thời gian chiến dịch, thời gian hiệu lực ưu đãi và giờ gửi thông báo. Nhân viên cửa
     * hàng chỉ thấy chiến dịch đã duyệt và ưu đãi của chúng; cảnh báo chồng lấn chỉ dành cho nhân viên cấp chuỗi.
     */
    public MarketingReportDtos.Calendar calendar(UUID actor, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) throw BusinessException.badRequest("Khoảng ngày không hợp lệ");
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_CALENDAR_DAYS)
            throw BusinessException.badRequest("Lịch xem tối đa " + MAX_CALENDAR_DAYS + " ngày mỗi lần");
        boolean global = access.isGlobal(actor);
        UUID ownStore = access.ownStore(actor);
        boolean manageOwn = ownStore != null && access.canManageCampaign(actor, ownStore);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        MapSqlParameterSource p = new MapSqlParameterSource("from", DbTimes.toDb(start)).addValue("to", DbTimes.toDb(end))
                .addValue("ownStore", ownStore, java.sql.Types.OTHER);
        // Nhân viên cửa hàng: chiến dịch đã duyệt của toàn chuỗi / cửa hàng mình; người quản lý cửa hàng thấy thêm chiến dịch
        // đang lập của cửa hàng mình.
        String visible = global ? "status not in ('CANCELLED', 'REJECTED')"
                : "((status = 'APPROVED' and (store_id is null or store_id = cast(:ownStore as uuid)))"
                        + (manageOwn ? " or (store_id = cast(:ownStore as uuid) and status not in ('CANCELLED', 'REJECTED')))" : ")");
        List<UUID> ids = jdbc.queryForList("select id from marketing_campaigns where starts_at < :to and ends_at > :from and "
                + visible + " order by starts_at", p, UUID.class);
        List<CalendarEvent> events = new ArrayList<>();
        List<CalendarEvent> offerEvents = new ArrayList<>();
        for (UUID id : ids) {
            Campaign c = campaigns.campaignForReport(id);
            events.add(new CalendarEvent("CAMPAIGN", c.id(), c.code(), c.name(), c.status(), c.name(), c.objective(),
                    c.startsAt(), c.endsAt(), c.phase() != null ? c.phase() : c.status()));
            for (Offer o : campaigns.linkedOffers(c)) {
                LocalDateTime validFrom = o.validFrom() != null ? o.validFrom() : c.startsAt();
                LocalDateTime validTo = o.validTo() != null ? o.validTo() : c.endsAt();
                if (!validFrom.isBefore(end) || !validTo.isAfter(start)) continue;
                CalendarEvent event = new CalendarEvent("OFFER", c.id(), c.code(), c.name(), c.status(),
                        o.code() == null ? o.name() : o.code() + " – " + o.name(), o.detail(), validFrom, validTo, o.itemStatus());
                events.add(event);
                offerEvents.add(event);
            }
        }
        List<CalendarEvent> sends = List.of();
        if (global) {
            sends = jdbc.query("""
                    select d.code, d.title, d.scheduled_at, d.status, c.id as campaign_id, c.code as campaign_code,
                           c.name as campaign_name, c.status as campaign_status
                    from marketing_dispatches d join marketing_campaigns c on c.id = d.campaign_id
                    where d.status <> 'CANCELLED' and c.status not in ('CANCELLED', 'REJECTED')
                      and d.scheduled_at >= :from and d.scheduled_at < :to order by d.scheduled_at""", p, (rs, i) -> {
                LocalDateTime at = DbTimes.fromDb(rs.getObject("scheduled_at"));
                return new CalendarEvent("DISPATCH", rs.getObject("campaign_id", UUID.class), rs.getString("campaign_code"),
                        rs.getString("campaign_name"), rs.getString("campaign_status"), rs.getString("code") + " · " + rs.getString("title"),
                        "Thông báo trong app", at, at, rs.getString("status"));
            });
            events.addAll(sends);
        }
        events.sort(Comparator.comparing(CalendarEvent::startsAt).thenComparing(CalendarEvent::kind));
        List<String> conflicts = global ? conflicts(offerEvents, sends) : List.of();
        return new MarketingReportDtos.Calendar(from, to, events, conflicts);
    }

    /**
     * Cảnh báo (không chặn): ưu đãi của hai chiến dịch khác nhau cùng hiệu lực — khách chỉ dùng được một voucher toàn đơn mỗi
     * đơn nên các chiến dịch có thể "giành" đơn của nhau; hai đợt gửi cách nhau dưới khoảng giới hạn tần suất — khách nhận
     * đợt trước sẽ bị bỏ qua ở đợt sau.
     */
    private List<String> conflicts(List<CalendarEvent> offers, List<CalendarEvent> sends) {
        List<String> conflicts = new ArrayList<>();
        for (int i = 0; i < offers.size() && conflicts.size() < MAX_CONFLICTS; i++) {
            for (int j = i + 1; j < offers.size() && conflicts.size() < MAX_CONFLICTS; j++) {
                CalendarEvent a = offers.get(i);
                CalendarEvent b = offers.get(j);
                if (a.campaignId().equals(b.campaignId()) || !live(a.campaignStatus()) || !live(b.campaignStatus())) continue;
                if (a.startsAt().isBefore(b.endsAt()) && b.startsAt().isBefore(a.endsAt())) {
                    conflicts.add("Ưu đãi " + a.label() + " (" + a.campaignCode() + ") và " + b.label() + " (" + b.campaignCode()
                            + ") cùng hiệu lực; các chiến dịch có thể giành đơn của nhau và báo cáo sẽ có đơn dùng chung.");
                }
            }
        }
        for (int i = 0; i + 1 < sends.size() && conflicts.size() < MAX_CONFLICTS; i++) {
            CalendarEvent a = sends.get(i);
            CalendarEvent b = sends.get(i + 1);
            if (Duration.between(a.startsAt(), b.startsAt()).toHours() < frequencyCapHours) {
                conflicts.add("Đợt " + a.label() + " (" + a.campaignCode() + ") và " + b.label() + " (" + b.campaignCode()
                        + ") cách nhau dưới " + frequencyCapHours + " giờ: khách đã nhận đợt trước sẽ bị bỏ qua ở đợt sau.");
            }
        }
        return conflicts;
    }

    private static boolean live(String status) {
        return MarketingCampaignService.APPROVED.equals(status) || MarketingCampaignService.PENDING.equals(status);
    }

    // ------------------------------------------------------------------ xuất Excel

    record Line(String label, BigDecimal value, String unit) {}

    public byte[] exportPerformance(UUID actor, UUID id) {
        MarketingReportDtos.Performance r = performance(actor, id);
        Kpi k = r.kpi();
        List<Line> lines = new ArrayList<>(List.of(
                line("Số đơn có dùng ưu đãi", k.orders(), "đơn"), line("Chỉ tiêu số đơn", k.targetOrders(), "đơn"),
                new Line("Doanh thu (gồm đơn đang xử lý)", k.revenue(), "đ"), new Line("Doanh thu đã hoàn tất", k.completedRevenue(), "đ"),
                new Line("Tiền hoàn trả hàng", k.refunds(), "đ"), new Line("Doanh thu thuần", k.netRevenue(), "đ"),
                new Line("Chỉ tiêu doanh thu", k.targetRevenue(), "đ"), line("Khách mua", k.buyers(), "khách"),
                line("Khách mua lần đầu", k.firstTimeBuyers(), "khách"), line("Chỉ tiêu số khách", k.targetBuyers(), "khách"),
                new Line("Giá trị đơn trung bình", k.averageOrderValue(), "đ"), line("Đơn online", k.onlineOrders(), "đơn"),
                line("Đơn tại quầy", k.offlineOrders(), "đơn"), new Line("Chi phí giảm giá (khuyến mãi / voucher)", k.discountCost(), "đ"),
                new Line("Chi phí giảm giá flash sale (ước tính)", k.flashDiscountCost(), "đ"), new Line("Chi phí thực tế", k.expenseCost(), "đ"),
                new Line("Tổng chi phí", k.totalCost(), "đ"), new Line("Ngân sách dự kiến", k.plannedBudget(), "đ"),
                line("Sử dụng ngân sách", k.budgetUsagePercent(), "%"), new Line("ROAS", k.roas(), "lần"),
                line("Voucher đã phát", k.vouchersIssued(), "voucher"), line("Voucher đã dùng", k.vouchersUsed(), "voucher"),
                line("Thông báo đã gửi", k.notificationsSent(), "khách"), line("Thông báo đã đọc", k.notificationsRead(), "khách"),
                line("Người nhận có mua sau khi nhận tin", k.buyersAfterNotification(), "khách"),
                new Line("Giá vốn ước tính (đơn đã hoàn tất)", k.cogs(), "đ"), line("Số món chưa có giá nhập", k.uncostedQuantity(), "món"),
                new Line("Lợi nhuận gộp ước tính", k.grossProfit(), "đ"), new Line("ROI theo lợi nhuận gộp", k.roi(), "lần")));
        List<ExcelColumn<Line>> overview = List.of(new ExcelColumn<>("Chỉ số", Line::label),
                new ExcelColumn<>("Giá trị", Line::value, ExcelCellFormat.NUMBER, "#,##0.##", ExcelAlignment.RIGHT),
                new ExcelColumn<>("Đơn vị", Line::unit));
        List<ExcelColumn<DayRow>> days = List.of(new ExcelColumn<>("Ngày", DayRow::date, ExcelCellFormat.DATE, "dd/MM/yyyy", ExcelAlignment.LEFT),
                number("Số đơn", DayRow::orders), money("Doanh thu", DayRow::revenue));
        List<ExcelColumn<OfferRow>> offers = List.of(new ExcelColumn<>("Loại", OfferRow::type), new ExcelColumn<>("Mã", OfferRow::code),
                new ExcelColumn<>("Tên", OfferRow::name), number("Số đơn", OfferRow::orders), money("Tiền giảm", OfferRow::discount));
        List<ExcelColumn<MarketingReportDtos.Experiment>> tests = List.of(new ExcelColumn<>("Đợt", MarketingReportDtos.Experiment::code),
                new ExcelColumn<>("Kênh", MarketingReportDtos.Experiment::channel), number("% đối chứng", MarketingReportDtos.Experiment::holdoutPercent),
                number("Người nhận", MarketingReportDtos.Experiment::treated), number("Người nhận có mua", MarketingReportDtos.Experiment::treatedBuyers),
                number("Nhóm đối chứng", MarketingReportDtos.Experiment::holdout), number("Đối chứng có mua", MarketingReportDtos.Experiment::holdoutBuyers),
                number("Tỷ lệ mua nhận tin (%)", MarketingReportDtos.Experiment::treatedRate),
                number("Tỷ lệ mua đối chứng (%)", MarketingReportDtos.Experiment::holdoutRate),
                number("Chênh lệch (điểm %)", MarketingReportDtos.Experiment::upliftPoints), number("z", MarketingReportDtos.Experiment::zScore),
                new ExcelColumn<>("Có ý nghĩa thống kê", e -> e.significant() ? "Có" : "Chưa"),
                number("Đơn tăng thêm (ước tính)", MarketingReportDtos.Experiment::incrementalOrders),
                money("Doanh thu tăng thêm (ước tính)", MarketingReportDtos.Experiment::incrementalRevenue));
        List<ExcelColumn<String>> notes = List.of(new ExcelColumn<>("Lưu ý về dữ liệu", Function.identity()));
        return excel.export(new ExcelWorkbookData(List.of(
                new ExcelSheet<>("Tong quan", overview, lines), new ExcelSheet<>("Theo ngay", days, r.daily()),
                new ExcelSheet<>("Theo uu dai", offers, r.offers()), new ExcelSheet<>("Doi chung", tests, r.experiments()),
                new ExcelSheet<>("Luu y", notes, r.dataNotes()))));
    }

    private static Line line(String label, Number value, String unit) {
        return new Line(label, value == null ? null : new BigDecimal(value.toString()), unit);
    }

    private static <T> ExcelColumn<T> number(String header, Function<T, ?> value) {
        return new ExcelColumn<>(header, value, ExcelCellFormat.NUMBER, null, ExcelAlignment.RIGHT);
    }

    private static <T> ExcelColumn<T> money(String header, Function<T, ?> value) {
        return new ExcelColumn<>(header, value, ExcelCellFormat.CURRENCY, "#,##0", ExcelAlignment.RIGHT);
    }

    // ------------------------------------------------------------------ hỗ trợ

    private void requireGlobal(UUID actor) {
        if (!access.isGlobal(actor)) throw BusinessException.forbidden("Chỉ nhân viên cấp chuỗi xem được báo cáo chiến dịch");
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return value instanceof BigDecimal d ? d : new BigDecimal(value.toString());
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(0, RoundingMode.HALF_UP);
    }

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}
