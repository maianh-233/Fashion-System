import test from "node:test";
import assert from "node:assert/strict";
import { EMPTY_REVIEW_FILTERS, normalizeReviewPage, reviewQuery } from "../../../src/pages/admin/reviewManagementLogic.js";

test("filters map to API params with zero-based pages", () => {
  assert.deepEqual(reviewQuery(EMPTY_REVIEW_FILTERS, 1, 20),
    { keyword: undefined, rating: undefined, hidden: undefined, page: 0, size: 20 });
  assert.deepEqual(reviewQuery({ keyword: "  ao len ", rating: "2", visibility: "hidden" }, 3, 20),
    { keyword: "ao len", rating: 2, hidden: true, page: 2, size: 20 });
  assert.equal(reviewQuery({ ...EMPTY_REVIEW_FILTERS, visibility: "visible" }, 1, 20).hidden, false);
});

test("Spring page is normalized for the table", () => {
  assert.deepEqual(normalizeReviewPage({ content: [{ id: "r" }], page: { totalPages: 3, totalElements: 41 } }),
    { rows: [{ id: "r" }], totalPages: 3, totalElements: 41 });
  assert.deepEqual(normalizeReviewPage(null), { rows: [], totalPages: 1, totalElements: 0 });
});
