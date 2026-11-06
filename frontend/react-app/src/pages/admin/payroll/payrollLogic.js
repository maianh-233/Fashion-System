export const PERIOD_STATUS = {
  DRAFT: { label: "Nháp", tone: "zinc" },
  CALCULATED: { label: "Đã tính", tone: "sky" },
  PENDING_APPROVAL: { label: "Chờ duyệt", tone: "amber" },
  APPROVED: { label: "Đã chốt", tone: "violet" },
  PAID: { label: "Đã thanh toán", tone: "emerald" },
  CANCELLED: { label: "Đã hủy", tone: "rose" },
};

export const REVIEW_STATUS = {
  OK: { label: "Đủ dữ liệu", tone: "emerald" },
  NEEDS_REVIEW: { label: "Cần xem xét", tone: "amber" },
  CONFIRMED: { label: "HR đã xác nhận", tone: "sky" },
};

export const PAYMENT_STATUS = {
  UNPAID: { label: "Chưa trả", tone: "zinc" },
  PAID: { label: "Đã trả", tone: "emerald" },
};

export const KIND_LABEL = { ALLOWANCE: "Phụ cấp", BONUS: "Thưởng", DEDUCTION: "Khấu trừ", OVERTIME: "Tăng ca" };

export const METHODS = [
  { value: "FIXED", label: "Cố định / tháng" },
  { value: "PRORATED", label: "Theo tỷ lệ ngày công" },
  { value: "PER_WORKED_DAY", label: "× ngày đi làm thực tế" },
  { value: "PERCENT_OF_BASE", label: "% lương cơ bản" },
  { value: "PER_LATE_EARLY", label: "× số lần trễ / về sớm" },
  { value: "PERFECT_ATTENDANCE", label: "Khi đạt chuyên cần" },
  { value: "MANUAL", label: "Nhập theo kỳ" },
];

export const SOURCE_LABEL = { STRUCTURE: "Hồ sơ lương", SYSTEM: "Tăng ca", MANUAL: "HR nhập", ADJUSTMENT: "Điều chỉnh kỳ trước" };

export const ACTION_LABEL = {
  CREATE: "Tạo kỳ lương", SYNC_ATTENDANCE: "Đồng bộ công", CALCULATE: "Tính lương", SUBMIT: "Gửi duyệt", APPROVE: "Phê duyệt & chốt",
  REJECT: "Từ chối", CANCEL: "Hủy kỳ", PAY: "Xác nhận thanh toán",
  ADD_ITEM: "Thêm khoản", REMOVE_ITEM: "Bỏ khoản", CONFIRM_DAYS: "Xác nhận ngày công", CORRECTION: "Điều chỉnh sau chốt",
  NEW_VERSION: "Phiên bản mới", UPDATE: "Sửa phiên bản", CLOSE: "Đóng phiên bản",
};

export const money = (value) => (value == null || value === "" ? "—" : `${Number(value).toLocaleString("vi-VN")} ₫`);
export const num = (value) => (value == null ? "0" : Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 2 }));
export const monthLabel = (value) => (value ? `${String(value).slice(5, 7)}/${String(value).slice(0, 4)}` : "");
export const methodLabel = (value) => METHODS.find((m) => m.value === value)?.label || value;
