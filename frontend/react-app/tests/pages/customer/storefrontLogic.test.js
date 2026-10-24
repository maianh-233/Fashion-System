import test from "node:test";
import assert from "node:assert/strict";
import {
  buildProductQuery, colorSwatch, galleryImages, initialSelection, priceBoundsOf, productImage, selectColor, sizeStates,
  stockLabel, tagLabel,
} from "../../../src/pages/customer/storefrontLogic.js";

const variants = [
  { id: "1", color: "Đen", size: "M", available: 0 },
  { id: "2", color: "Đen", size: "L", available: 3 },
  { id: "3", color: "Be", size: "M", available: 10 },
];

test("query sends only active filters and zero-based page", () => {
  const q = buildProductQuery({ search: " jeans ", searchType: "brand", genders: ["FEMALE"], categoryIds: ["c1", "c2"],
    priceRange: [200000, 1050000], priceBounds: [200000, 1050000], inStockOnly: true, sort: "PRICE_ASC", page: 3 });
  assert.equal(q.get("keyword"), "jeans");
  assert.equal(q.get("searchType"), "BRAND");
  assert.deepEqual(q.getAll("categoryId"), ["c1", "c2"]);
  assert.equal(q.get("minPrice"), null);
  assert.equal(q.get("maxPrice"), null);
  assert.equal(q.get("inStock"), "true");
  assert.equal(q.get("page"), "2");

  const priced = buildProductQuery({ priceRange: [300000, 800000], priceBounds: [200000, 1050000] });
  assert.equal(priced.get("minPrice"), "300000");
  assert.equal(priced.get("maxPrice"), "800000");
  assert.equal(priced.get("keyword"), null);
  assert.equal(priced.get("sort"), null);
});

test("price bounds round outward to the slider step", () => {
  assert.deepEqual(priceBoundsOf({ minPrice: 209000, maxPrice: 1009000 }), [200000, 1050000]);
  assert.deepEqual(priceBoundsOf(null), [0, 5000000]);
});

test("selection starts on an in-stock variant and keeps the size when switching colour if possible", () => {
  assert.deepEqual(initialSelection(variants), { color: "Đen", size: "L" });
  assert.deepEqual(selectColor(variants, ["M", "L"], "Be", "L"), { color: "Be", size: "M" });
  assert.deepEqual(selectColor(variants, ["M", "L"], "Đen", "L"), { color: "Đen", size: "L" });
  assert.deepEqual(sizeStates(variants, ["M", "L"], "Be"), [
    { size: "M", exists: true, available: 10 }, { size: "L", exists: false, available: 0 }]);
});

test("customer-facing labels", () => {
  assert.equal(stockLabel(variants[0]), "Tạm hết hàng");
  assert.equal(stockLabel(variants[1]), "Chỉ còn 3 sản phẩm");
  assert.equal(stockLabel(variants[2]), "Còn hàng");
  assert.equal(tagLabel("DEMO Best Seller"), "Best Seller");
  assert.equal(colorSwatch("Xanh rêu"), "#5b6b3f");
  assert.equal(colorSwatch("Lạ"), "#9a9a9f");
});

test("images fall back to a generated placeholder and prefer the chosen colour", () => {
  assert.match(productImage(null, "Sơ mi Premium"), /^data:image\/svg\+xml/);
  assert.equal(productImage("https://cdn/x.jpg", "x"), "https://cdn/x.jpg");
  const images = [{ url: "a", color: "Be" }, { url: "b", color: null }, { url: "c", color: "Đen" }];
  assert.deepEqual(galleryImages(images, "Đen", "x"), ["c", "b", "a"]);
  assert.equal(galleryImages([], "Đen", "x").length, 1);
});

test("collection and promotion labels", async () => {
  const { seasonLabel, promotionValueLabel, promotionConditions, promotionStatus } = await import("../../../src/pages/customer/storefrontLogic.js");
  assert.equal(seasonLabel("AUTUMN", 2026), "Thu · 2026");
  assert.equal(seasonLabel(null, 2026), "2026");
  assert.equal(promotionValueLabel({ discountType: "PERCENT", discountValue: 10, maxDiscount: 100000 }), "Giảm 10% (tối đa 100.000đ)");
  assert.equal(promotionValueLabel({ discountType: "FIXED", discountValue: 30000 }), "Giảm 30.000đ");
  assert.deepEqual(promotionConditions({ promotionType: "POINT_REDEMPTION", pointsRequired: 200, minOrderValue: 300000, tiers: [] }),
    ["Đổi 200 điểm tích lũy để nhận voucher", "Áp dụng cho đơn từ 300.000đ"]);
  assert.equal(promotionStatus({ status: "SCHEDULED" }).label, "Sắp diễn ra");
  assert.equal(promotionStatus({ status: "ACTIVE", soldOut: true }).label, "Đã hết lượt");
});
