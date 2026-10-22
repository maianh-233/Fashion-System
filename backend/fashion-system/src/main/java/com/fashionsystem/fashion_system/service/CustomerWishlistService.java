package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.storefront.WishlistDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sản phẩm yêu thích của khách (bảng {@code wishlist_items}). Chỉ trả sản phẩm còn đang bán; sản phẩm ngừng bán vẫn
 * được giữ để hiện lại nếu mở bán lại. Tối đa {@value WishlistDtos#MAX_ITEMS} sản phẩm mỗi khách.
 */
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(
        reason = "Wishlist is customer working state; it never changes orders, stock or payments")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerWishlistService {
    private final NamedParameterJdbcTemplate jdbc;

    public WishlistDtos.Wishlist view(UUID customerId) {
        return new WishlistDtos.Wishlist(jdbc.queryForList("""
                select w.product_id from wishlist_items w join products p on p.id = w.product_id
                where w.customer_id = :customerId and p.status = 'ACTIVE'
                order by w.created_at desc, w.product_id""",
                new MapSqlParameterSource("customerId", customerId), UUID.class));
    }

    @Transactional
    public WishlistDtos.Wishlist add(UUID customerId, UUID productId) {
        if (!sellable(productId)) throw BusinessException.notFound("Sản phẩm không tồn tại hoặc đã ngừng bán");
        if (!contains(customerId, productId) && count(customerId) >= WishlistDtos.MAX_ITEMS)
            throw BusinessException.conflict("Danh sách yêu thích tối đa " + WishlistDtos.MAX_ITEMS + " sản phẩm");
        insert(customerId, productId);
        return view(customerId);
    }

    @Transactional
    public WishlistDtos.Wishlist remove(UUID customerId, UUID productId) {
        jdbc.update("delete from wishlist_items where customer_id = :customerId and product_id = :productId",
                params(customerId, productId));
        return view(customerId);
    }

    /** Gộp danh sách trên trình duyệt: bỏ sản phẩm không còn bán, phần vượt giới hạn bị bỏ qua. */
    @Transactional
    public WishlistDtos.Wishlist merge(UUID customerId, WishlistDtos.GuestWishlist guest) {
        List<UUID> candidates = new ArrayList<>(new LinkedHashSet<>(guest.productIds()));
        if (!candidates.isEmpty()) {
            List<UUID> fresh = jdbc.queryForList("""
                    select p.id from products p where p.id in (:ids) and p.status = 'ACTIVE'
                    and not exists (select 1 from wishlist_items w where w.customer_id = :customerId and w.product_id = p.id)""",
                    new MapSqlParameterSource("ids", candidates).addValue("customerId", customerId), UUID.class);
            int room = Math.max(0, WishlistDtos.MAX_ITEMS - count(customerId));
            candidates.stream().filter(fresh::contains).limit(room).forEach(id -> insert(customerId, id));
        }
        return view(customerId);
    }

    private boolean sellable(UUID productId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from products where id = :productId and status = 'ACTIVE')",
                new MapSqlParameterSource("productId", productId), Boolean.class));
    }

    private boolean contains(UUID customerId, UUID productId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from wishlist_items where customer_id = :customerId and product_id = :productId)""",
                params(customerId, productId), Boolean.class));
    }

    private int count(UUID customerId) {
        Integer count = jdbc.queryForObject("select count(*) from wishlist_items where customer_id = :customerId",
                new MapSqlParameterSource("customerId", customerId), Integer.class);
        return count == null ? 0 : count;
    }

    private void insert(UUID customerId, UUID productId) {
        jdbc.update("""
                insert into wishlist_items (customer_id, product_id) values (:customerId, :productId)
                on conflict (customer_id, product_id) do nothing""", params(customerId, productId));
    }

    private static MapSqlParameterSource params(UUID customerId, UUID productId) {
        return new MapSqlParameterSource("customerId", customerId).addValue("productId", productId);
    }
}
