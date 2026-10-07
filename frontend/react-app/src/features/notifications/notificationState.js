export function isNotification(value) {
  return Boolean(value
    && typeof value === "object"
    && typeof value.id === "string"
    && typeof value.eventId === "string"
    && typeof value.type === "string"
    && typeof value.title === "string"
    && typeof value.message === "string"
    && typeof value.createdAt === "string");
}

export function mergeNotifications(current = [], incoming = []) {
  const byId = new Map();
  [...current, ...incoming].forEach((notification) => {
    if (isNotification(notification)) byId.set(notification.id, notification);
  });
  return [...byId.values()].sort((left, right) => {
    const timeDifference = Date.parse(right.createdAt) - Date.parse(left.createdAt);
    return timeDifference || right.id.localeCompare(left.id);
  });
}

export function notificationFromRealtime(event) {
  if (event?.channel !== "NOTIFICATION" || event?.eventType !== "NOTIFICATION_CREATED") return null;
  return isNotification(event.payload) ? event.payload : null;
}
