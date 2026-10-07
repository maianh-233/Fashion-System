export function createReconnectPolicy({ baseDelayMs = 1_000, maxDelayMs = 30_000 } = {}) {
  if (baseDelayMs <= 0 || maxDelayMs < baseDelayMs) {
    throw new Error("Invalid realtime reconnect policy");
  }
  let attempt = 0;

  return {
    nextDelay() {
      const delay = Math.min(maxDelayMs, baseDelayMs * (2 ** attempt));
      attempt += 1;
      return delay;
    },
    reset() {
      attempt = 0;
    },
  };
}
