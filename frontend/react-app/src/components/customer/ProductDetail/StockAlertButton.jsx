import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { BellOff, BellRing } from "lucide-react";
import Button from "../../common/Button";
import { useSystemNotification } from "../../common/SystemNotification";
import { stockAlertApi } from "../../../api/customer/accountApi";
import { useCustomerSession } from "../../../hooks/useCustomerSession";
import { loginRedirect } from "../../../pages/customer/stockAlertLogic";

/** Biến thể đang hết hàng online: khách đăng ký để được báo khi có hàng lại (hoặc hủy đăng ký). */
export default function StockAlertButton({ productId, variant }) {
  const { loggedIn } = useCustomerSession();
  const notification = useSystemNotification();
  const navigate = useNavigate();
  const [pending, setPending] = useState({ productId: null, ids: [] });
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!loggedIn) return undefined;
    const controller = new AbortController();
    stockAlertApi.pending(productId, { signal: controller.signal })
      .then((data) => setPending({ productId, ids: data.variantIds || [] }))
      .catch(() => {});
    return () => controller.abort();
  }, [loggedIn, productId]);

  const subscribed = loggedIn && pending.productId === productId && pending.ids.includes(variant.id);
  const label = [variant.color, variant.size].filter(Boolean).join(" / ");

  const toggle = async () => {
    if (!loggedIn) {
      navigate(loginRedirect(productId));
      return;
    }
    setBusy(true);
    try {
      const data = subscribed ? await stockAlertApi.unsubscribe(variant.id) : await stockAlertApi.subscribe(variant.id);
      setPending({ productId, ids: data.variantIds || [] });
      notification.success(subscribed ? "Đã hủy nhận thông báo cho phân loại này."
        : `Lunaria sẽ báo cho bạn khi ${label || "sản phẩm"} có hàng trở lại.`);
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="product-detail-stock-alert">
      <Button type="button" variant="unstyled" className={`product-detail-stock-alert__button ${subscribed ? "is-on" : ""}`}
        loading={busy} onClick={toggle} aria-pressed={subscribed}>
        {subscribed ? <BellOff size={16} /> : <BellRing size={16} />}
        {subscribed ? "Hủy nhận thông báo" : "Báo cho tôi khi có hàng"}
      </Button>
      <small>
        {subscribed ? `Đã đăng ký: chúng tôi sẽ gửi thông báo khi ${label || "sản phẩm"} có hàng.`
          : loggedIn ? "Nhận thông báo ngay khi phân loại này được nhập thêm." : "Đăng nhập để nhận thông báo khi có hàng."}
      </small>
    </div>
  );
}
