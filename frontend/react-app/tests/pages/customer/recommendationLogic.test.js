import test from "node:test";
import assert from "node:assert/strict";
import { recommendationIds, splitRecommendations } from "../../../src/pages/customer/recommendationLogic.js";

test("ids are merged without duplicates, cards split back in order", () => {
  const result = { boughtTogether: ["b", "a"], similar: ["c", "a", "x"] };
  assert.deepEqual(recommendationIds(result), ["b", "a", "c", "x"]);
  const cards = [{ id: "a" }, { id: "b" }, { id: "c" }];
  const rows = splitRecommendations(result, cards);
  assert.deepEqual(rows.boughtTogether.map((p) => p.id), ["b", "a"]);
  assert.deepEqual(rows.similar.map((p) => p.id), ["c", "a"]);
  assert.deepEqual(recommendationIds(null), []);
});
