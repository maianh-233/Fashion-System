import assert from "node:assert/strict";
import test from "node:test";
import { createReconnectPolicy } from "./reconnectPolicy.js";

test("reconnect delay grows exponentially and remains capped", () => {
  const policy = createReconnectPolicy({ baseDelayMs: 500, maxDelayMs: 4_000 });

  assert.deepEqual(
    [policy.nextDelay(), policy.nextDelay(), policy.nextDelay(), policy.nextDelay(), policy.nextDelay()],
    [500, 1_000, 2_000, 4_000, 4_000],
  );
});

test("successful connection resets the backoff", () => {
  const policy = createReconnectPolicy({ baseDelayMs: 500, maxDelayMs: 4_000 });
  policy.nextDelay();
  policy.nextDelay();

  policy.reset();

  assert.equal(policy.nextDelay(), 500);
});
