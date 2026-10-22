// Logic thuần của màn hình hỗ trợ đơn hàng.

export const SUPPORT_TABS = [
  { key: "waiting", label: "Chờ nhân viên", statuses: ["WAITING_STAFF"] },
  { key: "handling", label: "Đang hỗ trợ", statuses: ["STAFF_HANDLING"] },
  { key: "bot", label: "Bot đang hỗ trợ", statuses: ["BOT_HANDLING"] },
  { key: "closed", label: "Đã kết thúc", statuses: ["CLOSED"] },
];

export const STATUS_STYLES = {
  WAITING_STAFF: "bg-amber-500/20 text-amber-300",
  STAFF_HANDLING: "bg-emerald-500/20 text-emerald-400",
  BOT_HANDLING: "bg-sky-500/20 text-sky-300",
  CLOSED: "bg-zinc-700 text-zinc-300",
};

/** Khách bên trái, nhân viên bên phải, bot và thông báo hệ thống ở giữa. */
export function bubbleSide(senderType) {
  if (senderType === "STAFF") return "right";
  if (senderType === "CUSTOMER") return "left";
  return "center";
}

/** Gộp tin realtime vào danh sách hiện có, bỏ trùng theo id và giữ thứ tự thời gian. */
export function mergeMessages(current = [], incoming = []) {
  const byId = new Map(current.map(message => [message.id, message]));
  incoming.forEach(message => { if (message?.id) byId.set(message.id, message); });
  return [...byId.values()].sort((a, b) => Date.parse(a.createdAt) - Date.parse(b.createdAt) || a.id.localeCompare(b.id));
}

/** Thao tác nhân viên hiện tại được làm với hội thoại. */
export function roomActions(room, currentUserId) {
  if (!room) return { canClaim: false, canReply: false, canClose: false };
  const mine = room.assignedStaff?.id === currentUserId;
  const takenByOther = Boolean(room.status === "STAFF_HANDLING" && room.assignedStaff && !mine);
  const open = room.status !== "CLOSED";
  return {
    canClaim: open && room.status !== "STAFF_HANDLING",
    canReply: open && !takenByOther,
    canClose: open && !takenByOther,
    takenByOther,
  };
}

export function tabForStatus(status) {
  return SUPPORT_TABS.find(tab => tab.statuses.includes(status))?.key || "waiting";
}
