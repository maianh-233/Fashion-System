import { useState } from "react";
import { Heart } from "lucide-react";
import { useSystemNotification } from "../common/SystemNotification";
import { useCustomerWishlist } from "../../contexts/CustomerWishlistContext";

/** Nút tim bật/tắt yêu thích một sản phẩm; ẩn khi nằm ngoài trang khách. */
export default function WishlistButton({ productId, productName, className = "", showLabel = false }) {
  const wishlist = useCustomerWishlist();
  const notification = useSystemNotification();
  const [busy, setBusy] = useState(false);
  if (!wishlist || !productId) return null;

  const active = wishlist.has(productId);
  const toggle = async (event) => {
    event.preventDefault();
    event.stopPropagation();
    if (busy) return;
    setBusy(true);
    try {
      const added = await wishlist.toggle(productId);
      notification.success(added ? `Đã thêm ${productName || "sản phẩm"} vào yêu thích.` : "Đã bỏ khỏi danh sách yêu thích.");
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setBusy(false);
    }
  };

  const label = active ? "Bỏ khỏi yêu thích" : "Thêm vào yêu thích";
  return (
    <button type="button" className={`wishlist-button ${active ? "is-active" : ""} ${className}`} onClick={toggle}
      aria-pressed={active} aria-label={`${label}${productName ? `: ${productName}` : ""}`} title={label} disabled={busy}>
      <Heart size={showLabel ? 17 : 16} fill={active ? "currentColor" : "none"} />
      {showLabel && <span>{active ? "Đã yêu thích" : "Yêu thích"}</span>}
    </button>
  );
}
