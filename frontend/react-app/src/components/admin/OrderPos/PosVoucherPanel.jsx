import { TicketPercent } from "lucide-react";
import { money, voucherValueLabel } from "../../../pages/admin/offlinePosLogic";

const TYPE_LABEL = { ORDER: "Toàn đơn", PRODUCT: "Sản phẩm", POINT_REDEMPTION: "Đổi điểm", BIRTHDAY: "Sinh nhật" };

/**
 * Voucher khả dụng do backend tính và sắp theo số tiền giảm thực tế. Voucher chưa đủ điều kiện hiện mờ
 * kèm lý do. Quy tắc: 1 voucher toàn đơn + các khuyến mãi sản phẩm.
 */
export default function PosVoucherPanel({ vouchers, selected, loading, error, onToggle }) {
  const usable = vouchers.filter(v => v.eligible || selected.includes(v.code));
  const unusable = vouchers.filter(v => !v.eligible && !selected.includes(v.code));
  const item = voucher => {
    const isSelected = selected.includes(voucher.code);
    return <button type="button" key={voucher.id} disabled={!voucher.eligible && !isSelected}
      className={`pos-voucher ${isSelected ? "is-selected" : ""}`} aria-pressed={isSelected} onClick={() => onToggle(voucher)}>
      <span className="pos-voucher__row">
        <span className="flex flex-wrap items-center gap-2">
          <span className="pos-voucher__code">{voucher.code}</span>
          <span className="pos-tag pos-tag--muted">{TYPE_LABEL[voucher.type] || voucher.type}</span>
          {voucher.suggested && <span className="pos-tag">Gợi ý</span>}
        </span>
        {voucher.eligible && <span className="pos-voucher__amount">−{money(voucher.discount)}</span>}
      </span>
      <span className="text-sm">{voucher.name}</span>
      <span className="pos-muted">{voucherValueLabel(voucher)}{voucher.minOrderValue ? ` · đơn từ ${money(voucher.minOrderValue)}` : ""}</span>
      {voucher.reason && <span className="pos-muted" style={{ color: "var(--pos-danger)" }}>{voucher.reason}</span>}
    </button>;
  };

  return <section className="pos-card">
    <div className="pos-card__title"><span><TicketPercent size={18} /> Voucher</span><small>{loading ? "Đang cập nhật…" : `${usable.length} dùng được`}</small></div>
    {error && <p className="pos-error">{error}</p>}
    {!error && !vouchers.length && !loading && <p className="pos-muted">Không có voucher đang hiệu lực.</p>}
    <div className="pos-vouchers">
      {usable.map(item)}
      {unusable.length > 0 && <p className="pos-muted mt-1">Chưa đủ điều kiện</p>}
      {unusable.map(item)}
    </div>
  </section>;
}
