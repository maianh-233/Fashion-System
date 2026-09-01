import { Minus, Plus, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import Button from "../../common/Button";

const formatPrice = (value) => `${Number(value).toLocaleString("vi-VN")} ₫`;

export default function CartItem({
  item,
  onToggleCheck,
  onChangeQty,
  onRemove,
}) {
  return (
    <article className={`customer-cart-item cart-item ${item.checked ? "is-selected" : ""}`}>
      <label className="cart-item__check">
        <input
          type="checkbox"
          checked={item.checked}
          onChange={onToggleCheck}
          aria-label={`Chọn ${item.name}`}
        />
        <span />
      </label>

      <Link to={`/productdetail?id=${item.id}`} className="cart-item__media">
        <img src={item.image} alt={item.name} loading="lazy" />
      </Link>

      <div className="cart-item__content">
        <div className="cart-item__heading">
          <div>
            <p>{item.brand}</p>
            <Link to={`/productdetail?id=${item.id}`}>{item.name}</Link>
            <div className="cart-item__variants">
              <span>Màu · {item.color}</span>
              <span>Size · {item.size}</span>
            </div>
          </div>

          <Button
            type="button"
            variant="unstyled"
            className="cart-item__remove"
            onClick={onRemove}
            aria-label={`Xóa ${item.name} khỏi giỏ hàng`}
          >
            <Trash2 size={16} />
          </Button>
        </div>

        <div className="cart-item__footer">
          <div className="cart-item__quantity" aria-label={`Số lượng ${item.name}`}>
            <button type="button" onClick={() => onChangeQty(-1)} aria-label={`Giảm số lượng ${item.name}`} disabled={item.quantity <= 1}>
              <Minus size={14} />
            </button>
            <span>{item.quantity}</span>
            <button type="button" onClick={() => onChangeQty(1)} aria-label={`Tăng số lượng ${item.name}`}>
              <Plus size={14} />
            </button>
          </div>

          <div className="cart-item__price">
            <small>{formatPrice(item.price)} / sản phẩm</small>
            <strong>{formatPrice(item.price * item.quantity)}</strong>
          </div>
        </div>
      </div>
    </article>
  );
}
