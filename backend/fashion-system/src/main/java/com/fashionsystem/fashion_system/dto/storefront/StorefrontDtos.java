package com.fashionsystem.fashion_system.dto.storefront;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu sản phẩm cho cửa hàng online (khách xem, không cần đăng nhập). Giá là giá bán lúc thanh toán:
 * giá khuyến mãi niêm yết của biến thể nếu có, ngược lại giá gốc. Tồn kho là tồn online của các cửa hàng nhận đơn
 * online (đang hoạt động, có tọa độ).
 */
public final class StorefrontDtos {
    private StorefrontDtos() {
    }

    /**
     * Thẻ sản phẩm trong danh sách.
     *
     * @param minPrice         giá bán thấp nhất trong các biến thể
     * @param minOriginalPrice giá gốc thấp nhất; lớn hơn {@code minPrice} khi có biến thể đang giảm giá
     * @param inStock          còn ít nhất một biến thể có hàng online
     */
    public record ProductCard(UUID id, String slug, String code, String name, String imageUrl, String gender,
                              String brandName, BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minOriginalPrice,
                              boolean onSale, boolean inStock, List<String> tags) {
    }

    public record Category(UUID id, String name, long productCount, List<Category> children) {
    }

    public record Option(String value, String label, long productCount) {
    }

    /** Dữ liệu cho bộ lọc: cây danh mục, giới tính và khoảng giá thực tế của các sản phẩm đang bán. */
    public record Filters(List<Category> categories, List<Option> genders, BigDecimal minPrice, BigDecimal maxPrice) {
    }

    public record Ref(UUID id, String name) {
    }

    public record CollectionRef(UUID id, String name, String season, Integer year) {
    }

    public record CategoryRef(UUID id, String name, Ref parent) {
    }

    /**
     * Biến thể có thể mua.
     *
     * @param unitPrice giá bán (salePrice nếu có, ngược lại price)
     * @param available tồn online cộng dồn các cửa hàng nhận đơn online
     */
    public record Variant(UUID id, String sku, String color, String size, BigDecimal price, BigDecimal salePrice,
                          BigDecimal unitPrice, int available) {
    }

    /** Ảnh sản phẩm; {@code color} khác null khi ảnh thuộc một biến thể (để đổi ảnh theo màu). */
    public record Image(String url, String color) {
    }

    public record Attribute(String name, String value) {
    }

    /** Thương hiệu đang hợp tác; {@code productCount} chỉ đếm sản phẩm đang bán. */
    public record Brand(UUID id, String code, String name, String logo, String description, long productCount,
                        long collectionCount) {
    }

    /** Bộ sưu tập đang phát hành của thương hiệu đang hợp tác. */
    public record Collection(UUID id, String code, String name, String season, Integer year,
                             java.time.LocalDate releaseDate, String description, String imageUrl, Ref brand,
                             String brandLogo, long productCount) {
    }

    /**
     * Khuyến mãi công khai (đang áp dụng hoặc sắp diễn ra). Khách dùng bằng cách chọn/nhập mã khi thanh toán;
     * voucher đổi điểm cần đổi điểm trước.
     *
     * @param tiers     hạng thành viên được dùng (rỗng: mọi khách)
     * @param soldOut   đã dùng hết số lượt của chương trình
     * @param productCount số sản phẩm áp dụng (khuyến mãi theo sản phẩm)
     */
    public record Promotion(UUID id, String code, String name, String promotionType, String discountType,
                            BigDecimal discountValue, BigDecimal maxDiscount, BigDecimal minOrderValue,
                            java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, String status,
                            Integer pointsRequired, Integer usagePerUser, List<String> tiers, boolean soldOut,
                            long productCount) {
    }

    public record ProductDetail(UUID id, String slug, String code, String name, String description, String material,
                                String fit, String gender, Ref brand, CollectionRef collection, CategoryRef category,
                                BigDecimal minPrice, BigDecimal maxPrice, boolean inStock, List<String> colors,
                                List<String> sizes, List<Variant> variants, List<Image> images,
                                List<Attribute> attributes, List<String> tags) {
    }
}
