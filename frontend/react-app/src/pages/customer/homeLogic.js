// Dữ liệu trang chủ dựng từ API storefront: banner theo bộ sưu tập đang phát hành, danh mục theo cây danh mục.

export const HERO_LIMIT = 4;
export const CATEGORY_LIMIT = 8;

/** Slide mặc định khi chưa có bộ sưu tập nào phát hành. */
export const FALLBACK_SLIDE = {
  id: "all-products",
  eyebrow: "Lunaria",
  title: "Thời trang cho mọi ngày",
  description: "Khám phá các thiết kế đang bán, lọc theo danh mục, thương hiệu và khoảng giá.",
  image: null,
  link: "/products",
  cta: "Xem sản phẩm",
};

const productCountText = count => `${Number(count || 0).toLocaleString("vi-VN")} sản phẩm`;

/** Banner từ bộ sưu tập mới nhất có sản phẩm đang bán; luôn trả ít nhất một slide. */
export function heroSlidesFrom(collections = [], limit = HERO_LIMIT) {
  const slides = collections
    .filter(collection => Number(collection.productCount || 0) > 0)
    .slice(0, limit)
    .map(collection => ({
      id: collection.id,
      eyebrow: [collection.season, collection.year, collection.brand?.name].filter(Boolean).join(" · ") || "Bộ sưu tập",
      title: collection.name,
      description: collection.description?.trim()
        || `${productCountText(collection.productCount)}${collection.brand?.name ? ` từ ${collection.brand.name}` : ""}.`,
      image: collection.imageUrl || null,
      link: `/collectiondetail?id=${encodeURIComponent(collection.id)}`,
      cta: "Xem bộ sưu tập",
    }));
  return slides.length > 0 ? slides : [FALLBACK_SLIDE];
}

/** Danh mục gốc nhiều sản phẩm nhất, kèm vài danh mục con nổi bật; link mở trang sản phẩm đã lọc. */
export function featuredCategories(filters, limit = CATEGORY_LIMIT, childLimit = 3) {
  return [...(filters?.categories || [])]
    .filter(category => Number(category.productCount || 0) > 0)
    .sort((a, b) => Number(b.productCount) - Number(a.productCount) || a.name.localeCompare(b.name, "vi"))
    .slice(0, limit)
    .map(category => ({
      id: category.id,
      name: category.name,
      countText: productCountText(category.productCount),
      link: `/products?categoryId=${encodeURIComponent(category.id)}`,
      children: [...(category.children || [])]
        .sort((a, b) => Number(b.productCount) - Number(a.productCount))
        .slice(0, childLimit)
        .map(child => ({ id: child.id, name: child.name, link: `/products?categoryId=${encodeURIComponent(child.id)}` })),
      moreChildren: Math.max(0, (category.children || []).length - childLimit),
    }));
}

/** Chữ cái đầu dùng làm điểm nhấn thay cho ảnh: "Váy & Đầm" → "VĐ", "Áo" → "Áo". */
export function monogram(name = "") {
  const words = name.split(/[\s&/,-]+/).filter(Boolean);
  if (words.length <= 1) return name.slice(0, 2);
  return words.slice(0, 2).map(word => word[0].toUpperCase()).join("");
}
