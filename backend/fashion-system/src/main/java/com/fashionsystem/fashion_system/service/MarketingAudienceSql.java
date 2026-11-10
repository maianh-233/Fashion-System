package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.promotion.VoucherCampaignDtos;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Cầu nối để module Marketing dùng lại đúng điều kiện nhóm khách của chiến dịch voucher
 * ({@link VoucherCampaignService#segment}, package-private) mà không sửa module đó. Lớp nằm trong package {@code service}
 * chỉ vì lý do truy cập; nó chỉ dựng SQL, không ghi dữ liệu.
 * <p>
 * Cờ "bỏ qua khách đã nhận voucher chiến dịch gần đây" luôn tắt: luật đó chỉ nói về voucher; thông báo marketing dùng giới
 * hạn tần suất riêng. Test hợp đồng giữ cho hai module đếm ra cùng một nhóm khách.
 */
public final class MarketingAudienceSql {
    /** Bản sao CTE danh mục + danh mục con mà {@link VoucherCampaignService#segment} tham chiếu ({@code cat_tree}). */
    public static final String CATEGORY_TREE = """
            with recursive cat_tree as (
                select id from categories where id = :categoryId
                union all
                select ch.id from categories ch join cat_tree t on ch.parent_id = t.id
            )""";

    private MarketingAudienceSql() {
    }

    /**
     * Điều kiện lọc khách {@code c} (bắt đầu bằng " where"); dùng sau {@link #CATEGORY_TREE} với {@link #params}.
     */
    public static String where(List<UUID> tierIds, Integer inactiveDays, UUID categoryId) {
        return VoucherCampaignService.segment(criteria(tierIds, inactiveDays, categoryId));
    }

    /** Tham số của {@link #where}: cùng tên với tham số của chiến dịch voucher. */
    public static MapSqlParameterSource params(List<UUID> tierIds, Integer inactiveDays, UUID categoryId, LocalDateTime now) {
        VoucherCampaignDtos.Criteria criteria = criteria(tierIds, inactiveDays, categoryId);
        return new MapSqlParameterSource("categoryId", categoryId)
                .addValue("tierIds", criteria.tierIdsOrEmpty())
                .addValue("inactiveSince", inactiveDays == null ? null : DbTimes.toDb(now.minusDays(inactiveDays)))
                .addValue("recentSince", DbTimes.toDb(now));
    }

    private static VoucherCampaignDtos.Criteria criteria(List<UUID> tierIds, Integer inactiveDays, UUID categoryId) {
        return new VoucherCampaignDtos.Criteria(tierIds, inactiveDays, categoryId, Boolean.FALSE);
    }
}
