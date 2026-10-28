/** Trạng thái hiển thị của một đăng ký "báo khi có hàng". */
export function stockAlertStatus(alert) {
  if (alert?.notifiedAt) return { key: "notified", label: "Đã báo có hàng" };
  if (alert?.inStock) return { key: "available", label: "Đã có hàng" };
  return { key: "waiting", label: "Đang chờ hàng" };
}

/** "Đen / M" từ màu và size của biến thể. */
export function variantText(alert) {
  return [alert?.color, alert?.size].filter((part) => part && String(part).trim()).join(" / ");
}

/** Đường dẫn đăng nhập quay lại trang sản phẩm hiện tại. */
export function loginRedirect(productId) {
  return `/customerlogin?redirect=${encodeURIComponent(`/productdetail?id=${productId}`)}`;
}
