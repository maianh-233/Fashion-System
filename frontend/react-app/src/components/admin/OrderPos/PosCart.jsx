import { ImageOff, Minus, Plus, ShoppingCart, Trash2 } from "lucide-react";
import { money } from "../../../pages/admin/offlinePosLogic";

/**
 * Giỏ hàng: số tiền mỗi dòng lấy từ quote của backend; khi quote chưa về thì hiện tạm giá niêm yết.
 * Khuyến mãi sản phẩm hiển thị ngay trên dòng (giá gốc gạch ngang + giá sau giảm).
 */
export default function PosCart({ cart, quote, onQuantity, onRemove }) {
  const quoted = new Map((quote?.lines || []).map(line => [line.variantId, line]));
  const units = cart.reduce((sum, line) => sum + line.quantity, 0);

  return <section className="pos-card">
    <div className="pos-card__title"><span><ShoppingCart size={18} /> Giỏ hàng</span><small>{cart.length} dòng · {units} sản phẩm</small></div>
    {!cart.length && <div className="pos-empty"><ShoppingCart size={28} /><span>Chưa có sản phẩm. Quét mã hoặc tìm theo tên để thêm.</span></div>}
    <div className="pos-cart">
      {cart.map(line => {
        const q = quoted.get(line.variantId);
        const info = q || line.preview;
        const subtotal = q ? q.subtotal : Number(line.preview.unitPrice || 0) * line.quantity;
        const discount = q ? Number(q.discount) : 0;
        return <article key={line.variantId} className={`pos-line ${q?.warning ? "has-warning" : ""}`}>
          {info.imageUrl ? <img src={info.imageUrl} alt="" className="pos-thumb" /> : <span className="pos-thumb grid place-items-center"><ImageOff size={16} /></span>}
          <div className="pos-line__body">
            <div className="pos-line__head">
              <div className="min-w-0">
                <strong>{info.productName}</strong>
                <p className="pos-muted">{info.sku} · {[info.color, info.size].filter(Boolean).join(" / ")}</p>
              </div>
              <button type="button" className="pos-icon-button" aria-label={`Xóa ${info.sku}`} onClick={() => onRemove(line.variantId)}><Trash2 size={16} /></button>
            </div>
            <div className="pos-line__foot">
              <div className="flex items-center gap-3">
                <div className="pos-qty">
                  <button type="button" aria-label="Giảm số lượng" disabled={line.quantity <= 1} onClick={() => onQuantity(line.variantId, line.quantity - 1)}><Minus size={14} /></button>
                  <input inputMode="numeric" aria-label={`Số lượng ${info.sku}`} value={line.quantity}
                    onChange={event => onQuantity(line.variantId, event.target.value)} />
                  <button type="button" aria-label="Tăng số lượng" disabled={line.quantity >= (q?.stock ?? line.stock)} onClick={() => onQuantity(line.variantId, line.quantity + 1)}><Plus size={14} /></button>
                </div>
                <span className="pos-muted">× {money(info.unitPrice)} · tồn {q?.stock ?? line.stock}</span>
              </div>
              <div className="pos-price">
                {discount > 0 && <s>{money(subtotal)}</s>}
                <strong>{money(q ? q.total : subtotal)}</strong>
                {q?.promotionCode && <div><span className="pos-tag">{q.promotionCode} −{money(discount)}</span></div>}
              </div>
            </div>
            {q?.warning && <p className="pos-error">{q.warning}</p>}
          </div>
        </article>;
      })}
    </div>
  </section>;
}
