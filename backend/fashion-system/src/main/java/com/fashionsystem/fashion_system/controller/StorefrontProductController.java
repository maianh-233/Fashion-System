package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.StorefrontDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.service.StorefrontCatalogService;
import com.fashionsystem.fashion_system.service.StorefrontCatalogService.SearchType;
import com.fashionsystem.fashion_system.service.StorefrontCatalogService.Sort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Sản phẩm cho cửa hàng online: chỉ đọc, không cần đăng nhập. */
@RestController
@RequestMapping("/api/storefront/products")
@RequiredArgsConstructor
public class StorefrontProductController {
    private static final Set<String> GENDERS = Set.of("FEMALE", "MALE", "UNISEX");

    private final StorefrontCatalogService catalog;

    /**
     * Danh sách sản phẩm.
     *
     * @param searchType PRODUCT (tên, mã), BRAND, COLLECTION hoặc TAG
     * @param gender     FEMALE, MALE, UNISEX (nhiều giá trị)
     * @param categoryId danh mục; chọn danh mục cha thì gồm cả danh mục con (nhiều giá trị)
     * @param minPrice   lọc theo giá bán thấp nhất của sản phẩm
     * @param sort       NEWEST (mặc định), PRICE_ASC, PRICE_DESC, NAME
     */
    @GetMapping
    public Page<StorefrontDtos.ProductCard> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "PRODUCT") String searchType,
                                                 @RequestParam(required = false) List<String> gender,
                                                 @RequestParam(required = false) List<UUID> categoryId,
                                                 @RequestParam(required = false) BigDecimal minPrice,
                                                 @RequestParam(required = false) BigDecimal maxPrice,
                                                 @RequestParam(defaultValue = "false") boolean inStock,
                                                 @RequestParam(defaultValue = "NEWEST") String sort,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "12") int size,
                                                 @RequestParam(required = false) UUID brandId,
                                                 @RequestParam(required = false) UUID collectionId,
                                                 @RequestParam(required = false) UUID promotionId) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)
            throw BusinessException.badRequest("Khoảng giá không hợp lệ");
        Set<String> genders = gender == null ? Set.of() : gender.stream()
                .map(g -> g.trim().toUpperCase(Locale.ROOT)).filter(g -> !g.isEmpty()).collect(Collectors.toSet());
        if (!GENDERS.containsAll(genders)) throw BusinessException.badRequest("Giới tính không hợp lệ");
        return catalog.search(new StorefrontCatalogService.Query(keyword, parse(SearchType.class, searchType, "Kiểu tìm kiếm"),
                genders, categoryId == null ? Set.of() : Set.copyOf(categoryId), minPrice, maxPrice, inStock,
                parse(Sort.class, sort, "Kiểu sắp xếp"), page, size, brandId, collectionId, promotionId));
    }

    @GetMapping("/filters")
    public StorefrontDtos.Filters filters() {
        return catalog.filters();
    }

    /** Chi tiết theo id hoặc slug. */
    @GetMapping("/{idOrSlug}")
    public StorefrontDtos.ProductDetail detail(@PathVariable String idOrSlug) {
        return catalog.detail(idOrSlug);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String label) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            throw BusinessException.badRequest(label + " không hợp lệ");
        }
    }
}
