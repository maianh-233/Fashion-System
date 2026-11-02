package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.ReturnDtos;
import com.fashionsystem.fashion_system.dto.order.ReturnSizeReportDtos;
import com.fashionsystem.fashion_system.dto.order.ReturnSizeReportDtos.ExchangeFlow;
import com.fashionsystem.fashion_system.dto.order.ReturnSizeReportDtos.FitHint;
import com.fashionsystem.fashion_system.dto.order.ReturnSizeReportDtos.ProductRow;
import com.fashionsystem.fashion_system.dto.order.ReturnSizeReportDtos.SizeRow;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tỷ lệ đổi / trả theo sản phẩm và size, nhấn vào lý do "không vừa size" để biết mẫu nào cần chỉnh bảng size.
 *
 * <p>Đơn bán: đơn cửa hàng (không tính đơn tổng của đơn tách) đã hoàn thành, tạo trong kỳ. Hàng đổi / trả: món của chính
 * các đơn đó nằm trong yêu cầu đã duyệt hoặc hoàn tất, cả trả lẫn đổi. Đổi size cho biết chiều lệch: đổi lên size lớn
 * hơn là form nhỏ, đổi xuống là form rộng.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnSizeReportService {
    static final String WRONG_SIZE = "WRONG_SIZE";
    /** Sản phẩm cần xem lại bảng size: ít nhất {@value #FLAG_MIN_QUANTITY} món và {@value #FLAG_MIN_RATE}% "không vừa size". */
    static final int FLAG_MIN_QUANTITY = 2;
    static final double FLAG_MIN_RATE = 10.0;
    static final int DEFAULT_DAYS = 90;
    static final int MAX_DAYS = 731;
    private static final List<String> LETTER_SIZES = List.of("XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String NO_SIZE = "—";

    private final StoreAccessService storeAccess;
    private final NamedParameterJdbcTemplate jdbc;

    /** Một nhóm món đổi / trả cùng sản phẩm, size, lý do và size đổi sang. */
    record ReturnedRow(UUID productId, String size, String reasonCode, String exchangeSize, long quantity) {
    }

    record ProductInfo(String name, UUID categoryId, String categoryName) {
    }

    public ReturnSizeReportDtos.Report report(UUID actor, UUID storeId, LocalDate from, LocalDate to) {
        UUID store = storeAccess.requireOrAll(actor, storeId, ReturnRequestService.VIEW);
        LocalDate end = to == null ? LocalDate.now(ZONE) : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS - 1) : from;
        if (start.isAfter(end)) throw BusinessException.badRequest("Ngày bắt đầu phải trước ngày kết thúc");
        if (ChronoUnit.DAYS.between(start, end) >= MAX_DAYS)
            throw BusinessException.badRequest("Chỉ xem được tối đa 2 năm mỗi lần");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", DbTimes.toDb(start.atStartOfDay()))
                .addValue("to", DbTimes.toDb(end.plusDays(1).atStartOfDay()));
        String storeFilter = "";
        if (store != null) {
            storeFilter = " and o.store_id = :store";
            params.addValue("store", store);
        }
        String soldOrders = """
                 join orders o on o.id = oi.order_id
                where o.store_id is not null and o.status = 'COMPLETED' and oi.product_id is not null
                  and o.created_at >= :from and o.created_at < :to""" + storeFilter;

        Map<UUID, Map<String, Long>> sold = new HashMap<>();
        jdbc.query("select oi.product_id, " + sizeKey("oi.size") + " as size, sum(oi.quantity) as qty from order_items oi"
                        + soldOrders + " group by 1, 2", params,
                rs -> {
                    sold.computeIfAbsent(rs.getObject("product_id", UUID.class), ignored -> new HashMap<>())
                            .merge(rs.getString("size"), rs.getLong("qty"), Long::sum);
                });
        List<ReturnedRow> returned = jdbc.query("""
                select oi.product_id, %s as size, r.reason_code, nullif(upper(trim(ex.size)), '') as exchange_size,
                       sum(ri.quantity) as qty
                  from return_request_items ri
                  join return_requests r on r.id = ri.request_id and r.status in ('APPROVED', 'COMPLETED')
                  join order_items oi on oi.id = ri.order_item_id
                  left join product_variants ex on ex.id = ri.exchange_variant_id
                """.formatted(sizeKey("oi.size")) + soldOrders + " group by 1, 2, 3, 4", params,
                (rs, i) -> new ReturnedRow(rs.getObject("product_id", UUID.class), rs.getString("size"),
                        rs.getString("reason_code"), rs.getString("exchange_size"), rs.getLong("qty")));

        Map<UUID, ProductInfo> info = productInfo(returned.stream().map(ReturnedRow::productId).distinct().toList());
        return build(start, end, sold, returned, info);
    }

    /** Ghép số liệu đã truy vấn thành báo cáo; tách riêng để kiểm thử không cần DB. */
    static ReturnSizeReportDtos.Report build(LocalDate from, LocalDate to, Map<UUID, Map<String, Long>> sold,
                                             List<ReturnedRow> returned, Map<UUID, ProductInfo> info) {
        long soldTotal = sold.values().stream().flatMap(m -> m.values().stream()).mapToLong(Long::longValue).sum();
        long returnedTotal = returned.stream().mapToLong(ReturnedRow::quantity).sum();
        long wrongSizeTotal = returned.stream().filter(r -> WRONG_SIZE.equals(r.reasonCode()))
                .mapToLong(ReturnedRow::quantity).sum();

        Map<String, Long> byReason = new HashMap<>();
        returned.forEach(r -> byReason.merge(r.reasonCode(), r.quantity(), Long::sum));
        List<ReturnSizeReportDtos.ReasonCount> reasons = new ArrayList<>();
        ReturnDtos.REASONS.forEach(reason -> {
            Long quantity = byReason.remove(reason.code());
            if (quantity != null) reasons.add(new ReturnSizeReportDtos.ReasonCount(reason.code(), reason.label(), quantity));
        });
        byReason.forEach((code, quantity) -> reasons.add(
                new ReturnSizeReportDtos.ReasonCount(code, ReturnDtos.reasonLabel(code), quantity)));
        reasons.sort(Comparator.comparingLong(ReturnSizeReportDtos.ReasonCount::quantity).reversed());

        Map<UUID, Map<String, List<ReturnedRow>>> grouped = new LinkedHashMap<>();
        returned.forEach(r -> grouped.computeIfAbsent(r.productId(), ignored -> new HashMap<>())
                .computeIfAbsent(r.size(), ignored -> new ArrayList<>()).add(r));

        List<ProductRow> products = new ArrayList<>();
        grouped.forEach((productId, bySize) -> {
            Map<String, Long> productSold = sold.getOrDefault(productId, Map.of());
            List<String> sizes = SizeAdvisorService.sortSizes(new ArrayList<>(productSold.keySet()), List.of());
            List<SizeRow> sizeRows = sizes.stream()
                    .map(size -> sizeRow(size, productSold.getOrDefault(size, 0L), bySize.getOrDefault(size, List.of())))
                    .toList();
            long productSoldQty = productSold.values().stream().mapToLong(Long::longValue).sum();
            long productReturned = sizeRows.stream().mapToLong(SizeRow::returnedQuantity).sum();
            long productWrong = sizeRows.stream().mapToLong(SizeRow::wrongSizeQuantity).sum();
            double wrongRate = rate(productWrong, productSoldQty);
            ProductInfo product = info.getOrDefault(productId, new ProductInfo(null, null, null));
            products.add(new ProductRow(productId, product.name(), product.categoryId(), product.categoryName(),
                    productSoldQty, productReturned, productWrong, rate(productReturned, productSoldQty), wrongRate,
                    productWrong >= FLAG_MIN_QUANTITY && wrongRate >= FLAG_MIN_RATE,
                    fitHint(sizeRows.stream().mapToLong(SizeRow::exchangedUp).sum(),
                            sizeRows.stream().mapToLong(SizeRow::exchangedDown).sum()),
                    sizeRows));
        });
        products.sort(Comparator.comparing(ProductRow::flagged).reversed()
                .thenComparing(Comparator.comparingLong(ProductRow::wrongSizeQuantity).reversed())
                .thenComparing(Comparator.comparingDouble(ProductRow::wrongSizeRate).reversed())
                .thenComparing(Comparator.comparingLong(ProductRow::returnedQuantity).reversed())
                .thenComparing(p -> p.productName() == null ? "" : p.productName(), String.CASE_INSENSITIVE_ORDER));

        return new ReturnSizeReportDtos.Report(from, to,
                new ReturnSizeReportDtos.Summary(soldTotal, returnedTotal, wrongSizeTotal, rate(returnedTotal, soldTotal),
                        rate(wrongSizeTotal, soldTotal)),
                List.copyOf(reasons), List.copyOf(products), (int) products.stream().filter(ProductRow::flagged).count());
    }

    private static SizeRow sizeRow(String size, long soldQty, List<ReturnedRow> rows) {
        long returnedQty = rows.stream().mapToLong(ReturnedRow::quantity).sum();
        long wrongQty = 0, up = 0, down = 0;
        Map<String, Long> flows = new HashMap<>();
        for (ReturnedRow row : rows) {
            if (row.exchangeSize() != null) flows.merge(row.exchangeSize(), row.quantity(), Long::sum);
            if (!WRONG_SIZE.equals(row.reasonCode())) continue;
            wrongQty += row.quantity();
            Integer direction = direction(size, row.exchangeSize());
            if (direction != null && direction > 0) up += row.quantity();
            if (direction != null && direction < 0) down += row.quantity();
        }
        List<ExchangeFlow> exchanges = flows.entrySet().stream()
                .map(e -> new ExchangeFlow(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ExchangeFlow::quantity).reversed().thenComparing(ExchangeFlow::toSize))
                .toList();
        return new SizeRow(size, soldQty, returnedQty, wrongQty, rate(returnedQty, soldQty), rate(wrongQty, soldQty),
                up, down, fitHint(up, down), exchanges);
    }

    /** Đổi lên size lớn hơn nhiều hơn → form nhỏ; ngược lại → form rộng; bằng nhau hoặc không có → chưa rõ. */
    static FitHint fitHint(long up, long down) {
        if (up > down) return FitHint.RUNS_SMALL;
        if (down > up) return FitHint.RUNS_LARGE;
        return null;
    }

    /**
     * Chiều đổi size: dương là lên size lớn hơn, âm là xuống, 0 cùng size; null khi không so được (không đổi, size
     * lạ, hoặc một bên chữ một bên số).
     */
    static Integer direction(String from, String to) {
        if (from == null || to == null) return null;
        String a = from.trim().toUpperCase(Locale.ROOT), b = to.trim().toUpperCase(Locale.ROOT);
        int ia = LETTER_SIZES.indexOf(a), ib = LETTER_SIZES.indexOf(b);
        if (ia >= 0 && ib >= 0) return Integer.compare(ib, ia);
        Double na = number(a), nb = number(b);
        if (na != null && nb != null) return Double.compare(nb, na);
        return a.equals(b) ? 0 : null;
    }

    private static Double number(String size) {
        try {
            return Double.parseDouble(size.replace(',', '.'));
        } catch (NumberFormatException notNumber) {
            return null;
        }
    }

    /** Phần trăm, làm tròn 1 chữ số thập phân; 0 khi chưa bán. */
    static double rate(long part, long whole) {
        return whole <= 0 ? 0 : Math.round(part * 1000.0 / whole) / 10.0;
    }

    private static String sizeKey(String column) {
        return "coalesce(nullif(upper(trim(" + column + ")), ''), '" + NO_SIZE + "')";
    }

    private Map<UUID, ProductInfo> productInfo(List<UUID> productIds) {
        Map<UUID, ProductInfo> info = new HashMap<>();
        if (productIds.isEmpty()) return info;
        jdbc.query("""
                select p.id, p.name, c.id as category_id, c.name as category_name
                  from products p left join categories c on c.id = p.category_id
                 where p.id in (:ids)""", new MapSqlParameterSource("ids", productIds),
                rs -> {
                    info.put(rs.getObject("id", UUID.class), new ProductInfo(rs.getString("name"),
                            rs.getObject("category_id", UUID.class), rs.getString("category_name")));
                });
        return info;
    }
}
