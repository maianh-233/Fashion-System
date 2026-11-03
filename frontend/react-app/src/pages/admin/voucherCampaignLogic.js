export const CAMPAIGN_STATUS = {
  SCHEDULED: { label: "Đã hẹn gửi", style: "border-sky-400/30 bg-sky-500/10 text-sky-200" },
  SENT: { label: "Đã gửi", style: "border-emerald-400/30 bg-emerald-500/10 text-emerald-200" },
  CANCELLED: { label: "Đã hủy", style: "border-zinc-600 bg-zinc-800 text-zinc-400" },
};

export const emptyCampaignForm = () => ({
  name: "", tierIds: [], useInactive: false, inactiveDays: "90", categoryId: "", skipRecent: true,
  discountType: "PERCENT", discountValue: "10", maxDiscount: "", minOrderValue: "", validDays: "14",
  message: "", sendMode: "now", scheduledAt: "",
});

/** Tiêu chí nhóm khách gửi lên API (dùng cho cả xem trước và tạo). */
export function criteriaPayload(form) {
  return {
    tierIds: form.tierIds,
    inactiveDays: form.useInactive ? Number(form.inactiveDays) : null,
    categoryId: form.categoryId || null,
    skipRecent: form.skipRecent,
  };
}

const numberOrNull = (value) => (value === "" || value == null ? null : Number(value));

export function campaignPayload(form) {
  return {
    name: form.name.trim(),
    criteria: criteriaPayload(form),
    discountType: form.discountType,
    discountValue: Number(form.discountValue),
    maxDiscount: form.discountType === "PERCENT" ? numberOrNull(form.maxDiscount) : null,
    minOrderValue: numberOrNull(form.minOrderValue),
    validDays: Number(form.validDays),
    message: form.message.trim() || null,
    scheduledAt: form.sendMode === "schedule" ? form.scheduledAt : null,
  };
}

/** Lỗi đầu tiên của form, hoặc "" nếu hợp lệ. */
export function validateCampaignForm(form, now = new Date()) {
  if (!form.name.trim()) return "Vui lòng nhập tên chiến dịch.";
  if (form.useInactive && !(Number.isInteger(Number(form.inactiveDays)) && Number(form.inactiveDays) >= 1))
    return "Số ngày chưa mua phải là số nguyên từ 1.";
  const value = Number(form.discountValue);
  if (!(value > 0)) return "Mức giảm phải lớn hơn 0.";
  if (form.discountType === "PERCENT" && value > 100) return "Phần trăm giảm không được vượt quá 100.";
  if (form.maxDiscount !== "" && !(Number(form.maxDiscount) > 0)) return "Giảm tối đa phải lớn hơn 0.";
  if (form.minOrderValue !== "" && !(Number(form.minOrderValue) >= 0)) return "Đơn tối thiểu không được âm.";
  const days = Number(form.validDays);
  if (!(Number.isInteger(days) && days >= 1 && days <= 365)) return "Số ngày được dùng phải từ 1 đến 365.";
  if (form.sendMode === "schedule") {
    if (!form.scheduledAt) return "Vui lòng chọn giờ gửi.";
    if (new Date(form.scheduledAt) <= now) return "Giờ gửi phải ở tương lai.";
  }
  return "";
}

const money = (value) => `${Number(value || 0).toLocaleString("vi-VN")}đ`;

/** "Giảm 15% (tối đa 100.000đ) · đơn từ 300.000đ · dùng 14 ngày" */
export function benefitText(campaign) {
  let text = campaign.discountType === "PERCENT"
    ? `Giảm ${Number(campaign.discountValue).toLocaleString("vi-VN")}%${campaign.maxDiscount ? ` (tối đa ${money(campaign.maxDiscount)})` : ""}`
    : `Giảm ${money(campaign.discountValue)}`;
  if (Number(campaign.minOrderValue) > 0) text += ` · đơn từ ${money(campaign.minOrderValue)}`;
  return `${text} · dùng ${campaign.validDays} ngày`;
}

/** Mô tả nhóm khách của chiến dịch đã lưu. */
export function audienceText(campaign) {
  const parts = [];
  if (campaign.tierNames?.length) parts.push(`Hạng ${campaign.tierNames.join(", ")}`);
  if (campaign.inactiveDays) parts.push(`${campaign.inactiveDays} ngày chưa mua`);
  if (campaign.categoryName) parts.push(`Từng mua ${campaign.categoryName}`);
  if (!parts.length) parts.push("Mọi khách");
  if (campaign.skipRecentDays) parts.push(`bỏ qua khách nhận voucher trong ${campaign.skipRecentDays} ngày`);
  return parts.join(" · ");
}

/** Tỷ lệ dùng voucher, ví dụ "12 / 40 (30%)". */
export function usageText(campaign) {
  if (campaign.recipientCount == null) return "—";
  const rate = campaign.recipientCount ? Math.round((campaign.usedCount / campaign.recipientCount) * 100) : 0;
  return `${campaign.usedCount.toLocaleString("vi-VN")} / ${campaign.recipientCount.toLocaleString("vi-VN")} (${rate}%)`;
}
