import { AlertTriangle, Minus, Plus, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import Button from "../../common/Button";
import { formatPrice, productImage } from "../../../pages/customer/storefrontLogic";

/** Một dòng giỏ hàng (dữ liệu giỏ từ API: giá, tồn kho hiện tại, lý do không mua được). */
export default function CartItem({ item, busy, onToggleCheck, onChangeQty, onRemove }) {
  const link = `/productdetail?id=${encodeURIComponent(item.slug || item.productId)}`;
  const maxQuantity = Math.max(1, Math.min(99, item.available || 1));
  const onSale = item.salePrice != null && Number(item.salePrice) < Number(item.price);

  return (
    <article className={`customer-cart-item cart-item ${item.selected ? "is-selected" : ""} ${item.problem ? "has-problem" : ""}`} aria-busy={busy || undefined}>
      <label className="cart-item__check">
        <input
          type="checkbox"
          checked={item.selected}
          onChange={onToggleCheck}
          disabled={busy}
          aria-label={`Chọn ${item.productName}`}
        />
        <span />
      </label>

      <Link to={link} className="cart-item__media">
        <img src={productImage(item.imageUrl, item.productName)} alt={item.productName} loading="lazy" />
      </Link>

      <div className="cart-item__content">
        <div className="cart-item__heading">
          <div>
            <p>{item.brandName}</p>
            <Link to={link}>{item.productName}</Link>
            <div className="cart-item__variants">
              {item.color && <span>Màu · {item.color}</span>}
              {item.size && <span>Size · {item.size}</span>}
            </div>
            {item.problem && <p className="cart-item__problem"><AlertTriangle size={13} /> {item.problem}</p>}
          </div>

          <Button
            type="button"
            variant="unstyled"
            className="cart-item__remove"
            onClick={onRemove}
            disabled={busy}
            aria-label={`Xóa ${item.productName} khỏi giỏ hàng`}
          >
            <Trash2 size={16} />
          </Button>
        </div>

        <div className="cart-item__footer">
          <div className="cart-item__quantity" aria-label={`Số lượng ${item.productName}`}>
            <button type="button" onClick={() => onChangeQty(item.quantity - 1)} aria-label={`Giảm số lượng ${item.productName}`} disabled={busy || item.quantity <= 1}>
              <Minus size={14} />
            </button>
            <span>{item.quantity}</span>
            <button type="button" onClick={() => onChangeQty(item.quantity + 1)} aria-label={`Tăng số lượng ${item.productName}`}
              disabled={busy || !item.sellable || item.quantity >= maxQuantity}>
              <Plus size={14} />
            </button>
          </div>

          <div className="cart-item__price">
            <small>{formatPrice(item.unitPrice)} / sản phẩm{onSale && <del> {formatPrice(item.price)}</del>}</small>
            <strong>{formatPrice(item.lineTotal)}</strong>
          </div>
        </div>
      </div>
    </article>
  );
}
