import { useState } from "react";
import { Check, Tag, X } from "lucide-react";
import { formatPrice } from "../../../pages/customer/storefrontLogic";

/**
 * Voucher cho giỏ hiện tại (backend tính số tiền giảm và lý do chưa dùng được). Tối đa một voucher giảm đơn và
 * một voucher theo sản phẩm; backend tự bỏ voucher không hợp lệ và báo lại.
 */
export default function Promotions({ vouchers = [], selectedCodes = [], onToggle, onApplyCode, applied = [] }) {
  const [code, setCode] = useState("");

  const submit = (event) => {
    event.preventDefault();
    const value = code.trim().toUpperCase();
    if (!value) return;
    onApplyCode(value);
    setCode("");
  };

  return (
    <div className="checkout-promotions">
      <h3><Tag size={14} /> Mã ưu đãi</h3>
      <form className="cart-promo-field" onSubmit={submit}>
        <input value={code} onChange={(e) => setCode(e.target.value)} placeholder="Nhập mã ưu đãi" className="cart-promo-input" aria-label="Mã ưu đãi" />
        <button type="submit" className="cart-promo-apply"><span>Áp dụng</span></button>
      </form>

      {vouchers.length > 0 && (
        <div className="checkout-vouchers">
          {vouchers.map((voucher) => {
            const active = selectedCodes.includes(voucher.code);
            return (
              <button key={voucher.id} type="button" disabled={!voucher.eligible && !active}
                className={`checkout-voucher ${active ? "is-active" : ""} ${voucher.eligible ? "" : "is-ineligible"}`}
                onClick={() => onToggle(voucher.code)} aria-pressed={active}>
                <span>
                  <strong>{voucher.code}{voucher.suggested && !active && <em> · Gợi ý</em>}</strong>
                  <small>{voucher.name}</small>
                  {!voucher.eligible && voucher.reason && <small className="checkout-voucher__reason">{voucher.reason}</small>}
                </span>
                {voucher.eligible && Number(voucher.discount) > 0 && <b>-{formatPrice(voucher.discount)}</b>}
                <i>{active && <Check size={13} />}</i>
              </button>
            );
          })}
        </div>
      )}

      {applied.length > 0 && (
        <div>
          {applied.map((promotion) => (
            <article key={promotion.code}>
              <span><strong>{promotion.code}</strong><small>{promotion.name}</small></span>
              <b>-{formatPrice(promotion.discount)}</b>
              {onToggle && (
                <button type="button" className="checkout-voucher__remove" onClick={() => onToggle(promotion.code)} aria-label={`Bỏ mã ${promotion.code}`}>
                  <X size={13} />
                </button>
              )}
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
