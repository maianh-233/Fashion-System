package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.storefront.StorefrontDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thương hiệu, bộ sưu tập và khuyến mãi cho cửa hàng online (chỉ đọc, công khai).
 *
 * <ul>
 *   <li>Thương hiệu: đang hợp tác (ACTIVE).</li>
 *   <li>Bộ sưu tập: đang phát hành (ACTIVE) của thương hiệu đang hợp tác; bản nháp không hiện.</li>
 *   <li>Khuyến mãi: đang áp dụng hoặc sắp diễn ra (ACTIVE / SCHEDULED, chưa quá ngày kết thúc). Chương trình đã
 *       tắt hoặc hết hạn không hiện.</li>
 * </ul>
 * Số sản phẩm chỉ đếm sản phẩm đang bán (ACTIVE và còn biến thể hoạt động), như danh sách sản phẩm.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorefrontBrowseService {
    private static final String SELLABLE = """
            p.status = 'ACTIVE' and exists (select 1 from product_variants v where v.product_id = p.id and v.active = true)""";

    private final NamedParameterJdbcTemplate jdbc;

    public enum PromotionSort {
        ENDING_SOON("p.end_date, p.code"), NEWEST("p.start_date desc, p.code");

        final String orderBy;

        PromotionSort(String orderBy) {
            this.orderBy = orderBy;
        }
    }

    // ---------------- Thương hiệu ----------------

    public Page<StorefrontDtos.Brand> brands(String keyword, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" where b.status = 'ACTIVE'");
        keywordFilter(keyword, params, where, "b.name", "b.code");
        return page(BRAND_SELECT + where + " order by product_count desc, b.name", "select count(*) from brands b" + where,
                params, page, size, StorefrontBrowseService::toBrand);
    }

    public StorefrontDtos.Brand brand(UUID id) {
        List<StorefrontDtos.Brand> found = jdbc.query(BRAND_SELECT + " where b.status = 'ACTIVE' and b.id = :id",
                new MapSqlParameterSource("id", id), (rs, i) -> toBrand(rs));
        if (found.isEmpty()) throw BusinessException.notFound("Thương hiệu không tồn tại hoặc đã ngừng hợp tác");
        return found.getFirst();
    }

    private static final String BRAND_SELECT = """
            select b.id, b.code, b.name, b.logo, b.description,
                   (select count(*) from products p where p.brand_id = b.id and %s) as product_count,
                   (select count(*) from collections c where c.brand_id = b.id and c.status = 'ACTIVE') as collection_count
            from brands b""".formatted(SELLABLE);

    private static StorefrontDtos.Brand toBrand(ResultSet rs) throws SQLException {
        return new StorefrontDtos.Brand(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                blankToNull(rs.getString("logo")), rs.getString("description"), rs.getLong("product_count"),
                rs.getLong("collection_count"));
    }

    // ---------------- Bộ sưu tập ----------------

    public Page<StorefrontDtos.Collection> collections(String keyword, UUID brandId, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" where c.status = 'ACTIVE' and b.status = 'ACTIVE'");
        keywordFilter(keyword, params, where, "c.name", "b.name");
        if (brandId != null) {
            params.addValue("brandId", brandId);
            where.append(" and c.brand_id = :brandId");
        }
        return page(COLLECTION_SELECT + where + " order by c.release_date desc nulls last, c.name",
                "select count(*) from collections c join brands b on b.id = c.brand_id" + where,
                params, page, size, StorefrontBrowseService::toCollection);
    }

    public StorefrontDtos.Collection collection(UUID id) {
        List<StorefrontDtos.Collection> found = jdbc.query(COLLECTION_SELECT
                        + " where c.status = 'ACTIVE' and b.status = 'ACTIVE' and c.id = :id",
                new MapSqlParameterSource("id", id), (rs, i) -> toCollection(rs));
        if (found.isEmpty()) throw BusinessException.notFound("Bộ sưu tập không tồn tại hoặc chưa phát hành");
        return found.getFirst();
    }

    private static final String COLLECTION_SELECT = """
            select c.id, c.code, c.name, c.season, c.year, c.release_date, c.description, c.image_url,
                   b.id as brand_id, b.name as brand_name, b.logo as brand_logo,
                   (select count(*) from products p where p.collection_id = c.id and %s) as product_count
            from collections c join brands b on b.id = c.brand_id""".formatted(SELLABLE);

    private static StorefrontDtos.Collection toCollection(ResultSet rs) throws SQLException {
        return new StorefrontDtos.Collection(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                rs.getString("season"), (Integer) rs.getObject("year"), rs.getObject("release_date", LocalDate.class),
                rs.getString("description"), blankToNull(rs.getString("image_url")),
                new StorefrontDtos.Ref(rs.getObject("brand_id", UUID.class), rs.getString("brand_name")),
                blankToNull(rs.getString("brand_logo")), rs.getLong("product_count"));
    }

    // ---------------- Khuyến mãi ----------------

    /**
     * @param promotionType ORDER, PRODUCT hoặc POINT_REDEMPTION
     * @param discountType  PERCENT hoặc FIXED
     * @param from          lọc chương trình còn hiệu lực trong khoảng [from, to] (giao nhau)
     */
    public Page<StorefrontDtos.Promotion> promotions(String keyword, String promotionType, String discountType,
                                                     LocalDate from, LocalDate to, PromotionSort sort, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource("now", LocalDateTime.now());
        StringBuilder where = new StringBuilder(" where p.status in ('ACTIVE', 'SCHEDULED') and p.end_date > :now");
        keywordFilter(keyword, params, where, "p.name", "p.code");
        if (promotionType != null) {
            params.addValue("promotionType", promotionType);
            where.append(" and p.promotion_type = :promotionType");
        }
        if (discountType != null) {
            params.addValue("discountType", discountType);
            where.append(" and p.discount_type = :discountType");
        }
        if (from != null) {
            params.addValue("from", from.atStartOfDay());
            where.append(" and p.end_date > :from");
        }
        if (to != null) {
            params.addValue("to", to.plusDays(1).atStartOfDay());
            where.append(" and p.start_date < :to");
        }
        Page<StorefrontDtos.Promotion> result = page(PROMOTION_SELECT + where + " order by "
                        + (sort == null ? PromotionSort.ENDING_SOON : sort).orderBy,
                "select count(*) from promotions p" + where, params, page, size, StorefrontBrowseService::toPromotion);
        if (result.isEmpty()) return result;

        Map<UUID, List<String>> tiers = new HashMap<>();
        jdbc.query("""
                select pt.promotion_id, t.name from promotion_tiers pt join customer_tiers t on t.id = pt.tier_id
                where pt.promotion_id in (:ids) order by t.min_total_spent nulls first, t.name
                """, new MapSqlParameterSource("ids", result.map(StorefrontDtos.Promotion::id).toList()), rs -> {
            tiers.computeIfAbsent(rs.getObject("promotion_id", UUID.class), ignored -> new ArrayList<>())
                    .add(rs.getString("name"));
        });
        return result.map(p -> new StorefrontDtos.Promotion(p.id(), p.code(), p.name(), p.promotionType(),
                p.discountType(), p.discountValue(), p.maxDiscount(), p.minOrderValue(), p.startDate(), p.endDate(),
                p.status(), p.pointsRequired(), p.usagePerUser(), tiers.getOrDefault(p.id(), List.of()), p.soldOut(),
                p.productCount()));
    }

    private static final String PROMOTION_SELECT = """
            select p.id, p.code, p.name, p.promotion_type, p.discount_type, p.discount_value, p.max_discount,
                   p.min_order_value, p.start_date, p.end_date, p.status, p.points_required, p.usage_per_user,
                   (p.promotion_type <> 'POINT_REDEMPTION' and p.usage_limit is not null
                        and (select count(*) from promotion_usages u where u.promotion_id = p.id) >= p.usage_limit) as sold_out,
                   (select count(*) from promotion_products pp join products x on x.id = pp.product_id
                        where pp.promotion_id = p.id and x.status = 'ACTIVE') as product_count
            from promotions p""";

    private static StorefrontDtos.Promotion toPromotion(ResultSet rs) throws SQLException {
        return new StorefrontDtos.Promotion(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                rs.getString("promotion_type"), rs.getString("discount_type"), rs.getBigDecimal("discount_value"),
                rs.getBigDecimal("max_discount"), rs.getBigDecimal("min_order_value"),
                rs.getObject("start_date", LocalDateTime.class), rs.getObject("end_date", LocalDateTime.class),
                rs.getString("status"), (Integer) rs.getObject("points_required"),
                (Integer) rs.getObject("usage_per_user"), List.of(), rs.getBoolean("sold_out"), rs.getLong("product_count"));
    }

    // ---------------- Chung ----------------

    private interface Mapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private <T> Page<T> page(String select, String count, MapSqlParameterSource params, int page, int size, Mapper<T> mapper) {
        int safeSize = Math.clamp(size, 1, StorefrontCatalogService.MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Long total = jdbc.queryForObject(count, params, Long.class);
        if (total == null || total == 0) return Page.empty(PageRequest.of(safePage, safeSize));
        params.addValue("limit", safeSize).addValue("offset", (long) safePage * safeSize);
        List<T> content = jdbc.query(select + " limit :limit offset :offset", params, (rs, i) -> mapper.map(rs));
        return new PageImpl<>(content, PageRequest.of(safePage, safeSize), total);
    }

    /** Từ khóa không phân biệt dấu trên hai cột. */
    private static void keywordFilter(String keyword, MapSqlParameterSource params, StringBuilder where,
                                      String first, String second) {
        String folded = StorefrontCatalogService.fold(keyword);
        if (folded.isEmpty()) return;
        params.addValue("pattern", "%" + folded + "%");
        where.append(" and (").append(StorefrontCatalogService.folded(first)).append(" like :pattern or ")
                .append(StorefrontCatalogService.folded("coalesce(" + second + ", '')")).append(" like :pattern)");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
