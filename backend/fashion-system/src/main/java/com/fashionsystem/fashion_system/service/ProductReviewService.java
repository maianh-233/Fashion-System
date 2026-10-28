package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.dto.StorageUploadResult;
import com.fashionsystem.fashion_system.dto.storefront.ReviewDtos;
import com.fashionsystem.fashion_system.entity.ProductReview;
import com.fashionsystem.fashion_system.entity.ProductReviewImage;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.ProductReviewImageRepository;
import com.fashionsystem.fashion_system.repository.ProductReviewRepository;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * Đánh giá sản phẩm. Ghi qua JPA (để business audit ghi lại việc tạo, ẩn, trả lời); đọc bằng SQL, không audit.
 * Chỉ khách có đơn {@code COMPLETED} chứa sản phẩm mới đánh giá được, mỗi (đơn, sản phẩm) một lần.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReviewService {
    private final ProductReviewRepository reviews;
    private final ProductReviewImageRepository images;
    private final StorageService storage;
    private final NamedParameterJdbcTemplate jdbc;

    /** Ảnh biến thể chính khi sản phẩm không có ảnh bìa (như danh sách sản phẩm storefront). */
    private static final String PRODUCT_IMAGE = """
            (select i.image_url from product_images i join product_variants iv on iv.id = i.product_variant_id
             where iv.product_id = p.id order by i.is_primary desc nulls last, i.sort_order nulls last limit 1)""";

    // ---------------------------------------------------------------- customer

    @Transactional
    @BusinessAudit("PRODUCT_REVIEW")
    public ReviewDtos.MyReview create(UUID customerId, UUID orderId, UUID productId, Integer rating, String content,
                                      List<MultipartFile> files) {
        if (rating == null || rating < 1 || rating > 5) throw BusinessException.badRequest("Số sao phải từ 1 đến 5");
        String text = content == null ? null : content.trim();
        if (text != null && text.isEmpty()) text = null;
        if (text != null && text.length() > ReviewDtos.MAX_CONTENT)
            throw BusinessException.badRequest("Nhận xét tối đa " + ReviewDtos.MAX_CONTENT + " ký tự");
        List<MultipartFile> uploads = files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (uploads.size() > ReviewDtos.MAX_IMAGES)
            throw BusinessException.badRequest("Tối đa " + ReviewDtos.MAX_IMAGES + " ảnh cho mỗi đánh giá");
        if (uploads.stream().anyMatch(file -> file.getSize() > ReviewDtos.MAX_IMAGE_BYTES))
            throw BusinessException.badRequest("Mỗi ảnh đánh giá tối đa 5 MB");

        List<UUID> purchasedVariants = jdbc.queryForList("""
                select oi.product_variant_id from order_items oi join orders o on o.id = oi.order_id
                where o.id = :orderId and o.customer_id = :customerId and o.status = 'COMPLETED' and oi.product_id = :productId
                order by oi.created_at""",
                new MapSqlParameterSource("orderId", orderId).addValue("customerId", customerId).addValue("productId", productId),
                UUID.class);
        if (purchasedVariants.isEmpty())
            throw BusinessException.badRequest("Chỉ đánh giá được sản phẩm trong đơn hàng đã hoàn thành của bạn");
        if (reviews.existsByOrderIdAndProductId(orderId, productId))
            throw BusinessException.conflict("Bạn đã đánh giá sản phẩm này trong đơn hàng này");

        ProductReview review = reviews.saveAndFlush(ProductReview.builder()
                .productId(productId).productVariantId(purchasedVariants.getFirst()).orderId(orderId).customerId(customerId)
                .rating(rating.shortValue()).content(text).hidden(false).createdAt(LocalDateTime.now()).build());
        for (int i = 0; i < uploads.size(); i++) {
            StorageUploadResult uploaded = storage.uploadImage(uploads.get(i), "reviews/" + review.getId());
            deleteOnRollback(uploaded.publicId());
            images.save(ProductReviewImage.builder().reviewId(review.getId()).imageUrl(uploaded.secureUrl())
                    .publicId(uploaded.publicId()).sortOrder((short) i).build());
        }
        images.flush();
        return mine(customerId).stream().filter(r -> r.id().equals(review.getId())).findFirst().orElseThrow();
    }

    /** Sản phẩm trong các đơn đã hoàn thành của khách mà chưa được đánh giá, mới nhất trước. */
    public List<ReviewDtos.Pending> pending(UUID customerId) {
        return jdbc.query("""
                select distinct on (o.id, oi.product_id) o.id as order_id, o.order_code, oi.product_id,
                       oi.product_variant_id, oi.product_name, oi.color, oi.size,
                       coalesce(nullif(oi.image_url, ''), nullif(p.image_url, ''), :productImage) as image_url,
                       coalesce(o.updated_at, o.created_at) as completed_at
                from orders o
                join order_items oi on oi.order_id = o.id
                join products p on p.id = oi.product_id
                where o.customer_id = :customerId and o.status = 'COMPLETED'
                  and not exists (select 1 from product_reviews r where r.order_id = o.id and r.product_id = oi.product_id)
                order by o.id, oi.product_id, oi.created_at""".replace(":productImage", PRODUCT_IMAGE),
                new MapSqlParameterSource("customerId", customerId), (rs, i) -> new ReviewDtos.Pending(
                        rs.getObject("order_id", UUID.class), rs.getString("order_code"),
                        rs.getObject("product_id", UUID.class), rs.getObject("product_variant_id", UUID.class),
                        rs.getString("product_name"), variantLabel(rs.getString("color"), rs.getString("size")),
                        rs.getString("image_url"), time(rs, "completed_at")))
                .stream().sorted((a, b) -> compareDesc(a.completedAt(), b.completedAt())).toList();
    }

    public List<ReviewDtos.MyReview> mine(UUID customerId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select r.id, r.order_id, o.order_code, r.product_id, p.name as product_name,
                       coalesce(nullif(p.image_url, ''), :productImage) as product_image,
                       v.color, v.size, r.rating, r.content, r.hidden, r.reply, r.created_at
                from product_reviews r
                join orders o on o.id = r.order_id
                join products p on p.id = r.product_id
                left join product_variants v on v.id = r.product_variant_id
                where r.customer_id = :customerId
                order by r.created_at desc""".replace(":productImage", PRODUCT_IMAGE), new MapSqlParameterSource("customerId", customerId));
        Map<UUID, List<String>> imageUrls = imageUrls(rows);
        return rows.stream().map(row -> new ReviewDtos.MyReview(
                (UUID) row.get("id"), (UUID) row.get("order_id"), (String) row.get("order_code"),
                (UUID) row.get("product_id"), (String) row.get("product_name"),
                variantLabel((String) row.get("color"), (String) row.get("size")), (String) row.get("product_image"),
                ((Number) row.get("rating")).intValue(), (String) row.get("content"),
                imageUrls.getOrDefault((UUID) row.get("id"), List.of()), Boolean.TRUE.equals(row.get("hidden")),
                (String) row.get("reply"), toLocal(row.get("created_at")))).toList();
    }

    // ---------------------------------------------------------------- storefront

    public ReviewDtos.Summary summary(UUID productId) {
        Map<String, Object> row = jdbc.queryForMap("""
                select count(*) as total, avg(r.rating) as average,
                       count(*) filter (where r.rating = 5) as s5, count(*) filter (where r.rating = 4) as s4,
                       count(*) filter (where r.rating = 3) as s3, count(*) filter (where r.rating = 2) as s2,
                       count(*) filter (where r.rating = 1) as s1,
                       count(*) filter (where exists (select 1 from product_review_images i where i.review_id = r.id)) as with_images
                from product_reviews r where r.product_id = :productId and not r.hidden""",
                new MapSqlParameterSource("productId", productId));
        BigDecimal average = row.get("average") == null ? BigDecimal.ZERO
                : new BigDecimal(row.get("average").toString()).setScale(1, RoundingMode.HALF_UP);
        return new ReviewDtos.Summary(average, number(row, "total"),
                List.of(number(row, "s5"), number(row, "s4"), number(row, "s3"), number(row, "s2"), number(row, "s1")),
                number(row, "with_images"));
    }

    /** Đánh giá đang hiển thị của sản phẩm, mới nhất trước; lọc theo số sao và/hoặc chỉ đánh giá có ảnh. */
    public Page<ReviewDtos.PublicReview> productReviews(UUID productId, Integer rating, boolean withImages, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource("productId", productId);
        StringBuilder from = new StringBuilder("""
                 from product_reviews r
                join customers c on c.id = r.customer_id
                left join product_variants v on v.id = r.product_variant_id
                where r.product_id = :productId and not r.hidden""");
        if (rating != null) {
            from.append(" and r.rating = :rating");
            params.addValue("rating", rating);
        }
        if (withImages) from.append(" and exists (select 1 from product_review_images i where i.review_id = r.id)");
        Long total = jdbc.queryForObject("select count(*)" + from, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select r.id, r.rating, r.content, r.reply, r.replied_at, r.created_at, c.full_name, v.color, v.size"
                        + from + " order by r.created_at desc, r.id limit :limit offset :offset", params);
        Map<UUID, List<String>> imageUrls = imageUrls(rows);
        List<ReviewDtos.PublicReview> content = rows.stream().map(row -> new ReviewDtos.PublicReview(
                (UUID) row.get("id"), ((Number) row.get("rating")).intValue(), (String) row.get("content"),
                maskName((String) row.get("full_name")), variantLabel((String) row.get("color"), (String) row.get("size")),
                imageUrls.getOrDefault((UUID) row.get("id"), List.of()), true, (String) row.get("reply"),
                toLocal(row.get("replied_at")), toLocal(row.get("created_at")))).toList();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // ---------------------------------------------------------------- admin

    /** Mọi đánh giá (kể cả đã ẩn); {@code keyword} khớp tên/mã sản phẩm, mã đơn hoặc tên khách. */
    public Page<ReviewDtos.AdminReview> adminList(String keyword, Integer rating, Boolean hidden, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (p.name ilike :keyword or p.code ilike :keyword or o.order_code ilike :keyword"
                    + " or c.full_name ilike :keyword or r.content ilike :keyword)");
            params.addValue("keyword", "%" + keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        if (rating != null) {
            where.append(" and r.rating = :rating");
            params.addValue("rating", rating);
        }
        if (hidden != null) {
            where.append(" and r.hidden = :hidden");
            params.addValue("hidden", hidden);
        }
        return adminQuery(where.toString(), params, pageable);
    }

    private Page<ReviewDtos.AdminReview> adminQuery(String conditions, MapSqlParameterSource params, Pageable pageable) {
        String from = """
                 from product_reviews r
                join products p on p.id = r.product_id
                join orders o on o.id = r.order_id
                join customers c on c.id = r.customer_id
                left join product_variants v on v.id = r.product_variant_id
                where 1 = 1""" + conditions;
        Long total = jdbc.queryForObject("select count(*)" + from, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select r.id, r.product_id, p.name as product_name, p.code as product_code, r.order_id, o.order_code,
                       r.customer_id, c.full_name, v.color, v.size, r.rating, r.content, r.hidden, r.hidden_reason,
                       r.hidden_at, r.reply, r.replied_at, r.created_at""" + from
                + " order by r.created_at desc, r.id limit :limit offset :offset", params);
        Map<UUID, List<String>> imageUrls = imageUrls(rows);
        List<ReviewDtos.AdminReview> content = rows.stream().map(row -> new ReviewDtos.AdminReview(
                (UUID) row.get("id"), (UUID) row.get("product_id"), (String) row.get("product_name"),
                (String) row.get("product_code"), (UUID) row.get("order_id"), (String) row.get("order_code"),
                (UUID) row.get("customer_id"), (String) row.get("full_name"),
                variantLabel((String) row.get("color"), (String) row.get("size")), ((Number) row.get("rating")).intValue(),
                (String) row.get("content"), imageUrls.getOrDefault((UUID) row.get("id"), List.of()),
                Boolean.TRUE.equals(row.get("hidden")), (String) row.get("hidden_reason"), toLocal(row.get("hidden_at")),
                (String) row.get("reply"), toLocal(row.get("replied_at")), toLocal(row.get("created_at")))).toList();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Transactional
    @BusinessAudit("PRODUCT_REVIEW")
    public ReviewDtos.AdminReview setVisibility(UUID reviewId, ReviewDtos.VisibilityRequest request, UUID actorUserId) {
        ProductReview review = find(reviewId);
        LocalDateTime now = LocalDateTime.now();
        boolean hide = Boolean.TRUE.equals(request.hidden());
        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        if (hide && reason == null) throw BusinessException.badRequest("Vui lòng nhập lý do ẩn đánh giá");
        review.setHidden(hide);
        review.setHiddenReason(hide ? reason : null);
        review.setHiddenBy(hide ? actorUserId : null);
        review.setHiddenAt(hide ? now : null);
        review.setUpdatedAt(now);
        reviews.saveAndFlush(review);
        return adminRow(reviewId);
    }

    @Transactional
    @BusinessAudit("PRODUCT_REVIEW")
    public ReviewDtos.AdminReview reply(UUID reviewId, ReviewDtos.ReplyRequest request, UUID actorUserId) {
        ProductReview review = find(reviewId);
        LocalDateTime now = LocalDateTime.now();
        String text = request.reply() == null || request.reply().isBlank() ? null : request.reply().trim();
        review.setReply(text);
        review.setRepliedBy(text == null ? null : actorUserId);
        review.setRepliedAt(text == null ? null : now);
        review.setUpdatedAt(now);
        reviews.saveAndFlush(review);
        return adminRow(reviewId);
    }

    // ---------------------------------------------------------------- helpers

    private ProductReview find(UUID reviewId) {
        return reviews.findById(reviewId).orElseThrow(() -> BusinessException.notFound("Đánh giá không tồn tại"));
    }

    private ReviewDtos.AdminReview adminRow(UUID reviewId) {
        return adminQuery(" and r.id = :id", new MapSqlParameterSource("id", reviewId), Pageable.ofSize(1))
                .getContent().getFirst();
    }

    private Map<UUID, List<String>> imageUrls(List<Map<String, Object>> rows) {
        List<UUID> ids = rows.stream().map(row -> (UUID) row.get("id")).toList();
        Map<UUID, List<String>> result = new LinkedHashMap<>();
        if (ids.isEmpty()) return result;
        jdbc.query("select review_id, image_url from product_review_images where review_id in (:ids) order by review_id, sort_order",
                new MapSqlParameterSource("ids", ids), rs -> {
                    result.computeIfAbsent(rs.getObject("review_id", UUID.class), ignored -> new ArrayList<>())
                            .add(rs.getString("image_url"));
                });
        return result;
    }

    private void deleteOnRollback(String publicId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) return;
                try { storage.deleteImage(publicId); }
                catch (RuntimeException ignored) { /* best effort: the review row was never stored */ }
            }
        });
    }

    /** "Nguyễn Minh Anh" → "N*** M*** Anh": giữ tên gọi, che họ và tên đệm. */
    static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Khách hàng";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].charAt(0) + "***";
        StringBuilder masked = new StringBuilder();
        Arrays.stream(parts, 0, parts.length - 1).forEach(part -> masked.append(part.charAt(0)).append("*** "));
        return masked.append(parts[parts.length - 1]).toString();
    }

    private static String variantLabel(String color, String size) {
        List<String> parts = new ArrayList<>();
        if (color != null && !color.isBlank()) parts.add(color.trim());
        if (size != null && !size.isBlank()) parts.add(size.trim());
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private static long number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static LocalDateTime toLocal(Object value) {
        return DbTimes.fromDb(value);
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        return DbTimes.fromDb(rs.getTimestamp(column));
    }

    private static int compareDesc(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b == null ? 0 : 1;
        if (b == null) return -1;
        return b.compareTo(a);
    }
}
