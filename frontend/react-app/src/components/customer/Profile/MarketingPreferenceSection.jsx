import { useEffect, useState } from "react";
import { LoaderCircle } from "lucide-react";
import { useSystemNotification } from "../../common/SystemNotification";
import { marketingPreferenceApi } from "../../../api/customer/accountApi";

/**
 * Tùy chọn nhận tin khuyến mãi. Email là opt-in (mặc định tắt) và cần có email trong hồ sơ; thông báo trong app mặc định
 * bật. Không ảnh hưởng thông báo / email về đơn hàng.
 */
export default function MarketingPreferenceSection() {
  const notification = useSystemNotification();
  const [prefs, setPrefs] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    marketingPreferenceApi.get({ signal: controller.signal })
      .then(setPrefs)
      .catch((failure) => { if (!controller.signal.aborted) setError(failure.message); });
    return () => controller.abort();
  }, []);

  if (!prefs && !error) return <p className="customer-profile-measurements__loading"><LoaderCircle className="animate-spin" size={15} /> Đang tải...</p>;

  const save = async (patch, message) => {
    setSaving(true);
    setError("");
    try {
      setPrefs(await marketingPreferenceApi.update({ emailOptIn: prefs.emailOptIn, inAppEnabled: prefs.inAppEnabled, ...patch }));
      notification.success(message);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="customer-profile-alerts" style={{ display: "grid", gap: "0.75rem" }}>
      <label className="customer-profile-alerts__row">
        <span>
          <strong>Nhận email ưu đãi và khuyến mãi</strong>
          <small>
            {prefs?.email ? `Gửi tới ${prefs.email}. ` : "Thêm email trong hồ sơ để bật. "}
            Bạn có thể hủy bất cứ lúc nào bằng link trong mỗi email.
          </small>
        </span>
        <input type="checkbox" role="switch" checked={Boolean(prefs?.emailOptIn)} disabled={saving || !prefs || (!prefs.email && !prefs.emailOptIn)}
          onChange={() => save({ emailOptIn: !prefs.emailOptIn }, prefs.emailOptIn ? "Đã hủy nhận email ưu đãi." : "Đã đăng ký nhận email ưu đãi.")} />
      </label>
      <label className="customer-profile-alerts__row">
        <span>
          <strong>Thông báo ưu đãi trong tài khoản</strong>
          <small>Tin khuyến mãi hiện ở chuông thông báo. Thông báo về đơn hàng luôn được gửi.</small>
        </span>
        <input type="checkbox" role="switch" checked={Boolean(prefs?.inAppEnabled)} disabled={saving || !prefs}
          onChange={() => save({ inAppEnabled: !prefs.inAppEnabled }, prefs.inAppEnabled ? "Đã tắt thông báo ưu đãi." : "Đã bật thông báo ưu đãi.")} />
      </label>
      {error && <p className="checkout-error">{error}</p>}
    </div>
  );
}
