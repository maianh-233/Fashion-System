export const FLASH_PER_CUSTOMER = 2;

/** Thời gian còn lại tới {@code endsAt}: {done, hours, minutes, seconds, text "hh:mm:ss"}. */
export function countdown(endsAt, now = Date.now()) {
  const left = Math.max(0, Math.floor((new Date(endsAt).getTime() - now) / 1000));
  const hours = Math.floor(left / 3600);
  const minutes = Math.floor((left % 3600) / 60);
  const seconds = left % 60;
  const pad = (n) => String(n).padStart(2, "0");
  return { done: left === 0, hours, minutes, seconds, text: `${pad(hours)}:${pad(minutes)}:${pad(seconds)}` };
}

/** Biến thể → thông tin flash sale đang chạy (chương trình kết thúc sớm nhất thắng nếu trùng). */
export function flashByVariant(sales = []) {
  const map = {};
  [...sales].sort((a, b) => new Date(a.endsAt) - new Date(b.endsAt)).forEach((sale) => {
    sale.items.forEach((item) => {
      if (!map[item.variantId]) map[item.variantId] = { ...item, saleName: sale.name, endsAt: sale.endsAt };
    });
  });
  return map;
}

export function percentOff(regular, sale) {
  const r = Number(regular);
  return r > 0 ? Math.round((1 - Number(sale) / r) * 100) : 0;
}

/** % suất đã bán để vẽ thanh tiến độ. */
export function soldPercent(item) {
  const quota = Number(item?.quota || 0);
  return quota > 0 ? Math.min(100, Math.round(((quota - Number(item.remaining || 0)) / quota) * 100)) : 0;
}
