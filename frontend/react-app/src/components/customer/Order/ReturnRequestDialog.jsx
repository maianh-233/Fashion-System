import { useEffect, useState } from "react";
import { LoaderCircle, Minus, Plus, X } from "lucide-react";
import Button from "../../common/Button";
import { returnApi } from "../../../api/customer/accountApi";
import { formatPrice } from "../../../pages/customer/storefrontLogic";
import {
  RETURN_TYPE_LABELS, buildReturnPayload, estimateRefund, initialSelection, validateReturnForm, variantLabel,
} from "../../../pages/customer/returnLogic";

/** Khách gửi yêu cầu đổi / trả cho một đơn (hoặc phần đơn) đã hoàn thành. */
export default function ReturnRequestDialog({ orderId, orderCode, onClose, onCreated }) {
  const [data, setData] = useState(null);
  const [type, setType] = useState("RETURN");
  const [selection, setSelection] = useState({});
  const [reasonCode, setReasonCode] = useState("");
  const [note, setNote] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    returnApi.eligibility(orderId, { signal: controller.signal })
      .then((result) => { setData(result); setSelection(initialSelection(result.items)); })
      .catch((failure) => { if (!controller.signal.aborted) setData({ eligible: false, reason: failure.message, items: [], reasons: [] }); });
    const onKey = (event) => { if (event.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => { controller.abort(); window.removeEventListener("keydown", onKey); };
  }, [orderId, onClose]);

  const items = (data?.items || []).filter((item) => item.requestable > 0);
  const update = (id, patch) => {
    setError("");
    setSelection((current) => ({ ...current, [id]: { ...current[id], ...patch } }));
  };

  const submit = async (event) => {
    event.preventDefault();
    const problem = validateReturnForm({ type, reasonCode, items, selection });
    setError(problem);
    if (problem) return;
    setSaving(true);
    try {
      onCreated(await returnApi.create(buildReturnPayload({ orderId, type, reasonCode, note, items, selection })));
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  const refund = estimateRefund(items, selection);
  return (
    <div className="customer-dialog" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <form className="customer-dialog__panel return-form" role="dialog" aria-modal="true" aria-labelledby="return-form-title" onSubmit={submit}>
        <header>
          <div>
            <small>Đơn #{orderCode}</small>
            <h2 id="return-form-title">Yêu cầu đổi / trả</h2>
          </div>
          <button type="button" onClick={onClose} aria-label="Đóng"><X size={17} /></button>
        </header>

        {!data && <p className="customer-dialog__hint"><LoaderCircle className="animate-spin" size={15} /> Đang tải đơn hàng...</p>}
        {data && !data.eligible && <p className="checkout-error">{data.reason}</p>}
        {data?.eligible && items.length === 0 && (
          <p className="customer-dialog__hint">Tất cả sản phẩm của đơn đã nằm trong yêu cầu đổi / trả khác.</p>
        )}

        {data?.eligible && items.length > 0 && <>
          <p className="customer-dialog__hint">
            Hạn đổi / trả: {new Date(data.deadline).toLocaleDateString("vi-VN")}. Sau khi cửa hàng duyệt, vui lòng mang sản phẩm
            (còn tem mác) tới cửa hàng đã bán.
          </p>

          <div className="return-form__types" role="radiogroup" aria-label="Loại yêu cầu">
            {Object.entries(RETURN_TYPE_LABELS).map(([key, label]) => (
              <button key={key} type="button" role="radio" aria-checked={type === key} className={type === key ? "is-active" : ""}
                onClick={() => { setError(""); setType(key); }}>{label}</button>
            ))}
          </div>

          <ul className="return-form__items">
            {items.map((item) => {
              const line = selection[item.orderItemId] || {};
              return (
                <li key={item.orderItemId} className={line.checked ? "is-checked" : ""}>
                  <label className="return-form__pick">
                    <input type="checkbox" checked={Boolean(line.checked)} onChange={(e) => update(item.orderItemId, { checked: e.target.checked })} />
                    {item.imageUrl ? <img src={item.imageUrl} alt="" /> : <span className="return-form__thumb" />}
                    <span>
                      <strong>{item.productName}</strong>
                      <small>{variantLabel(item.color, item.size)} · {formatPrice(item.unitPrice)} · còn đổi/trả được {item.requestable}</small>
                    </span>
                  </label>
                  {line.checked && (
                    <div className="return-form__line">
                      <div className="product-detail-quantity">
                        <button type="button" aria-label="Giảm" disabled={line.quantity <= 1}
                          onClick={() => update(item.orderItemId, { quantity: line.quantity - 1 })}><Minus size={13} /></button>
                        <span>{line.quantity}</span>
                        <button type="button" aria-label="Tăng" disabled={line.quantity >= item.requestable}
                          onClick={() => update(item.orderItemId, { quantity: line.quantity + 1 })}><Plus size={13} /></button>
                      </div>
                      {type === "EXCHANGE" && (
                        <select value={line.exchangeVariantId} aria-label={`Đổi ${item.productName} sang`}
                          onChange={(e) => update(item.orderItemId, { exchangeVariantId: e.target.value })}>
                          <option value="">Đổi sang phân loại...</option>
                          {item.exchangeOptions.map((option) => (
                            <option key={option.variantId} value={option.variantId} disabled={!option.inStock}>
                              {variantLabel(option.color, option.size)}{option.inStock ? "" : " (cửa hàng đang hết)"}
                            </option>
                          ))}
                        </select>
                      )}
                    </div>
                  )}
                </li>
              );
            })}
          </ul>

          <label className="customer-dialog__note">
            <span>Lý do</span>
            <select value={reasonCode} onChange={(e) => { setError(""); setReasonCode(e.target.value); }}>
              <option value="">Chọn lý do...</option>
              {data.reasons.map((reason) => <option key={reason.code} value={reason.code}>{reason.label}</option>)}
            </select>
          </label>
          <label className="customer-dialog__note">
            <span>Ghi chú (không bắt buộc)</span>
            <textarea rows={2} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)}
              placeholder="Mô tả thêm tình trạng sản phẩm" />
          </label>
          {type === "RETURN" && refund > 0 && (
            <p className="return-form__refund">Dự kiến hoàn: <strong>{formatPrice(refund)}</strong> (theo giá bạn đã trả)</p>
          )}
        </>}

        {error && <p className="checkout-error" role="alert">{error}</p>}
        <footer>
          <Button type="button" variant="unstyled" className="checkout-secondary-button" onClick={onClose} disabled={saving}>Đóng</Button>
          {data?.eligible && items.length > 0 && (
            <Button type="submit" variant="unstyled" className="checkout-primary-button" loading={saving}>Gửi yêu cầu</Button>
          )}
        </footer>
      </form>
    </div>
  );
}
