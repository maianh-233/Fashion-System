import { Banknote, CheckCircle2 } from "lucide-react";
import Button from "../../common/Button";
import { cashSuggestions, money } from "../../../pages/admin/offlinePosLogic";

/** Tổng tiền (từ quote backend), tiền khách đưa, tiền thối và nút tạo đơn. */
export default function PosCheckout({ quote, quoteLoading, quoteError, hasItems, cash, onCash, note, onNote, creating, onCreate, lastCreated }) {
  const total = quote?.total ?? 0;
  const warnings = quote?.warnings || [];
  const notices = quote?.notices || [];
  const canSubmit = hasItems && quote?.canCreate && !quoteLoading && !creating;

  return <section className="pos-card">
    <div className="pos-card__title"><span><Banknote size={18} /> Thanh toán tiền mặt</span><small>{quoteLoading ? "Đang tính…" : ""}</small></div>
    {lastCreated && <div className="pos-success mb-3">
      <span className="flex items-center gap-2"><CheckCircle2 size={18} /><strong>Đã tạo đơn {lastCreated.orderCode}</strong></span>
      <span className="pos-muted">Tổng {money(lastCreated.total)} · khách đưa {money(lastCreated.cashReceived)} · thối lại <strong>{money(lastCreated.changeAmount)}</strong></span>
    </div>}
    <div className="pos-summary">
      <div className="pos-summary__row"><span>Tạm tính</span><span>{money(quote?.subtotal)}</span></div>
      {Number(quote?.productDiscount) > 0 && <div className="pos-summary__row pos-summary__row--discount"><span>Giảm theo sản phẩm</span><span>−{money(quote.productDiscount)}</span></div>}
      {(quote?.promotions || []).filter(p => p.type !== "PRODUCT").map(p => <div key={p.id} className="pos-summary__row pos-summary__row--discount"><span>Voucher {p.code}</span><span>−{money(p.discount)}</span></div>)}
      <div className="pos-summary__total"><span>Khách cần trả</span><strong>{money(total)}</strong></div>
    </div>

    <label className="mt-3 block">
      <span className="pos-muted">Tiền khách đưa</span>
      <input className="pos-input mt-1" inputMode="numeric" placeholder="0" aria-label="Tiền khách đưa"
        value={cash === "" ? "" : Number(cash).toLocaleString("vi-VN")}
        onChange={event => { const digits = event.target.value.replace(/\D/g, ""); onCash(digits === "" ? "" : Number(digits)); }} />
    </label>
    {hasItems && <div className="pos-cash-chips">
      {cashSuggestions(total).map(value => <button type="button" key={value} className="pos-chip" onClick={() => onCash(value)}>{money(value)}</button>)}
    </div>}
    <div className="pos-change"><span>Tiền thối lại</span><strong>{quote?.changeAmount != null ? money(quote.changeAmount) : "—"}</strong></div>

    <textarea className="pos-input mt-3" rows={2} maxLength={1000} placeholder="Ghi chú đơn (không bắt buộc)" aria-label="Ghi chú đơn"
      value={note} onChange={event => onNote(event.target.value)} />

    {quoteError && <p className="pos-error mt-2">{quoteError}</p>}
    {(warnings.length > 0 || notices.length > 0) && <ul className="pos-warnings">
      {warnings.map(message => <li key={message}>{message}</li>)}
      {notices.map(message => <li key={message} className="is-notice">{message}</li>)}
    </ul>}

    <Button type="button" variant="primary" className="mt-4 w-full justify-center" loading={creating} disabled={!canSubmit} onClick={onCreate}>
      Tạo đơn & thu {money(total)}
    </Button>
  </section>;
}
