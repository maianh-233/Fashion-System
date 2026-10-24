import test from "node:test";
import assert from "node:assert/strict";
import { MAX_WISHLIST, orderCards, pageIds, sanitizeIds, toggleId } from "../../../src/pages/customer/wishlistLogic.js";

const id = (n) => `00000000-0000-4000-8000-${String(n).padStart(12, "0")}`;

test("stored ids are validated and deduplicated", () => {
  assert.deepEqual(sanitizeIds([id(1), id(1), "x", 5, null, id(2)]), [id(1), id(2)]);
  assert.deepEqual(sanitizeIds("nope"), []);
  assert.equal(sanitizeIds(Array.from({ length: 250 }, (_, i) => id(i))).length, MAX_WISHLIST);
});

test("toggle adds on top and removes", () => {
  assert.deepEqual(toggleId([id(1)], id(2)), [id(2), id(1)]);
  assert.deepEqual(toggleId([id(1), id(2)], id(1)), [id(2)]);
  const full = Array.from({ length: MAX_WISHLIST }, (_, i) => id(i));
  assert.throws(() => toggleId(full, id(999)), /tối đa/);
  assert.equal(toggleId(full, id(0)).length, MAX_WISHLIST - 1);
});

test("pages keep wishlist order", () => {
  const ids = [id(1), id(2), id(3)];
  assert.deepEqual(pageIds(ids, 2, 2), [id(3)]);
  assert.deepEqual(orderCards([{ id: id(3) }, { id: id(1) }], ids).map((c) => c.id), [id(1), id(3)]);
});
