package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.StorefrontDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.service.StorefrontBrowseService;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thương hiệu, bộ sưu tập, khuyến mãi cho cửa hàng online: chỉ đọc, không cần đăng nhập. */
@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
public class StorefrontBrowseController {
    private static final Set<String> PROMOTION_TYPES = Set.of("ORDER", "PRODUCT", "POINT_REDEMPTION", "BIRTHDAY");
    private static final Set<String> DISCOUNT_TYPES = Set.of("PERCENT", "FIXED");

    private final StorefrontBrowseService browse;

    @GetMapping("/brands")
    public Page<StorefrontDtos.Brand> brands(@RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "12") int size) {
        return browse.brands(keyword, page, size);
    }

    @GetMapping("/brands/{id}")
    public StorefrontDtos.Brand brand(@PathVariable UUID id) {
        return browse.brand(id);
    }

    @GetMapping("/collections")
    public Page<StorefrontDtos.Collection> collections(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) UUID brandId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "12") int size) {
        return browse.collections(keyword, brandId, page, size);
    }

    @GetMapping("/collections/{id}")
    public StorefrontDtos.Collection collection(@PathVariable UUID id) {
        return browse.collection(id);
    }

    /**
     * Khuyến mãi đang áp dụng hoặc sắp diễn ra.
     *
     * @param type         ORDER (giảm trên đơn), PRODUCT (giảm theo sản phẩm), POINT_REDEMPTION (đổi điểm), BIRTHDAY (quà sinh nhật)
     * @param discountType PERCENT hoặc FIXED
     * @param from         ngày bắt đầu khoảng thời gian khách quan tâm (yyyy-MM-dd)
     * @param sort         ENDING_SOON (mặc định) hoặc NEWEST
     */
    @GetMapping("/promotions")
    public Page<StorefrontDtos.Promotion> promotions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "ENDING_SOON") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        if (from != null && to != null && from.isAfter(to)) throw BusinessException.badRequest("Khoảng ngày không hợp lệ");
        StorefrontBrowseService.PromotionSort order;
        try {
            order = StorefrontBrowseService.PromotionSort.valueOf(sort.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            throw BusinessException.badRequest("Kiểu sắp xếp không hợp lệ");
        }
        return browse.promotions(keyword, oneOf(type, PROMOTION_TYPES, "Loại khuyến mãi"),
                oneOf(discountType, DISCOUNT_TYPES, "Kiểu giảm giá"), from, to, order, page, size);
    }

    private static String oneOf(String value, Set<String> allowed, String label) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw BusinessException.badRequest(label + " không hợp lệ");
        return normalized;
    }
}
