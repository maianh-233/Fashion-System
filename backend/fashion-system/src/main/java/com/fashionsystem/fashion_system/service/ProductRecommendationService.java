package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.storefront.StorefrontDtos;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gợi ý trên trang sản phẩm, chỉ trả id theo thứ tự (thẻ sản phẩm lấy qua danh sách storefront theo ids).
 * <ul>
 *   <li>Thường được mua cùng: số "giỏ" (đơn, hoặc đơn tổng cùng các đơn con khi đơn bị tách) có cả hai sản phẩm.
 *       Bỏ đơn đã hủy và đơn trả qua cổng chưa thanh toán.</li>
 *   <li>Có thể bạn cũng thích: cùng danh mục (3 điểm), bộ sưu tập (2), thương hiệu (1); bằng điểm thì bán chạy hơn,
 *       rồi mới hơn. Không lặp lại sản phẩm đã có ở nhóm trên.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductRecommendationService {
    public static final int DEFAULT_LIMIT = 8;
    public static final int MAX_LIMIT = 12;

    /** Đơn được tính là đã mua: không hủy, không phải đơn qua cổng đang chờ / bỏ dở thanh toán. */
    private static final String COUNTED_ORDER = """
            o.status <> 'CANCELLED' and coalesce(o.payment_status, '') not in ('AWAITING_PAYMENT', 'ABANDONED')""";

    private final NamedParameterJdbcTemplate jdbc;

    public StorefrontDtos.Recommendations recommend(UUID productId, int limit) {
        int size = Math.max(1, Math.min(limit, MAX_LIMIT));
        MapSqlParameterSource params = new MapSqlParameterSource("product", productId).addValue("limit", size);

        List<UUID> together = jdbc.queryForList("""
                with baskets as (
                    select distinct coalesce(o.parent_order_id, o.id) as basket
                    from order_items oi join orders o on o.id = oi.order_id
                    where oi.product_id = :product and """ + " " + COUNTED_ORDER + """
                )
                select oi.product_id
                from baskets b
                join orders o on coalesce(o.parent_order_id, o.id) = b.basket
                join order_items oi on oi.order_id = o.id
                join products p on p.id = oi.product_id
                where oi.product_id <> :product and p.status = 'ACTIVE' and """ + " " + COUNTED_ORDER + """
                group by oi.product_id
                order by count(distinct b.basket) desc, max(o.created_at) desc, oi.product_id
                limit :limit""", params, UUID.class);

        List<UUID> exclude = new ArrayList<>(together);
        exclude.add(productId);
        List<UUID> similar = jdbc.queryForList("""
                select p.id
                from products p
                join products me on me.id = :product
                left join lateral (
                    select count(*) as sold from order_items oi join orders o on o.id = oi.order_id
                    where oi.product_id = p.id and """ + " " + COUNTED_ORDER + """
                ) sales on true
                where p.status = 'ACTIVE' and p.id not in (:exclude)
                  and exists (select 1 from product_variants v where v.product_id = p.id and v.active)
                  and (p.category_id = me.category_id or p.collection_id = me.collection_id or p.brand_id = me.brand_id)
                order by (case when p.category_id = me.category_id then 3 else 0 end
                        + case when p.collection_id = me.collection_id then 2 else 0 end
                        + case when p.brand_id = me.brand_id then 1 else 0 end) desc,
                         sales.sold desc, p.created_at desc, p.id
                limit :limit""", params.addValue("exclude", exclude), UUID.class);

        return new StorefrontDtos.Recommendations(together, similar);
    }
}
