import { useEffect, useMemo, useRef, useState } from "react";
import { LoaderCircle, ShoppingBag } from "lucide-react";
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
  },
  {
    name: "Hoodie Premium Local Brand",
    brand: "Local Brand",
    color: "Xám",
    size: "XL",
    price: 1250000,
  },
  {
    name: "Sơ Mi Linen Tay Dài",
    brand: "Lunaria",
    color: "Trắng kem",
    size: "M",
    price: 790000,
  },
  {
    name: "Quần Cargo Ống Rộng",
    brand: "Urban Studio",
    color: "Rêu",
    size: "L",
    price: 990000,
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
      image: `https://picsum.photos/300/300?random=${index + 1}`,
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

  /* ===== HANDLERS ===== */
  const applyPromo = (code) => {
    if (appliedPromos.length >= 3) return;
    if (appliedPromos.find((p) => p.code === code)) return;
    if (!VALID_PROMOS[code]) return;

    setAppliedPromos([...appliedPromos, { code, amount: VALID_PROMOS[code] }]);
  };

  const removePromo = (code) => {
    setAppliedPromos(appliedPromos.filter((p) => p.code !== code));
  };

  return (
    <div className="min-h-screen bg-zinc-950 px-4 py-5 text-zinc-200 sm:px-6 sm:py-8 lg:px-10 xl:px-16 2xl:px-24">
      <div className="mx-auto flex w-full max-w-[1440px] flex-col gap-6 lg:flex-row lg:items-start lg:gap-10">
      <section className="min-w-0 flex-1">
        <div className="mb-5 flex items-center gap-3">
          <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-amber-400/10 text-amber-400">
            <ShoppingBag size={21} />
          </span>
          <div>
            <h1 className="text-xl font-semibold sm:text-2xl">Giỏ hàng</h1>
            <p className="text-sm text-zinc-400">{cart.length} sản phẩm</p>
          </div>
        </div>

        <div className="space-y-3 sm:space-y-4">
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

          <div
            ref={loadMoreRef}
            className="flex min-h-16 items-center justify-center sm:hidden"
            aria-live="polite"
          >
            {isLoadingMore && (
              <span className="flex items-center gap-2 text-sm text-zinc-400">
                <LoaderCircle className="animate-spin" size={18} />
                Đang tải thêm sản phẩm...
              </span>
            )}
            {!hasMore && cart.length > 0 && (
              <span className="text-sm text-zinc-500">
                Bạn đã xem hết sản phẩm trong giỏ
              </span>
            )}
          </div>
        </div>
      </section>

      <OrderSummary
        subtotal={subtotal}
        total={total}
        appliedPromos={appliedPromos}
        onApplyPromo={applyPromo}
        onRemovePromo={removePromo}
      />
      </div>
    </div>
  );
}
