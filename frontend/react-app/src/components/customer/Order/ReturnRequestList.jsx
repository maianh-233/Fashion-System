import { useEffect, useState } from "react";
import { RotateCcw } from "lucide-react";
import Button from "../../common/Button";
import { useSystemNotification } from "../../common/SystemNotification";
import { returnApi } from "../../../api/customer/accountApi";
import { formatPrice } from "../../../pages/customer/storefrontLogic";
import { RETURN_STATUS_TONES, RETURN_TYPE_LABELS, variantLabel } from "../../../pages/customer/returnLogic";

/** Các yêu cầu đổi / trả của một đơn trên trang chi tiết đơn; khách hủy được yêu cầu đang chờ duyệt. */
export default function ReturnRequestList({ orderId, version = 0 }) {
  const notification = useSystemNotification();
  const [requests, setRequests] = useState([]);
  const [busy, setBusy] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    returnApi.list(orderId, { signal: controller.signal }).then(setRequests).catch(() => {});
    return () => controller.abort();
  }, [orderId, version]);

  if (requests.length === 0) return null;

  const cancel = async (request) => {
    if (!await notification.confirm(`Hủy yêu cầu ${request.requestCode}?`, { confirmText: "Hủy yêu cầu" })) return;
    setBusy(request.id);
    try {
      const updated = await returnApi.cancel(request.id);
      setRequests((current) => current.map((r) => (r.id === updated.id ? updated : r)));
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setBusy(null);
    }
  };

  return (
    <section className="checkout-section return-requests">
      <div className="checkout-section__heading">
        <span><RotateCcw size={17} /></span>
        <div><small>Returns & exchanges</small><h2>Yêu cầu đổi / trả</h2></div>
      </div>
      <ul>
        {requests.map((request) => (
          <li key={request.id}>
            <div className="return-requests__head">
              <strong>{request.requestCode} · {RETURN_TYPE_LABELS[request.type]}</strong>
              <span className={`return-status is-${RETURN_STATUS_TONES[request.status] || "muted"}`}>{request.statusLabel}</span>
            </div>
            <small>{request.reasonLabel} · {new Date(request.createdAt).toLocaleString("vi-VN")} · {request.storeName}</small>
            <ul className="return-requests__items">
              {request.items.map((item) => (
                <li key={item.id}>
                  {item.quantity} × {item.productName} ({variantLabel(item.color, item.size)})
                  {item.exchangeVariantId && <> → <strong>{variantLabel(item.exchangeColor, item.exchangeSize)}</strong></>}
                </li>
              ))}
            </ul>
            {request.status === "REJECTED" && request.rejectReason && <p className="return-requests__note">Lý do từ chối: {request.rejectReason}</p>}
            {request.status === "APPROVED" && (
              <p className="return-requests__note">Vui lòng mang sản phẩm tới {request.storeName} để hoàn tất.</p>
            )}
            {request.type === "RETURN" && (request.refundAmount ?? request.estimatedRefund) > 0 && (
              <p className="return-requests__note">
                {request.refundAmount != null ? "Số tiền hoàn" : "Dự kiến hoàn"}: <strong>{formatPrice(request.refundAmount ?? request.estimatedRefund)}</strong>
                {request.refundStatus === "PENDING" && " · đang chờ hoàn về tài khoản thanh toán"}
              </p>
            )}
            {request.status === "REQUESTED" && (
              <Button type="button" variant="unstyled" className="checkout-secondary-button" loading={busy === request.id}
                onClick={() => cancel(request)}>Hủy yêu cầu</Button>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
