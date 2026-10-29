// Logic thuần của trình sửa bảng size theo danh mục.

let nextKey = 0;
const key = () => `row-${++nextKey}`;
const text = (value) => (value === null || value === undefined ? "" : String(value));

export const emptyChartRow = () => ({ key: key(), size: "", minHeightCm: "", maxHeightCm: "", minWeightKg: "", maxWeightKg: "" });

/** Bảng mẫu cho áo, để admin sửa lại thay vì nhập từ đầu. */
export const SIZE_CHART_PRESET = [
  ["S", 150, 160, 42, 50], ["M", 160, 167, 50, 58], ["L", 167, 174, 58, 66], ["XL", 174, 182, 66, 76],
].map(([size, minHeightCm, maxHeightCm, minWeightKg, maxWeightKg]) => ({
  key: key(), size, minHeightCm: String(minHeightCm), maxHeightCm: String(maxHeightCm),
  minWeightKg: String(minWeightKg), maxWeightKg: String(maxWeightKg),
}));

export const rowsFromChart = (chart) => (chart?.entries || []).map((entry) => ({
  key: key(), size: entry.size, minHeightCm: text(entry.minHeightCm), maxHeightCm: text(entry.maxHeightCm),
  minWeightKg: text(entry.minWeightKg), maxWeightKg: text(entry.maxWeightKg),
}));

const numberOrNull = (value) => (String(value ?? "").trim() === "" ? null : Number(String(value).replace(",", ".")));

export const chartPayload = (rows) => ({
  entries: rows.map((row) => ({
    size: row.size.trim(),
    minHeightCm: numberOrNull(row.minHeightCm), maxHeightCm: numberOrNull(row.maxHeightCm),
    minWeightKg: numberOrNull(row.minWeightKg), maxWeightKg: numberOrNull(row.maxWeightKg),
  })),
});

/** Kiểm tra nhanh phía client; backend vẫn kiểm tra lại. Trả thông báo lỗi hoặc null. */
export function validateChartRows(rows) {
  const seen = new Set();
  for (const [index, entry] of chartPayload(rows).entries.entries()) {
    const label = entry.size || `dòng ${index + 1}`;
    if (!entry.size) return `Nhập tên size cho dòng ${index + 1}.`;
    const name = entry.size.toLowerCase();
    if (seen.has(name)) return `Size ${entry.size} bị trùng.`;
    seen.add(name);
    const values = [entry.minHeightCm, entry.maxHeightCm, entry.minWeightKg, entry.maxWeightKg];
    if (values.every((value) => value === null)) return `Size ${label} cần ít nhất một khoảng chiều cao hoặc cân nặng.`;
    if (values.some((value) => value !== null && Number.isNaN(value))) return `Size ${label} có giá trị không hợp lệ.`;
    if ([entry.minHeightCm, entry.maxHeightCm].some((v) => v !== null && (!Number.isInteger(v) || v < 80 || v > 250)))
      return `Size ${label}: chiều cao là số nguyên từ 80 đến 250 cm.`;
    if ([entry.minWeightKg, entry.maxWeightKg].some((v) => v !== null && (v < 20 || v > 250)))
      return `Size ${label}: cân nặng từ 20 đến 250 kg.`;
    if (entry.minHeightCm !== null && entry.maxHeightCm !== null && entry.minHeightCm > entry.maxHeightCm
      || entry.minWeightKg !== null && entry.maxWeightKg !== null && entry.minWeightKg > entry.maxWeightKg)
      return `Size ${label}: giá trị "từ" phải nhỏ hơn hoặc bằng "đến".`;
  }
  return null;
}
