import test from "node:test";
import assert from "node:assert/strict";
import { bubbleSide, mergeMessages, roomActions, tabForStatus } from "./supportChatLogic.js";

test("bubbles: customer left, staff right, bot and system centered", () => {
  assert.equal(bubbleSide("CUSTOMER"), "left");
  assert.equal(bubbleSide("STAFF"), "right");
  assert.equal(bubbleSide("BOT"), "center");
  assert.equal(bubbleSide("SYSTEM"), "center");
});

test("realtime messages merge without duplicates in time order", () => {
  const a = { id: "a", createdAt: "2026-10-19T08:00:00" };
  const b = { id: "b", createdAt: "2026-10-19T08:01:00" };
  assert.deepEqual(mergeMessages([b], [a, b]).map(m => m.id), ["a", "b"]);
});

test("actions depend on status and who handles the room", () => {
  assert.deepEqual(roomActions({ status: "WAITING_STAFF" }, "me"), { canClaim: true, canReply: true, canClose: true, takenByOther: false });
  const mine = roomActions({ status: "STAFF_HANDLING", assignedStaff: { id: "me" } }, "me");
  assert.equal(mine.canClaim, false);
  assert.equal(mine.canReply, true);
  const other = roomActions({ status: "STAFF_HANDLING", assignedStaff: { id: "other" } }, "me");
  assert.equal(other.canReply, false);
  assert.equal(other.takenByOther, true);
  assert.equal(roomActions({ status: "CLOSED" }, "me").canReply, false);
});

test("a room opens on the tab of its status", () => {
  assert.equal(tabForStatus("CLOSED"), "closed");
  assert.equal(tabForStatus("BOT_HANDLING"), "bot");
});
