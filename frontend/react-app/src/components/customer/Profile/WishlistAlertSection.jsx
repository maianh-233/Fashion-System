import { useEffect, useState } from "react";
import { LoaderCircle } from "lucide-react";
import { useSystemNotification } from "../../common/SystemNotification";
import { wishlistApi } from "../../../api/customer/accountApi";

/** Công tắc báo khi sản phẩm yêu thích giảm giá, vào flash sale hoặc sắp hết hàng (thông báo + email gộp mỗi ngày). */
export default function WishlistAlertSection() {
  const notification = useSystemNotification();
  const [enabled, setEnabled] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    wishlistApi.alerts({ signal: controller.signal })
      .then((data) => setEnabled(Boolean(data.enabled)))
      .catch((failure) => { if (!controller.signal.aborted) setError(failure.message); });
    return () => controller.abort();
  }, []);

  if (enabled === null && !error) return <p className="customer-profile-measurements__loading"><LoaderCircle className="animate-spin" size={15} /> Đang tải...</p>;

  const toggle = async () => {
    setSaving(true);
    setError("");
    try {
      const saved = await wishlistApi.setAlerts(!enabled);
      setEnabled(Boolean(saved.enabled));
      notification.success(saved.enabled ? "Đã bật báo sản phẩm yêu thích." : "Đã tắt báo sản phẩm yêu thích.");
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="customer-profile-alerts">
      <label className="customer-profile-alerts__row">
        <span>
          <strong>Báo khi sản phẩm yêu thích giảm giá, vào flash sale hoặc sắp hết hàng</strong>
          <small>Thông báo ngay trong tài khoản; email gộp tối đa 1 lần mỗi ngày.</small>
        </span>
        <input type="checkbox" role="switch" checked={Boolean(enabled)} disabled={saving || enabled === null} onChange={toggle} />
      </label>
      {error && <p className="checkout-error">{error}</p>}
    </div>
  );
}
