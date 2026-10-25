import test from "node:test";
import assert from "node:assert/strict";
import { FALLBACK_SLIDE, featuredCategories, heroSlidesFrom, monogram } from "../../../src/pages/customer/homeLogic.js";

test("hero slides come from collections with products, newest first as given", () => {
  const slides = heroSlidesFrom([
    { id: "a", name: "Winter Edit", season: "Winter", year: 2026, brand: { name: "Lunaria" }, productCount: 12, description: "", imageUrl: null },
    { id: "b", name: "Empty", productCount: 0 },
    { id: "c", name: "Spring", season: null, year: null, brand: null, productCount: 3, description: " Nhẹ nhàng ", imageUrl: "https://x/y.jpg" },
  ]);
  assert.deepEqual(slides.map(slide => slide.id), ["a", "c"]);
  assert.equal(slides[0].eyebrow, "Winter · 2026 · Lunaria");
  assert.equal(slides[0].description, "12 sản phẩm từ Lunaria.");
  assert.equal(slides[0].image, null);
  assert.equal(slides[0].link, "/collectiondetail?id=a");
  assert.equal(slides[1].eyebrow, "Bộ sưu tập");
  assert.equal(slides[1].description, "Nhẹ nhàng");
  assert.equal(slides[1].image, "https://x/y.jpg");
});

test("hero falls back to a single products slide and respects the limit", () => {
  assert.deepEqual(heroSlidesFrom([]), [FALLBACK_SLIDE]);
  assert.deepEqual(heroSlidesFrom(undefined), [FALLBACK_SLIDE]);
  const many = Array.from({ length: 6 }, (_, i) => ({ id: String(i), name: `C${i}`, productCount: 1 }));
  assert.equal(heroSlidesFrom(many).length, 4);
});

test("featured categories sort by product count and link to filtered products", () => {
  const result = featuredCategories({
    categories: [
      { id: "ao", name: "Áo", productCount: 60, children: [
        { id: "t", name: "Áo thun", productCount: 15 }, { id: "s", name: "Áo sơ mi", productCount: 30 },
        { id: "b", name: "Blazer", productCount: 10 }, { id: "h", name: "Hoodie", productCount: 5 },
      ] },
      { id: "quan", name: "Quần", productCount: 1200, children: [] },
      { id: "x", name: "Rỗng", productCount: 0, children: [] },
    ],
  });
  assert.deepEqual(result.map(category => category.id), ["quan", "ao"]);
  assert.equal(result[0].countText, "1.200 sản phẩm");
  assert.equal(result[1].link, "/products?categoryId=ao");
  assert.deepEqual(result[1].children.map(child => child.name), ["Áo sơ mi", "Áo thun", "Blazer"]);
  assert.equal(result[1].moreChildren, 1);
  assert.deepEqual(featuredCategories(null), []);
});

test("monogram replaces the missing category image", () => {
  assert.equal(monogram("Váy & Đầm"), "VĐ");
  assert.equal(monogram("Áo khoác"), "ÁK");
  assert.equal(monogram("Áo"), "Áo");
  assert.equal(monogram(""), "");
});
