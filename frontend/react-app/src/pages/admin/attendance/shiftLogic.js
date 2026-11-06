import { isoDate } from "./attendanceLogic";

import "./shiftColors.css";

/** Màu ca → lớp CSS của thẻ ca (shiftColors.css, đọc được cả theme sáng / tối). */
export const SHIFT_COLORS = Object.fromEntries(
  ["amber", "sky", "violet", "emerald", "rose", "teal", "orange", "zinc"].map((c) => [c, `shift-chip shift-chip--${c}`]),
);

export const SHIFT_DOT = {
  amber: "bg-amber-400", sky: "bg-sky-400", violet: "bg-violet-400", emerald: "bg-emerald-400",
  rose: "bg-rose-400", teal: "bg-teal-400", orange: "bg-orange-400", zinc: "bg-zinc-400",
};

export const COLOR_OPTIONS = Object.keys(SHIFT_COLORS);

export const SHIFT_TYPES = [
  { value: "MORNING", label: "Ca sáng" },
  { value: "AFTERNOON", label: "Ca chiều" },
  { value: "EVENING", label: "Ca tối" },
  { value: "FULL_DAY", label: "Cả ngày" },
  { value: "NIGHT", label: "Ca đêm" },
  { value: "CUSTOM", label: "Khác" },
];

/** Đối chiếu ca với chấm công. */
export const ACTUAL_STATUS = {
  UPCOMING: { label: "Sắp tới", tone: "zinc" },
  NOT_CHECKED_IN: { label: "Chưa vào ca", tone: "amber" },
  ABSENT: { label: "Vắng", tone: "rose" },
  ON_LEAVE: { label: "Nghỉ phép", tone: "violet" },
  WORKING: { label: "Đang làm", tone: "sky" },
  ON_TIME: { label: "Đúng giờ", tone: "emerald" },
  LATE: { label: "Đi trễ", tone: "amber" },
  EARLY: { label: "Về sớm", tone: "amber" },
  LATE_EARLY: { label: "Trễ & về sớm", tone: "rose" },
  MISSING_CHECK_OUT: { label: "Thiếu check-out", tone: "rose" },
  MISSING_CHECK_IN: { label: "Thiếu check-in", tone: "rose" },
};

export const SWAP_STATUS = {
  PENDING_PEER: { label: "Chờ đồng nghiệp", tone: "sky" },
  PENDING_APPROVAL: { label: "Chờ quản lý duyệt", tone: "amber" },
  APPROVED: { label: "Đã duyệt", tone: "emerald" },
  DECLINED: { label: "Đồng nghiệp từ chối", tone: "zinc" },
  REJECTED: { label: "Quản lý từ chối", tone: "rose" },
  CANCELLED: { label: "Đã hủy", tone: "zinc" },
};

export const SWAP_TYPE = { SWAP: "Đổi ca", GIVE_AWAY: "Nhờ làm thay" };

const WEEKDAY_SHORT = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];

/** Thứ Hai của tuần chứa ngày (yyyy-MM-dd). */
export function mondayOf(value = isoDate()) {
  const d = new Date(`${value}T00:00:00`);
  const offset = (d.getDay() + 6) % 7;
  d.setDate(d.getDate() - offset);
  return isoDate(d);
}

export function addDays(value, days) {
  const d = new Date(`${value}T00:00:00`);
  d.setDate(d.getDate() + days);
  return isoDate(d);
}

export function weekDays(monday) {
  return Array.from({ length: 7 }, (_, i) => addDays(monday, i));
}

export function dayLabel(value) {
  const d = new Date(`${value}T00:00:00`);
  return { weekday: WEEKDAY_SHORT[d.getDay()], date: `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}` };
}

export const hhmm = (value) => (value ? String(value).slice(0, 5) : "");
export const timeRange = (start, end) => `${hhmm(start)}–${hhmm(end)}`;

export function hoursText(minutes) {
  if (!minutes) return "0g";
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m ? `${h}g${String(m).padStart(2, "0")}` : `${h}g`;
}

/** "HH:mm" (input time) → "HH:mm:00" cho API. */
export const toApiTime = (value) => (value ? (value.length === 5 ? `${value}:00` : value) : null);
