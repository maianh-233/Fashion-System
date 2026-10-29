package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.dto.storefront.SizeAdvisorDtos;
import com.fashionsystem.fashion_system.dto.storefront.SizeAdvisorDtos.ChartEntry;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gợi ý size. Admin nhập bảng size (khoảng chiều cao / cân nặng cho từng size) theo danh mục; danh mục không có
 * bảng riêng dùng bảng của danh mục cha gần nhất. Khách lưu số đo trong hồ sơ. Gợi ý ưu tiên size khách đã mua và
 * giữ lại (đơn hoàn thành, không trả; đổi hàng thì tính size đổi sang) trong cùng danh mục, sau đó mới tới bảng size.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SizeAdvisorService {
    /** Sai lệch được quy đổi để so chiều cao với cân nặng: 5 cm ~ 3 kg ~ một bậc size. */
    private static final double CM_PER_STEP = 5.0;
    private static final double KG_PER_STEP = 3.0;
    private static final List<String> SIZE_ORDER = List.of("XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL", "FREESIZE");

    private final NamedParameterJdbcTemplate jdbc;

    // ------------------------------------------------------------------ admin

    public SizeAdvisorDtos.Chart chart(UUID categoryId) {
        String name = categoryName(categoryId);
        return new SizeAdvisorDtos.Chart(categoryId, name, entries(categoryId));
    }

    @Transactional
    @BusinessAudit("SIZE_CHART")
    public SizeAdvisorDtos.Chart replaceChart(UUID categoryId, SizeAdvisorDtos.ChartRequest request) {
        String name = categoryName(categoryId);
        Set<String> seen = new HashSet<>();
        for (ChartEntry entry : request.entries()) {
            if (!seen.add(entry.size().trim().toLowerCase(Locale.ROOT)))
                throw BusinessException.badRequest("Size " + entry.size().trim() + " bị trùng trong bảng");
            if (entry.minHeightCm() == null && entry.maxHeightCm() == null
                    && entry.minWeightKg() == null && entry.maxWeightKg() == null)
                throw BusinessException.badRequest("Size " + entry.size().trim() + " cần ít nhất một khoảng chiều cao hoặc cân nặng");
            if (entry.minHeightCm() != null && entry.maxHeightCm() != null && entry.minHeightCm() > entry.maxHeightCm()
                    || entry.minWeightKg() != null && entry.maxWeightKg() != null
                    && entry.minWeightKg().compareTo(entry.maxWeightKg()) > 0)
                throw BusinessException.badRequest("Size " + entry.size().trim() + ": giá trị \"từ\" phải nhỏ hơn hoặc bằng \"đến\"");
        }
        MapSqlParameterSource category = new MapSqlParameterSource("categoryId", categoryId);
        jdbc.update("delete from size_chart_entries where category_id = :categoryId", category);
        for (int i = 0; i < request.entries().size(); i++) {
            ChartEntry entry = request.entries().get(i);
            jdbc.update("""
                    insert into size_chart_entries (category_id, size, min_height_cm, max_height_cm, min_weight_kg,
                                                    max_weight_kg, sort_order)
                    values (:categoryId, :size, :minHeight, :maxHeight, :minWeight, :maxWeight, :sortOrder)""",
                    new MapSqlParameterSource("categoryId", categoryId).addValue("size", entry.size().trim())
                            .addValue("minHeight", entry.minHeightCm()).addValue("maxHeight", entry.maxHeightCm())
                            .addValue("minWeight", entry.minWeightKg()).addValue("maxWeight", entry.maxWeightKg())
                            .addValue("sortOrder", i));
        }
        return new SizeAdvisorDtos.Chart(categoryId, name, entries(categoryId));
    }

    // ------------------------------------------------------------------ customer

    public SizeAdvisorDtos.Measurements measurements(UUID customerId) {
        return jdbc.query("select height_cm, weight_kg from customers where id = :id",
                        new MapSqlParameterSource("id", customerId), (rs, i) -> new SizeAdvisorDtos.Measurements(
                                (Integer) rs.getObject("height_cm", Integer.class), rs.getBigDecimal("weight_kg")))
                .stream().findFirst().orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
    }

    @Transactional
    @com.fashionsystem.fashion_system.audit.AuditInfrastructure(
            reason = "Body measurements are customer self-service preferences used only for size suggestions")
    public SizeAdvisorDtos.Measurements saveMeasurements(UUID customerId, SizeAdvisorDtos.Measurements request) {
        int updated = jdbc.update("""
                update customers set height_cm = :height, weight_kg = :weight, updated_at = now() at time zone 'UTC'
                where id = :id""", new MapSqlParameterSource("id", customerId)
                .addValue("height", request.heightCm()).addValue("weight", request.weightKg()));
        if (updated == 0) throw BusinessException.notFound("Khách hàng không tồn tại");
        return measurements(customerId);
    }

    /**
     * Hướng dẫn chọn size cho sản phẩm. Số đo truyền vào được ưu tiên; khách đăng nhập không truyền thì dùng số đo
     * đã lưu. {@code customerId} null với khách chưa đăng nhập (không có lịch sử mua).
     */
    public SizeAdvisorDtos.Guide guide(UUID productId, UUID customerId, Integer heightCm, BigDecimal weightKg) {
        Map<String, Object> product = jdbc.queryForList(
                "select category_id from products where id = :id and status = 'ACTIVE'",
                new MapSqlParameterSource("id", productId)).stream().findFirst()
                .orElseThrow(() -> BusinessException.notFound("Sản phẩm không tồn tại"));
        UUID categoryId = (UUID) product.get("category_id");
        if (heightCm != null && (heightCm < 80 || heightCm > 250))
            throw BusinessException.badRequest("Chiều cao từ 80 đến 250 cm");
        if (weightKg != null && (weightKg.compareTo(BigDecimal.valueOf(20)) < 0 || weightKg.compareTo(BigDecimal.valueOf(250)) > 0))
            throw BusinessException.badRequest("Cân nặng từ 20 đến 250 kg");
        if (customerId != null && heightCm == null && weightKg == null) {
            SizeAdvisorDtos.Measurements saved = measurements(customerId);
            heightCm = saved.heightCm();
            weightKg = saved.weightKg();
        }

        UUID chartCategory = categoryId == null ? null : chartCategory(categoryId);
        List<ChartEntry> entries = chartCategory == null ? List.of() : entries(chartCategory);
        List<String> productSizes = sortSizes(jdbc.queryForList("""
                select distinct trim(size) from product_variants
                where product_id = :id and active and size is not null and trim(size) <> ''""",
                new MapSqlParameterSource("id", productId), String.class), entries);

        String historySize = customerId == null || categoryId == null ? null : keptSize(customerId, categoryId);
        ChartEntry chartMatch = match(entries, heightCm, weightKg);
        String chartSize = chartMatch == null ? null : chartMatch.size();

        String recommended = null;
        String source = null;
        String note = null;
        String fromHistory = find(productSizes, historySize);
        String fromChart = find(productSizes, chartSize);
        if (fromHistory != null) {
            recommended = fromHistory;
            source = SizeAdvisorDtos.SOURCE_HISTORY;
            note = "Bạn thường mua và giữ size " + fromHistory + " ở danh mục này"
                    + (chartSize != null && !chartSize.equalsIgnoreCase(fromHistory) ? "; theo số đo là size " + chartSize : "");
        } else if (fromChart != null) {
            recommended = fromChart;
            source = SizeAdvisorDtos.SOURCE_CHART;
            note = "Theo bảng size với số đo của bạn";
        } else if (chartSize != null) {
            note = "Theo bảng size bạn mặc " + chartSize + ", nhưng sản phẩm này không có size đó";
        } else if (!entries.isEmpty() && heightCm == null && weightKg == null) {
            note = "Nhập chiều cao và cân nặng để được gợi ý size";
        }
        return new SizeAdvisorDtos.Guide(chartCategory == null ? null : categoryName(chartCategory), entries,
                productSizes, heightCm, weightKg, recommended, source, chartSize, historySize, note);
    }

    /**
     * Dòng bảng size gần số đo nhất. Sai lệch ngoài khoảng được quy về số bậc size ({@link #CM_PER_STEP},
     * {@link #KG_PER_STEP}); chỉ xét chiều đo mà cả khách và dòng bảng đều có. Bằng nhau thì chọn size lớn hơn
     * (đứng sau trong bảng), vì mặc rộng dễ chịu hơn mặc chật.
     */
    static ChartEntry match(List<ChartEntry> entries, Integer heightCm, BigDecimal weightKg) {
        if (heightCm == null && weightKg == null) return null;
        ChartEntry best = null;
        double bestScore = Double.MAX_VALUE;
        for (ChartEntry entry : entries) {
            boolean heightKnown = heightCm != null && (entry.minHeightCm() != null || entry.maxHeightCm() != null);
            boolean weightKnown = weightKg != null && (entry.minWeightKg() != null || entry.maxWeightKg() != null);
            if (!heightKnown && !weightKnown) continue;
            double score = 0;
            if (heightKnown)
                score += outside(heightCm, entry.minHeightCm() == null ? null : entry.minHeightCm().doubleValue(),
                        entry.maxHeightCm() == null ? null : entry.maxHeightCm().doubleValue()) / CM_PER_STEP;
            if (weightKnown)
                score += outside(weightKg.doubleValue(), entry.minWeightKg() == null ? null : entry.minWeightKg().doubleValue(),
                        entry.maxWeightKg() == null ? null : entry.maxWeightKg().doubleValue()) / KG_PER_STEP;
            if (score <= bestScore) {
                best = entry;
                bestScore = score;
            }
        }
        return best;
    }

    private static double outside(double value, Double min, Double max) {
        if (min != null && value < min) return min - value;
        if (max != null && value > max) return value - max;
        return 0;
    }

    /** Size xuất hiện nhiều nhất (rồi gần nhất) trong các món khách đã mua và giữ lại thuộc danh mục. */
    private String keptSize(UUID customerId, UUID categoryId) {
        return jdbc.queryForList("""
                with kept as (
                    select trim(coalesce(ev.size, oi.size)) as size, o.created_at, oi.quantity
                    from order_items oi
                    join orders o on o.id = oi.order_id
                    join product_variants v on v.id = oi.product_variant_id
                    join products p on p.id = v.product_id
                    left join lateral (
                        select rr.request_type, rr.status, ri.exchange_variant_id
                        from return_request_items ri join return_requests rr on rr.id = ri.request_id
                        where ri.order_item_id = oi.id and rr.status not in ('REJECTED', 'CANCELLED')
                        order by rr.created_at desc limit 1) r on true
                    left join product_variants ev on ev.id = r.exchange_variant_id
                    where o.customer_id = :customerId and o.status = 'COMPLETED' and p.category_id = :categoryId
                      and (r.request_type is null or (r.request_type = 'EXCHANGE' and r.status = 'COMPLETED'))
                )
                select min(size) from kept where size is not null and size <> ''
                group by lower(size) order by sum(quantity) desc, max(created_at) desc limit 1""",
                new MapSqlParameterSource("customerId", customerId).addValue("categoryId", categoryId), String.class)
                .stream().findFirst().orElse(null);
    }

    /** Danh mục gần nhất (chính nó rồi lên các cấp cha) có bảng size. */
    private UUID chartCategory(UUID categoryId) {
        return jdbc.queryForList("""
                with recursive up (id, parent_id, depth) as (
                    select id, parent_id, 0 from categories where id = :categoryId
                    union all
                    select c.id, c.parent_id, up.depth + 1 from categories c join up on c.id = up.parent_id
                    where up.depth < 10
                )
                select up.id from up
                where exists (select 1 from size_chart_entries e where e.category_id = up.id)
                order by up.depth limit 1""", new MapSqlParameterSource("categoryId", categoryId), UUID.class)
                .stream().findFirst().orElse(null);
    }

    private List<ChartEntry> entries(UUID categoryId) {
        return jdbc.query("""
                select size, min_height_cm, max_height_cm, min_weight_kg, max_weight_kg from size_chart_entries
                where category_id = :categoryId order by sort_order, size""",
                new MapSqlParameterSource("categoryId", categoryId), (rs, i) -> new ChartEntry(rs.getString("size"),
                        (Integer) rs.getObject("min_height_cm", Integer.class),
                        (Integer) rs.getObject("max_height_cm", Integer.class),
                        rs.getBigDecimal("min_weight_kg"), rs.getBigDecimal("max_weight_kg")));
    }

    private String categoryName(UUID categoryId) {
        return jdbc.queryForList("select name from categories where id = :id",
                        new MapSqlParameterSource("id", categoryId), String.class).stream().findFirst()
                .orElseThrow(() -> BusinessException.notFound("Danh mục không tồn tại"));
    }

    /** Theo thứ tự trong bảng size, rồi XS…XXXL, rồi số (size quần/giày), rồi chữ cái. */
    static List<String> sortSizes(List<String> sizes, List<ChartEntry> entries) {
        List<String> chartOrder = entries.stream().map(e -> e.size().trim().toLowerCase(Locale.ROOT)).toList();
        List<String> sorted = new ArrayList<>(sizes);
        sorted.sort(Comparator.<String>comparingInt(s -> rank(chartOrder, s.toLowerCase(Locale.ROOT)))
                .thenComparingInt(s -> rank(SIZE_ORDER, s.toUpperCase(Locale.ROOT)))
                .thenComparingDouble(SizeAdvisorService::numeric)
                .thenComparing(String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }

    private static int rank(List<String> order, String value) {
        int index = order.indexOf(value);
        return index < 0 ? order.size() : index;
    }

    private static double numeric(String size) {
        try {
            return Double.parseDouble(size.replace(',', '.'));
        } catch (NumberFormatException notNumber) {
            return Double.MAX_VALUE;
        }
    }

    private static String find(List<String> sizes, String size) {
        if (size == null) return null;
        return sizes.stream().filter(s -> s.equalsIgnoreCase(size.trim())).findFirst().orElse(null);
    }
}
