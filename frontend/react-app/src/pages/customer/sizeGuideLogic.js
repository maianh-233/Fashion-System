// Logic thuần của gợi ý size: số đo nhập tay, nhãn khoảng trong bảng size và câu gợi ý.

export const HEIGHT_RANGE = [80, 250];
export const WEIGHT_RANGE = [20, 250];

/** Chuỗi truy vấn số đo, bắt đầu bằng {@code prefix} ("?" hoặc "&"); rỗng khi không có số đo. */
export function measurementQuery(heightCm, weightKg, prefix) {
  const params = new URLSearchParams();
  if (heightCm != null && heightCm !== "") params.set("heightCm", String(heightCm));
  if (weightKg != null && weightKg !== "") params.set("weightKg", String(weightKg));
  const query = params.toString();
  return query ? `${prefix}${query}` : "";
}

/**
 * Đọc số đo từ ô nhập: chiều cao số nguyên cm, cân nặng tối đa 1 số lẻ (chấp nhận dấu phẩy).
 * Trả {heightCm, weightKg, error}; ô trống là null.
 */
export function parseMeasurements(height, weight) {
  const text = (value) => String(value ?? "").trim().replace(",", ".");
  const h = text(height);
  const w = text(weight);
  const heightCm = h === "" ? null : Number(h);
  const weightKg = w === "" ? null : Math.round(Number(w) * 10) / 10;
  if (heightCm != null && (!Number.isInteger(heightCm) || heightCm < HEIGHT_RANGE[0] || heightCm > HEIGHT_RANGE[1]))
    return { heightCm: null, weightKg: null, error: `Chiều cao từ ${HEIGHT_RANGE[0]} đến ${HEIGHT_RANGE[1]} cm.` };
  if (weightKg != null && (Number.isNaN(weightKg) || weightKg < WEIGHT_RANGE[0] || weightKg > WEIGHT_RANGE[1]))
    return { heightCm: null, weightKg: null, error: `Cân nặng từ ${WEIGHT_RANGE[0]} đến ${WEIGHT_RANGE[1]} kg.` };
  return { heightCm, weightKg, error: "" };
}

const number = (value) => Number(value).toLocaleString("vi-VN");

/** "160 – 170 cm", "từ 60 kg", "đến 50 kg" hoặc "—". */
export function rangeLabel(min, max, unit) {
  if (min != null && max != null) return `${number(min)} – ${number(max)} ${unit}`;
  if (min != null) return `từ ${number(min)} ${unit}`;
  if (max != null) return `đến ${number(max)} ${unit}`;
  return "—";
}

/** Câu ngắn dưới nút chọn size; null khi chưa có gì để gợi ý. */
export function recommendationLine(guide) {
  if (!guide?.recommendedSize) return null;
  return guide.source === "HISTORY"
    ? `Gợi ý cho bạn: size ${guide.recommendedSize} (size bạn thường mua và giữ lại)`
    : `Gợi ý cho bạn: size ${guide.recommendedSize} (theo số đo của bạn)`;
}

/** Dòng bảng size khớp với size được gợi ý theo số đo. */
export const isChartMatch = (entry, guide) =>
  Boolean(guide?.chartSize) && entry.size.trim().toLowerCase() === guide.chartSize.trim().toLowerCase();
