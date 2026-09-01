import { useEffect, useMemo, useRef, useState } from "react";
import { LoaderCircle, ShoppingBag, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import CartItem from "../../components/customer/CartComponent/CartItem";
import OrderSummary from "../../components/customer/CartComponent/OrderSummary";

const VALID_PROMOS = {
  SALE20: 670000,
  HELLO50: 500000,
  FREESHIP: 300000,
  SUMMER25: 837500,
  VIP10: 335000,
};

const PAGE_SIZE = 4;
const INITIAL_ITEM_COUNT = 6;
const MAX_CART_ITEMS = 18;

const PRODUCT_TEMPLATES = [
  {
    name: "Áo Thun Oversize Basic",
    brand: "Nike",
    color: "Đen",
    size: "L",
    price: 850000,
    image: "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?q=85&w=700&auto=format&fit=crop",
  },
  {
    name: "Hoodie Premium Local Brand",
    brand: "Local Brand",
    color: "Xám",
    size: "XL",
    price: 1250000,
    image: "https://images.unsplash.com/photo-1556821840-3a63f95609a7?q=85&w=700&auto=format&fit=crop",
  },
  {
    name: "Sơ Mi Linen Tay Dài",
    brand: "Lunaria",
    color: "Trắng kem",
    size: "M",
    price: 790000,
    image: "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?q=85&w=700&auto=format&fit=crop",
  },
  {
    name: "Quần Cargo Ống Rộng",
    brand: "Urban Studio",
    color: "Rêu",
    size: "L",
    price: 990000,
    image: "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?q=85&w=700&auto=format&fit=crop",
  },
];

const createCartItems = (startIndex, count) =>
  Array.from({ length: count }, (_, offset) => {
    const index = startIndex + offset;
    const template = PRODUCT_TEMPLATES[index % PRODUCT_TEMPLATES.length];

    return {
      id: `cart-item-${index + 1}`,
      ...template,
      quantity: index % 3 === 1 ? 2 : 1,
      image: template.image,
      checked: true,
    };
  });

export default function CartPage() {
  const [cart, setCart] = useState(() =>
    createCartItems(0, INITIAL_ITEM_COUNT),
  );

  const [appliedPromos, setAppliedPromos] = useState([]);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const loadMoreRef = useRef(null);
  const loadingRef = useRef(false);
  const hasMore = cart.length < MAX_CART_ITEMS;

  useEffect(() => {
    const target = loadMoreRef.current;
    const isMobile = window.matchMedia("(max-width: 639px)").matches;
    if (!target || !isMobile || !hasMore) return undefined;

    let loadingTimer;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting || loadingRef.current) return;

        loadingRef.current = true;
        setIsLoadingMore(true);
        loadingTimer = window.setTimeout(() => {
          setCart((currentCart) => {
            const remaining = MAX_CART_ITEMS - currentCart.length;
            return [
              ...currentCart,
              ...createCartItems(
                currentCart.length,
                Math.min(PAGE_SIZE, remaining),
              ),
            ];
          });
          setIsLoadingMore(false);
          loadingRef.current = false;
        }, 450);
      },
      { rootMargin: "0px 0px 160px", threshold: 0.1 },
    );

    observer.observe(target);
    return () => {
      observer.disconnect();
      window.clearTimeout(loadingTimer);
    };
  }, [hasMore, cart.length]);

  /* ===== TOTAL ===== */
  const subtotal = useMemo(() => {
    return cart
      .filter((i) => i.checked)
      .reduce((s, i) => s + i.price * i.quantity, 0);
  }, [cart]);

  const totalDiscount = appliedPromos.reduce((s, p) => s + p.amount, 0);
  const total = Math.max(0, subtotal - totalDiscount);
  const selectedCount = cart.filter((item) => item.checked).length;
  const allSelected = cart.length > 0 && selectedCount === cart.length;

  /* ===== HANDLERS ===== */
  const applyPromo = (code) => {
    if (appliedPromos.length >= 3) return false;
    if (appliedPromos.find((p) => p.code === code)) return false;
    if (!VALID_PROMOS[code]) return false;

    setAppliedPromos([...appliedPromos, { code, amount: VALID_PROMOS[code] }]);
    return true;
  };

  const removePromo = (code) => {
    setAppliedPromos(appliedPromos.filter((p) => p.code !== code));
  };

  return (
    <div className="customer-page cart-page min-h-screen text-zinc-200">
      <div className="customer-page__wide cart-page__inner">
        <header className="cart-page__header">
          <div>
            <p><Sparkles size={13} /> Your selection</p>
            <h1>Giỏ hàng của bạn</h1>
            <span>{cart.length} sản phẩm · {selectedCount} sản phẩm được chọn</span>
          </div>
          {cart.length > 0 && (
            <label className="cart-select-all">
              <input
                type="checkbox"
                checked={allSelected}
                onChange={() =>
                  setCart((currentCart) =>
                    currentCart.map((item) => ({ ...item, checked: !allSelected })),
                  )
                }
              />
              Chọn tất cả
            </label>
          )}
        </header>

        <div className="cart-page__layout">
        <section className="cart-page__items">
          {cart.length === 0 ? (
            <div className="cart-empty-state">
              <ShoppingBag size={30} />
              <h2>Giỏ hàng đang trống</h2>
              <p>Khám phá những thiết kế được tuyển chọn và thêm vào giỏ hàng của bạn.</p>
              <Link to="/products">Khám phá sản phẩm</Link>
            </div>
          ) : (
            <div className="cart-item-list">
              {cart.map((item) => (
                <CartItem
                  key={item.id}
                  item={item}
                  onToggleCheck={() =>
                    setCart((currentCart) =>
                      currentCart.map((i) =>
                        i.id === item.id ? { ...i, checked: !i.checked } : i,
                      ),
                    )
                  }
                  onChangeQty={(d) =>
                    setCart((currentCart) =>
                      currentCart.map((i) =>
                        i.id === item.id
                          ? { ...i, quantity: Math.max(1, i.quantity + d) }
                          : i,
                      ),
                    )
                  }
                  onRemove={() =>
                    setCart((currentCart) =>
                      currentCart.filter((i) => i.id !== item.id),
                    )
                  }
                />
              ))}
            </div>
          )}

          <div ref={loadMoreRef} className="cart-load-more sm:hidden" aria-live="polite">
            {isLoadingMore && (
              <span><LoaderCircle className="animate-spin" size={17} /> Đang tải thêm sản phẩm...</span>
            )}
            {!hasMore && cart.length > 0 && <span>Bạn đã xem hết sản phẩm trong giỏ</span>}
          </div>
        </section>

        <OrderSummary
          subtotal={subtotal}
          total={total}
          selectedCount={selectedCount}
          appliedPromos={appliedPromos}
          onApplyPromo={applyPromo}
          onRemovePromo={removePromo}
        />
        </div>
      </div>
    </div>
  );
}
