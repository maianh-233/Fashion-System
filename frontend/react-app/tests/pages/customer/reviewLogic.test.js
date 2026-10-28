import test from "node:test";
import assert from "node:assert/strict";
import {
  REVIEW_MAX_IMAGE_BYTES, addReviewImages, mergeReviewPage, starPercents, validateReviewForm,
} from "../../../src/pages/customer/reviewLogic.js";

const page = (ids, number, totalElements) => ({ content: ids.map((id) => ({ id })), page: { number, totalElements } });

test("load more appends without duplicates; a new filter replaces the list", () => {
  const first = mergeReviewPage({ key: "p:all", items: [] }, page(["a", "b"], 0, 4), "p:all");
  assert.deepEqual(first.items.map((r) => r.id), ["a", "b"]);
  assert.equal(first.total, 4);

  const more = mergeReviewPage(first, page(["b", "c"], 1, 4), "p:all");
  assert.deepEqual(more.items.map((r) => r.id), ["a", "b", "c"]);
  assert.equal(more.page, 1);

  const filtered = mergeReviewPage(more, page(["z"], 0, 1), "p:5");
  assert.deepEqual(filtered.items.map((r) => r.id), ["z"]);
  assert.equal(filtered.key, "p:5");
});

test("star bars are rounded shares of the total", () => {
  assert.deepEqual(starPercents({ count: 3, stars: [2, 1, 0, 0, 0] }), [67, 33, 0, 0, 0]);
  assert.deepEqual(starPercents({ count: 0, stars: [0, 0, 0, 0, 0] }), [0, 0, 0, 0, 0]);
  assert.deepEqual(starPercents(null), [0, 0, 0, 0, 0]);
});

test("form needs a rating and accepts at most 3 small images", () => {
  const jpg = (size = 1000, name = "a.jpg") => ({ name, size, type: "image/jpeg" });
  assert.deepEqual(validateReviewForm({ rating: 5, content: "Đẹp", images: [jpg()] }), {});
  assert.ok(validateReviewForm({ rating: 0 }).rating);
  assert.ok(validateReviewForm({ rating: 4, content: "x".repeat(2001) }).content);
  assert.ok(validateReviewForm({ rating: 4, images: [jpg(), jpg(), jpg(), jpg()] }).images);
  assert.ok(validateReviewForm({ rating: 4, images: [{ name: "a.pdf", size: 10, type: "application/pdf" }] }).images);
  assert.ok(validateReviewForm({ rating: 4, images: [jpg(REVIEW_MAX_IMAGE_BYTES + 1)] }).images);
});

test("picking images skips duplicates and stops at the limit", () => {
  const file = (name, size = 10) => ({ name, size, type: "image/png" });
  const picked = addReviewImages([file("a.png")], [file("a.png"), file("b.png"), file("c.png"), file("d.png")]);
  assert.deepEqual(picked.map((f) => f.name), ["a.png", "b.png", "c.png"]);
  assert.deepEqual(addReviewImages([], null), []);
});
