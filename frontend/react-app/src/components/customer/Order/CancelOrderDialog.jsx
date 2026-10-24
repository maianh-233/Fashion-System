import { useEffect, useState } from "react";
import { X } from "lucide-react";
import Button from "../../common/Button";
import { customerOrderApi } from "../../../api/customer/accountApi";

/** Khách hủy đơn khi cửa hàng chưa xác nhận: chọn lý do (không bắt buộc) và ghi chú. */
export default function CancelOrderDialog({ order, onClose, onCancelled }) {
  const [reasons, setReasons] = useState([]);
  const [reasonCode, setReasonCode] = useState("");
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    customerOrderApi.cancelReasons().then(setReasons).catch(() => setReasons([]));
    const onKey = (event) => { if (event.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const updated = await customerOrderApi.cancel(order.id, { reasonCode: reasonCode || null, note: note.trim() || null });
      onCancelled(updated);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="customer-dialog" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <form className="customer-dialog__panel" role="dialog" aria-modal="true" aria-labelledby="cancel-order-title" onSubmit={submit}>
        <header>
          <div>
            <small>Đơn #{order.orderCode}</small>
            <h2 id="cancel-order-title">Hủy đơn hàng</h2>
          </div>
          <button type="button" onClick={onClose} aria-label="Đóng"><X size={17} /></button>
        </header>
        <p className="customer-dialog__hint">Đơn đã hủy không khôi phục được. Voucher đã dùng cho đơn sẽ không được hoàn lại.</p>
        <fieldset className="customer-dialog__reasons">
          <legend>Lý do hủy (không bắt buộc)</legend>
          {reasons.map((reason) => (
            <label key={reason.code}>
              <input type="radio" name="cancel-reason" checked={reasonCode === reason.code} onChange={() => setReasonCode(reason.code)} />
              {reason.label}
            </label>
          ))}
        </fieldset>
        <label className="customer-dialog__note">
          <span>Ghi chú thêm</span>
          <textarea value={note} onChange={(e) => setNote(e.target.value.slice(0, 500))} rows={3} placeholder="Ví dụ: muốn đổi sang size khác" />
        </label>
        {error && <p className="checkout-error">{error}</p>}
        <footer>
          <Button type="button" variant="unstyled" className="checkout-secondary-button" onClick={onClose} disabled={saving}>Giữ đơn</Button>
          <Button type="submit" variant="unstyled" className="checkout-primary-button customer-dialog__danger" loading={saving}>Hủy đơn</Button>
        </footer>
      </form>
    </div>
  );
}
