import test from "node:test";
import assert from "node:assert/strict";
import { ALL_STORES, NO_STORE, selectionToStoreFilter } from "./storeSelectorLogic.js";

test("maps semantic store selections without numeric magic values", () => {
  assert.deepEqual(selectionToStoreFilter(ALL_STORES), {});
  assert.deepEqual(selectionToStoreFilter(NO_STORE), { noStore: true });
  assert.deepEqual(selectionToStoreFilter("store-1"), { storeId: "store-1" });
});
