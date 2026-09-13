export function getLoginRetrySeconds(error) {
  if (error?.status !== 429) return 0;
  const seconds = Number(error.retryAfterSeconds);
  return Number.isFinite(seconds) && seconds > 0 ? Math.ceil(seconds) : 0;
}

export function formatLoginLock(value) {
  const seconds = Math.max(0, Math.ceil(Number(value) || 0));
  const minutesPart = Math.floor(seconds / 60).toString().padStart(2, "0");
  const secondsPart = (seconds % 60).toString().padStart(2, "0");
  return `${minutesPart}:${secondsPart}`;
}
