export const RECEIPT_STATUSES = { DRAFT: "Nháp", PENDING_CONFIRMATION: "Chờ xác nhận", CONFIRMED: "Đã xác nhận", COMPLETED: "Hoàn thành", CANCELLED: "Đã hủy" };
export const ISSUE_TYPES = { ONLINE_TO_OFFLINE: "Online → Offline", OFFLINE_TO_ONLINE: "Offline → Online", DAMAGED: "Hàng hỏng", RETURN_TO_SUPPLIER: "Trả nhà cung cấp", OTHER: "Xuất khác" };
export const fixedSource = (type) => type === "ONLINE_TO_OFFLINE" ? "ONLINE" : type === "OFFLINE_TO_ONLINE" ? "OFFLINE" : null;
export const receiptDateParams = (fromDate, toDate) => ({
  ...(fromDate ? { fromDate } : {}),
  ...(toDate ? { toDate } : {}),
});
export const isReceiptEditable = status => ["DRAFT", "PENDING_CONFIRMATION"].includes(status);
export function receiptActions(status, kind, hasPermission) {
  const prefix = kind === "import" ? "IMPORT_RECEIPT" : "EXPORT_RECEIPT";
  return [
    ["submit", "Gửi xác nhận", "UPDATE", status === "DRAFT"],
    ["confirm", "Xác nhận", "CONFIRM", status === "PENDING_CONFIRMATION"],
    ["complete", "Hoàn tất kho", "COMPLETE", status === "CONFIRMED"],
    ["cancel", "Hủy phiếu", "CANCEL", ["DRAFT", "PENDING_CONFIRMATION"].includes(status)],
  ].filter(([, , permission, allowed]) => allowed && hasPermission(`${prefix}_${permission}`));
}
export function validateHeader(header, kind) {
  if (!header.storeId) return "Vui lòng chọn cửa hàng.";
  if ((kind === "import" || header.issueType === "RETURN_TO_SUPPLIER") && !header.supplierId) return "Vui lòng chọn nhà cung cấp.";
  if (kind === "export" && !ISSUE_TYPES[header.issueType]) return "Vui lòng chọn loại xuất.";
  if (kind === "export" && header.issueType === "OTHER" && !header.reason?.trim()) return "Vui lòng nhập lý do xuất khác.";
  return "";
}
export function validateItem(item, kind, items = [], balance = null, issueType = "") {
  if (!item.productId || !item.productVariantId) return "Vui lòng chọn sản phẩm và biến thể.";
  if (!Number.isSafeInteger(Number(item.quantity)) || Number(item.quantity) <= 0 || Number(item.quantity) > 2147483647) return "Số lượng phải là số nguyên từ 1 đến 2147483647.";
  if (kind === "import" && (item.costPrice === "" || !Number.isFinite(Number(item.costPrice)) || Number(item.costPrice) < 0)) return "Giá nhập phải lớn hơn hoặc bằng 0.";
  if (kind === "import" && !/^\d+(\.\d{1,2})?$/.test(String(item.costPrice))) return "Giá nhập tối đa 2 chữ số thập phân.";
  const otherItems = items.filter(row => !item.id || row.id !== item.id);
  if (otherItems.reduce((sum, row) => sum + Number(row.quantity), Number(item.quantity)) > 2147483647) return "Tổng số lượng phiếu vượt giới hạn 2147483647.";
  if (kind === "import") {
    const cents = value => { const [whole, fraction = ""] = String(value).split("."); return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, "0")); };
    const cost = cents(item.costPrice);
    if (cost > 999999999999n) return "Giá nhập tối đa 9.999.999.999,99 ₫.";
    const total = otherItems.reduce((sum, row) => sum + cents(row.costPrice) * BigInt(Number(row.quantity)), cost * BigInt(Number(item.quantity)));
    if (total > 99999999999999n) return "Tổng tiền phiếu vượt giới hạn 999.999.999.999,99 ₫.";
  }
  const channel = kind === "import" ? item.targetChannel : item.sourceChannel;
  if (!["ONLINE", "OFFLINE"].includes(channel)) return "Vui lòng chọn kênh kho.";
  if (kind === "export") {
    if (fixedSource(issueType) && channel !== fixedSource(issueType)) return "Kênh xuất không phù hợp với loại chuyển kho.";
    if (!balance) return "Chưa tải được tồn kho khả dụng. Vui lòng thử lại.";
    const others = items.filter((row) => row.id !== item.id && row.productVariantId === item.productVariantId && row.sourceChannel === channel).reduce((sum, row) => sum + Number(row.quantity), 0);
    const available = Number(channel === "ONLINE" ? balance.onlineQuantity : balance.offlineQuantity);
    if (!Number.isFinite(available) || Number(item.quantity) + others > available) return `Vượt tồn ${channel}: khả dụng ${available || 0}, các dòng khác đã dùng ${others}.`;
  }
  return "";
}
export function headerPayload(header, kind) {
  return { storeId: header.storeId, supplierId: header.supplierId || null, note: header.note || "", ...(kind === "export" ? { issueType: header.issueType, reason: header.reason || "" } : {}) };
}
