export const CAMPAIGN_STATUS = {
  DRAFT: { label: "Nháp", tone: "zinc" },
  PENDING_APPROVAL: { label: "Chờ duyệt", tone: "amber" },
  APPROVED: { label: "Đã duyệt", tone: "emerald" },
  REJECTED: { label: "Bị từ chối", tone: "rose" },
  CANCELLED: { label: "Đã hủy", tone: "rose" },
};

/** Giai đoạn của chiến dịch đã duyệt; backend tính từ thời gian, không lưu. */
export const CAMPAIGN_PHASE = {
  UPCOMING: { label: "Sắp diễn ra", tone: "sky" },
  RUNNING: { label: "Đang chạy", tone: "emerald" },
  ENDED: { label: "Đã kết thúc", tone: "zinc" },
};

export const OBJECTIVES = {
  REVENUE: { label: "Tăng doanh thu", hint: "Đẩy doanh số trong một khoảng thời gian" },
  ACQUISITION: { label: "Thu hút khách mới", hint: "Khách mua lần đầu" },
  RETENTION: { label: "Giữ chân khách", hint: "Kéo khách cũ quay lại mua" },
  CLEARANCE: { label: "Xả hàng tồn", hint: "Giải phóng hàng tồn kho, cuối mùa" },
  AWARENESS: { label: "Nhận diện thương hiệu", hint: "Ra mắt bộ sưu tập, sự kiện" },
};

export const OFFER_TYPES = {
  PROMOTION: { label: "Khuyến mãi", hint: "Mã giảm giá toàn đơn hoặc theo sản phẩm" },
  VOUCHER_CAMPAIGN: { label: "Chiến dịch voucher", hint: "Voucher riêng gửi cho nhóm khách" },
  FLASH_SALE: { label: "Flash sale", hint: "Giá flash trong khung giờ" },
};

export const OFFER_STATUS = {
  SCHEDULED: { label: "Sắp hiệu lực", tone: "sky" },
  ACTIVE: { label: "Đang hiệu lực", tone: "emerald" },
  RUNNING: { label: "Đang chạy", tone: "emerald" },
  SENT: { label: "Đã gửi", tone: "emerald" },
  EXPIRED: { label: "Hết hạn", tone: "zinc" },
  ENDED: { label: "Đã kết thúc", tone: "zinc" },
  INACTIVE: { label: "Đã tắt", tone: "rose" },
  CANCELLED: { label: "Đã hủy", tone: "rose" },
};

export const EXPENSE_CATEGORIES = {
  ADS: "Quảng cáo",
  KOL: "KOL / KOC",
  PRINTING: "In ấn",
  EVENT: "Sự kiện",
  OTHER: "Khác",
};

export const HISTORY_LABEL = {
  CREATED: "Tạo chiến dịch",
  UPDATED: "Sửa thông tin",
  SUBMITTED: "Gửi duyệt",
  WITHDRAWN: "Rút lại",
  APPROVED: "Duyệt",
  REJECTED: "Từ chối",
  REOPENED: "Mở lại để sửa",
  CANCELLED: "Hủy chiến dịch",
  OFFER_LINKED: "Liên kết ưu đãi",
  OFFER_UNLINKED: "Gỡ ưu đãi",
  EXPENSE_ADDED: "Thêm chi phí",
  EXPENSE_UPDATED: "Sửa chi phí",
  EXPENSE_DELETED: "Xóa chi phí",
  DISPATCH_CREATED: "Thêm đợt gửi",
  DISPATCH_UPDATED: "Sửa đợt gửi",
  DISPATCH_APPROVED: "Duyệt đợt gửi",
  DISPATCH_REJECTED: "Từ chối đợt gửi",
  DISPATCH_CANCELLED: "Hủy đợt gửi",
  DISPATCH_STARTED: "Bắt đầu gửi",
  DISPATCH_FINISHED: "Gửi xong",
  DISPATCH_RETRIED: "Gửi lại người lỗi",
};

export const DISPATCH_STATUS = {
  DRAFT: { label: "Nháp", tone: "zinc" },
  PENDING_APPROVAL: { label: "Chờ duyệt", tone: "amber" },
  SCHEDULED: { label: "Đã hẹn giờ", tone: "sky" },
  SENDING: { label: "Đang gửi", tone: "violet" },
  COMPLETED: { label: "Hoàn tất", tone: "emerald" },
  COMPLETED_WITH_ERRORS: { label: "Hoàn tất có lỗi", tone: "rose" },
  CANCELLED: { label: "Đã hủy", tone: "rose" },
};

export const RECIPIENT_STATUS = {
  PENDING: { label: "Chờ gửi", tone: "zinc" },
  SENDING: { label: "Đang gửi", tone: "violet" },
  SENT: { label: "Đã gửi", tone: "emerald" },
  FAILED: { label: "Lỗi", tone: "rose" },
  SKIPPED: { label: "Bỏ qua", tone: "amber" },
  HOLDOUT: { label: "Nhóm đối chứng", tone: "sky" },
};

export const CHANNELS = {
  IN_APP: { label: "Thông báo trong app", hint: "Khách có tài khoản online và không tắt tin ưu đãi" },
  EMAIL: { label: "Email", hint: "Chỉ khách có email và đã đồng ý nhận email ưu đãi; mỗi email có link hủy đăng ký" },
};

export const SKIP_REASONS = {
  NO_ACCOUNT: "Không có tài khoản online",
  INACTIVE: "Khách ngừng hoạt động",
  FREQUENCY_CAP: "Vừa nhận tin marketing",
  CANCELLED: "Đợt gửi bị hủy",
  CAMPAIGN_NOT_ACTIVE: "Chiến dịch không còn chạy",
  NO_EMAIL: "Không có email",
  NO_CONSENT: "Chưa đồng ý nhận email ưu đãi",
  OPTED_OUT: "Đã tắt tin ưu đãi",
};

/** Mô tả nhóm khách bằng lời từ tiêu chí đã lưu (kèm nhãn lúc tạo). */
export function describeAudience(audience) {
  if (!audience) return "";
  const parts = [];
  if (audience.tierNames?.length) parts.push(`Hạng ${audience.tierNames.join(", ")}`);
  if (audience.inactiveDays) parts.push(`chưa mua ${audience.inactiveDays} ngày`);
  if (audience.categoryName) parts.push(`từng mua ${audience.categoryName}`);
  return parts.length ? parts.join(" · ") : "Mọi khách đang hoạt động";
}

/** Phần trăm đã xử lý (gửi + bỏ qua + lỗi + nhóm đối chứng) trên số người đã chốt; null khi chưa chốt danh sách. */
export function dispatchProgress(dispatch) {
  if (!dispatch.audienceCount) return null;
  const done = dispatch.sent + dispatch.skipped + dispatch.failed + (dispatch.holdout || 0);
  return Math.min(100, Math.round((done / dispatch.audienceCount) * 100));
}

/** Đường dẫn nội bộ trang khách (khớp backend): bắt đầu bằng "/", không "//", không "/admin". */
export function isCustomerPath(value) {
  if (!value) return true;
  return value.startsWith("/") && !value.startsWith("//") && !value.startsWith("/admin") && !/\s/.test(value);
}

export function emptyDispatchForm() {
  return { tierIds: [], inactiveDays: "", categoryId: "", title: "", message: "", actionUrl: "", scheduledAt: "", channel: "IN_APP", holdoutPercent: "0" };
}

export function dispatchToForm(dispatch) {
  return {
    tierIds: dispatch.audience?.tierIds || [],
    inactiveDays: dispatch.audience?.inactiveDays ? String(dispatch.audience.inactiveDays) : "",
    categoryId: dispatch.audience?.categoryId || "",
    title: dispatch.title,
    message: dispatch.message,
    actionUrl: dispatch.actionUrl || "",
    scheduledAt: toDateTimeInput(dispatch.scheduledAt),
    channel: dispatch.channel || "IN_APP",
    holdoutPercent: String(dispatch.holdoutPercent ?? 0),
  };
}

export function dispatchCriteria(form) {
  return {
    tierIds: form.tierIds,
    inactiveDays: form.inactiveDays === "" ? null : Number(form.inactiveDays),
    categoryId: form.categoryId || null,
  };
}

/** Kiểm tra phía giao diện; backend kiểm tra lại cả khoảng giờ gửi theo thời gian chiến dịch. */
export function validateDispatchForm(form, now = new Date()) {
  if (form.title.trim().length < 3) return "Tiêu đề cần ít nhất 3 ký tự";
  if (!form.message.trim()) return "Vui lòng nhập nội dung thông báo";
  if (!isCustomerPath(form.actionUrl.trim())) return "Liên kết phải là đường dẫn nội bộ của trang khách, ví dụ /promotions";
  if (form.inactiveDays !== "") {
    const days = Number(form.inactiveDays);
    if (!Number.isInteger(days) || days < 1 || days > 1095) return "Số ngày chưa mua phải từ 1 đến 1095";
  }
  if (!CHANNELS[form.channel]) return "Vui lòng chọn kênh gửi";
  const holdout = Number(form.holdoutPercent || 0);
  if (!Number.isInteger(holdout) || holdout < 0 || holdout > 50) return "Tỷ lệ nhóm đối chứng từ 0 đến 50%";
  if (!form.scheduledAt) return "Vui lòng chọn giờ gửi";
  if (!(new Date(form.scheduledAt) > now)) return "Giờ gửi phải sau thời điểm hiện tại";
  return "";
}

export function dispatchPayload(form) {
  return {
    criteria: dispatchCriteria(form),
    title: form.title.trim(),
    message: form.message.trim(),
    actionUrl: form.actionUrl.trim() || null,
    channel: form.channel,
    scheduledAt: form.scheduledAt,
    holdoutPercent: Number(form.holdoutPercent || 0),
  };
}

export const formatDateTime = (value) => (value ? new Date(value).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" }) : "");

export const formatDate = (value) => (value ? new Date(value).toLocaleDateString("vi-VN") : "");

/** Ngày dạng yyyy-MM-dd → "dd/MM" (không phụ thuộc cách trình duyệt định dạng ngày). */
export const shortDate = (isoDate) => (isoDate ? `${isoDate.slice(8, 10)}/${isoDate.slice(5, 7)}` : "");

export function formatMoney(value) {
  if (value === null || value === undefined || value === "") return "—";
  return `${Math.round(Number(value)).toLocaleString("vi-VN")}đ`;
}

/** Khoảng thời gian dạng "01/12/2026 08:00 → 10/12/2026 22:00". */
export function formatWindow(from, to) {
  if (!from && !to) return "";
  return `${formatDateTime(from) || "…"} → ${formatDateTime(to) || "…"}`;
}

/** Giá trị từ API → giá trị ô datetime-local (bỏ phần giây). */
export const toDateTimeInput = (value) => (value ? String(value).slice(0, 16) : "");

/** Ngày theo giờ máy người dùng dạng yyyy-MM-dd, dùng cho ô chọn ngày. */
export function dateInput(date = new Date()) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** Tỷ lệ phần trăm đã dùng ngân sách (làm tròn), null khi chưa đặt ngân sách. */
export function budgetUsage(spent, budget) {
  const total = Number(budget);
  if (!budget || !Number.isFinite(total) || total <= 0) return null;
  return Math.round((Number(spent || 0) / total) * 100);
}

export function emptyCampaignForm() {
  return {
    name: "", objective: "REVENUE", description: "", startsAt: "", endsAt: "", ownerId: "",
    plannedBudget: "", targetRevenue: "", targetOrders: "", targetBuyers: "", storeId: "",
  };
}

export function campaignToForm(campaign) {
  const text = (value) => (value === null || value === undefined ? "" : String(value));
  return {
    name: campaign.name,
    objective: campaign.objective,
    description: campaign.description || "",
    startsAt: toDateTimeInput(campaign.startsAt),
    endsAt: toDateTimeInput(campaign.endsAt),
    ownerId: campaign.ownerId || "",
    plannedBudget: text(campaign.plannedBudget),
    targetRevenue: text(campaign.targetRevenue),
    targetOrders: text(campaign.targetOrders),
    targetBuyers: text(campaign.targetBuyers),
    storeId: campaign.storeId || "",
  };
}

const MAX_DAYS = 366;
const isNonNegative = (value) => value === "" || (Number.isFinite(Number(value)) && Number(value) >= 0);
const isWhole = (value) => value === "" || (Number.isInteger(Number(value)) && Number(value) >= 0);

/** Trả về thông báo lỗi đầu tiên, hoặc chuỗi rỗng khi hợp lệ (backend vẫn kiểm tra lại). */
export function validateCampaignForm(form) {
  if (form.name.trim().length < 3) return "Tên chiến dịch cần ít nhất 3 ký tự";
  if (!OBJECTIVES[form.objective]) return "Vui lòng chọn mục tiêu";
  if (!form.startsAt || !form.endsAt) return "Vui lòng chọn thời gian bắt đầu và kết thúc";
  const start = new Date(form.startsAt);
  const end = new Date(form.endsAt);
  if (!(end > start)) return "Thời gian kết thúc phải sau thời gian bắt đầu";
  if ((end - start) / 86_400_000 > MAX_DAYS) return `Một chiến dịch kéo dài tối đa ${MAX_DAYS} ngày`;
  if (!isNonNegative(form.plannedBudget) || !isNonNegative(form.targetRevenue)) return "Ngân sách và chỉ tiêu doanh thu không được âm";
  if (!isWhole(form.targetOrders) || !isWhole(form.targetBuyers)) return "Chỉ tiêu số đơn và số khách phải là số nguyên không âm";
  return "";
}

export function campaignPayload(form) {
  const number = (value) => (value === "" ? null : Number(value));
  return {
    name: form.name.trim(),
    objective: form.objective,
    description: form.description.trim() || null,
    startsAt: form.startsAt,
    endsAt: form.endsAt,
    ownerId: form.ownerId || null,
    plannedBudget: number(form.plannedBudget),
    targetRevenue: number(form.targetRevenue),
    targetOrders: number(form.targetOrders),
    targetBuyers: number(form.targetBuyers),
    storeId: form.storeId || null,
  };
}

export function emptyExpenseForm() {
  return { category: "ADS", amount: "", spentOn: dateInput(), vendor: "", note: "" };
}

export function expenseToForm(expense) {
  return {
    category: expense.category,
    amount: String(expense.amount),
    spentOn: expense.spentOn,
    vendor: expense.vendor || "",
    note: expense.note || "",
  };
}

export function validateExpenseForm(form) {
  if (!EXPENSE_CATEGORIES[form.category]) return "Vui lòng chọn loại chi phí";
  const amount = Number(form.amount);
  if (form.amount === "" || !Number.isFinite(amount) || amount <= 0) return "Số tiền phải lớn hơn 0";
  if (!form.spentOn) return "Vui lòng chọn ngày chi";
  return "";
}

export function expensePayload(form) {
  return {
    category: form.category,
    amount: Number(form.amount),
    spentOn: form.spentOn,
    vendor: form.vendor.trim() || null,
    note: form.note.trim() || null,
  };
}

/** Phần trăm đạt chỉ tiêu (làm tròn), null khi không đặt chỉ tiêu. */
export function targetProgress(actual, target) {
  const goal = Number(target);
  if (target === null || target === undefined || !Number.isFinite(goal) || goal <= 0) return null;
  return Math.round((Number(actual || 0) / goal) * 100);
}

export const formatRatio = (value) => (value === null || value === undefined ? "—" : `${Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 2 })}×`);

export function formatPercent(part, whole) {
  if (!whole) return "—";
  return `${Math.round((Number(part) / Number(whole)) * 100)}%`;
}

/** Khoảng mặc định của lịch: từ đầu tháng hiện tại tới hết tháng sau. */
export function defaultCalendarRange(today = new Date()) {
  const from = new Date(today.getFullYear(), today.getMonth(), 1);
  const to = new Date(today.getFullYear(), today.getMonth() + 2, 0);
  return { from: dateInput(from), to: dateInput(to) };
}

/**
 * Vị trí thanh của một mốc trên trục thời gian [from, to] (ngày, bao gồm cả ngày cuối), theo phần trăm; phần nằm ngoài
 * khoảng bị cắt. Mốc một thời điểm (đợt gửi) được vẽ rộng tối thiểu {@code minWidth}%.
 */
export function timelineBar(startsAt, endsAt, from, to, minWidth = 1) {
  const start = new Date(`${from}T00:00:00`).getTime();
  const end = new Date(`${to}T00:00:00`).getTime() + 86_400_000;
  const span = end - start;
  const a = Math.max(start, new Date(startsAt).getTime());
  const b = Math.min(end, new Date(endsAt).getTime());
  const width = Math.min(100, Math.max(minWidth, ((b - a) / span) * 100));
  const left = Math.max(0, Math.min(((a - start) / span) * 100, 100 - width));
  return { left, width };
}

/** Các ngày đầu tuần (thứ Hai) trong khoảng, dùng làm vạch chia trên trục thời gian. */
export function weekTicks(from, to) {
  const ticks = [];
  const day = new Date(`${from}T00:00:00`);
  const last = new Date(`${to}T00:00:00`);
  while (day <= last) {
    if (day.getDay() === 1) ticks.push(dateInput(day));
    day.setDate(day.getDate() + 1);
  }
  return ticks;
}

export const CALENDAR_KINDS = {
  CAMPAIGN: { label: "Chiến dịch", bar: "bg-amber-400/80" },
  OFFER: { label: "Ưu đãi", bar: "bg-emerald-400/70" },
  DISPATCH: { label: "Đợt gửi", bar: "bg-violet-400" },
};

/** Nhãn hiển thị một ưu đãi: "MÃ – Tên" hoặc chỉ tên (flash sale không có mã). */
export const offerLabel = (offer) => (offer.code ? `${offer.code} – ${offer.name}` : offer.name);
