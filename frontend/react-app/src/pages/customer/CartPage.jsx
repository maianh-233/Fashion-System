import { useState } from "react";
import { LoaderCircle, ShoppingBag, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import CartItem from "../../components/customer/CartComponent/CartItem";
import OrderSummary from "../../components/customer/CartComponent/OrderSummary";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { useCustomerCart } from "../../contexts/CustomerCartContext";

/** Giỏ hàng (máy chủ khi đã đăng nhập, trình duyệt khi chưa); giá và tồn kho luôn là hiện tại. */
export default function CartPage() {
  const { cart, loading, error, loggedIn, setQuantity, setSelected, selectAll, remove, refresh } = useCustomerCart();
  const notification = useSystemNotification();
  const [busy, setBusy] = useState(null);

  const run = async (key, action) => {
    setBusy(key);
    try {
      await action();
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setBusy(null);
    }
  };

  const items = cart.items;
  const allSelected = items.length > 0 && items.every((item) => item.selected);
  const blocked = items.filter((item) => item.selected && item.problem);
  const blockedReason = blocked.length
    ? `Bỏ chọn hoặc sửa ${blocked.length} sản phẩm không mua được để thanh toán.` : "";

  return (
    <div className="customer-page cart-page min-h-screen text-zinc-200">
      <div className="customer-page__wide cart-page__inner">
        <header className="cart-page__header">
          <div>
            <p><Sparkles size={13} /> Your selection</p>
            <h1>Giỏ hàng của bạn</h1>
            <span>{cart.itemCount} sản phẩm · {cart.selectedCount} dòng được chọn</span>
          </div>
          {items.length > 0 && (
            <label className="cart-select-all">
              <input type="checkbox" checked={allSelected} disabled={busy === "all"}
                onChange={() => run("all", () => selectAll(!allSelected))} />
              Chọn tất cả
            </label>
          )}
        </header>

        <div className="cart-page__layout">
          <section className="cart-page__items">
            {loading && !items.length ? (
              <div className="cart-empty-state"><LoaderCircle className="animate-spin" size={26} /><p>Đang tải giỏ hàng...</p></div>
            ) : error && !items.length ? (
              <div className="cart-empty-state">
                <ShoppingBag size={30} />
                <h2>Không tải được giỏ hàng</h2>
                <p>{error}</p>
                <button type="button" className="text-amber-400 underline" onClick={refresh}>Thử lại</button>
              </div>
            ) : items.length === 0 ? (
              <div className="cart-empty-state">
                <ShoppingBag size={30} />
                <h2>Giỏ hàng đang trống</h2>
                <p>Khám phá những thiết kế được tuyển chọn và thêm vào giỏ hàng của bạn.</p>
                <Link to="/products">Khám phá sản phẩm</Link>
              </div>
            ) : (
              <div className="cart-item-list">
                {items.map((item) => (
                  <CartItem
                    key={item.variantId}
                    item={item}
                    busy={busy === item.variantId}
                    onToggleCheck={() => run(item.variantId, () => setSelected(item.variantId, !item.selected))}
                    onChangeQty={(quantity) => run(item.variantId, () => setQuantity(item.variantId, quantity))}
                    onRemove={() => run(item.variantId, () => remove(item.variantId))}
                  />
                ))}
              </div>
            )}
          </section>

          <OrderSummary
            subtotal={cart.selectedSubtotal}
            selectedCount={cart.selectedCount}
            canCheckout={cart.canCheckout && !busy}
            loggedIn={loggedIn}
            blockedReason={blockedReason}
          />
        </div>
      </div>
    </div>
  );
}
