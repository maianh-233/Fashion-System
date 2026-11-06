export const RECORD_STATUS = {
  COMPLETED: { label: "Đủ công", tone: "emerald" },
  WORKING: { label: "Đang làm", tone: "sky" },
  MISSING_CHECK_OUT: { label: "Thiếu check-out", tone: "amber" },
  MISSING_CHECK_IN: { label: "Thiếu check-in", tone: "rose" },
};

export const REQUEST_STATUS = {
  PENDING: { label: "Chờ duyệt", tone: "amber" },
  APPROVED: { label: "Đã duyệt", tone: "emerald" },
  REJECTED: { label: "Từ chối", tone: "rose" },
  CANCELLED: { label: "Đã hủy", tone: "zinc" },
};

export const EXCUSE_TYPES = [
  { value: "FORGOT_CHECK_IN", label: "Quên check-in" },
  { value: "FORGOT_CHECK_OUT", label: "Quên check-out" },
];

export const OVERTIME_TYPES = { WEEKDAY: "Ngày thường", WEEKEND: "Ngày nghỉ tuần", HOLIDAY: "Ngày lễ" };

export const SOURCE_LABEL = { SELF: "Tự chấm", EXCUSE: "Giải trình", ADJUSTED: "HR điều chỉnh" };

export const WEEKDAYS = [
  { value: 1, label: "Thứ 2" }, { value: 2, label: "Thứ 3" }, { value: 3, label: "Thứ 4" }, { value: 4, label: "Thứ 5" },
  { value: 5, label: "Thứ 6" }, { value: 6, label: "Thứ 7" }, { value: 7, label: "Chủ nhật" },
];

const pad = (n) => String(n).padStart(2, "0");

/** yyyy-MM-dd theo giờ máy người dùng (không dùng toISOString vì lệch UTC). */
export function isoDate(date = new Date()) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function monthRange(month) {
  const [y, m] = month.split("-").map(Number);
  const last = new Date(y, m, 0).getDate();
  return { from: `${month}-01`, to: `${month}-${pad(last)}` };
}

export const currentMonth = () => isoDate().slice(0, 7);

export const formatDate = (value) => (value ? new Date(`${String(value).slice(0, 10)}T00:00:00`).toLocaleDateString("vi-VN") : "");
export const formatTime = (value) => (value ? String(value).slice(11, 16) : "—");
export const formatDateTime = (value) => (value ? `${formatTime(value)} ${formatDate(value)}` : "");
export const shortTime = (value) => (value ? String(value).slice(0, 5) : "");

export function minutesText(minutes) {
  if (minutes == null) return "—";
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h ? `${h}g${m ? ` ${pad(m)}p` : ""}` : `${m}p`;
}

export const daysText = (value) => (value == null ? "∞" : Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 1 }));

/** datetime-local (yyyy-MM-ddTHH:mm) → LocalDateTime cho API. */
export const toLocalDateTime = (value) => (value ? `${value}:00` : null);
export const fromLocalDateTime = (value) => (value ? String(value).slice(0, 16) : "");

/** Lấy vị trí hiện tại; trả null nếu người dùng từ chối hoặc thiết bị không hỗ trợ. */
export function currentPosition(timeout = 10000) {
  return new Promise((resolve) => {
    if (!navigator.geolocation) return resolve({ position: null, error: "Thiết bị không hỗ trợ định vị" });
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ position: { latitude: Number(pos.coords.latitude.toFixed(6)), longitude: Number(pos.coords.longitude.toFixed(6)) }, error: "" }),
      (err) => resolve({ position: null, error: err.code === 1 ? "Bạn đã từ chối quyền vị trí" : "Không lấy được vị trí" }),
      { enableHighAccuracy: true, timeout, maximumAge: 30000 },
    );
  });
}
