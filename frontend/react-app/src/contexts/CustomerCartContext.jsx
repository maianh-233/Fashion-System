import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { cartApi } from "../api/customerApi";
import { storefrontApi } from "../api/storefrontApi";
import { useCustomerSession } from "../hooks/useCustomerSession";

const GUEST_KEY = "lunaria_guest_cart";
const EMPTY = { items: [], itemCount: 0, selectedCount: 0, selectedSubtotal: 0, canCheckout: false };
const CustomerCartContext = createContext(null);

function readGuest() {
  try {
    const items = JSON.parse(window.localStorage.getItem(GUEST_KEY) || "[]");
    return Array.isArray(items) ? items.filter((i) => i?.variantId && i.quantity > 0) : [];
  } catch {
    return [];
  }
}

function writeGuest(items) {
  try {
    if (items.length) window.localStorage.setItem(GUEST_KEY, JSON.stringify(items));
    else window.localStorage.removeItem(GUEST_KEY);
  } catch {
    // Trình duyệt chặn lưu trữ: giỏ chỉ còn trong phiên hiện tại.
  }
}

/** Giỏ của khách chưa đăng nhập: chọn/bỏ chọn giữ trên trình duyệt, giá và tồn kho lấy từ API xem trước. */
async function previewGuest(items) {
  if (!items.length) return EMPTY;
  const cart = await storefrontApi.previewCart(items.map(({ variantId, quantity }) => ({ variantId, quantity })));
  const selected = new Map(items.map((i) => [i.variantId, i.selected !== false]));
  const lines = cart.items.map((line) => ({ ...line, selected: selected.get(line.variantId) ?? true }));
  const chosen = lines.filter((l) => l.selected);
  return {
    ...cart,
    items: lines,
    selectedCount: chosen.length,
    selectedSubtotal: chosen.filter((l) => !l.problem).reduce((sum, l) => sum + Number(l.lineTotal), 0),
    canCheckout: chosen.length > 0 && chosen.every((l) => !l.problem),
  };
}

/**
 * Giỏ hàng phía khách: đã đăng nhập dùng giỏ trên máy chủ; chưa đăng nhập giữ trên trình duyệt và gộp vào giỏ
 * tài khoản ngay khi đăng nhập.
 */
export function CustomerCartProvider({ children }) {
  const { loggedIn } = useCustomerSession();
  const [cart, setCart] = useState(EMPTY);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const apply = useCallback((next) => {
    setCart(next || EMPTY);
    setError("");
    return next;
  }, []);

  const refresh = useCallback(async () => {
    try {
      if (loggedIn) {
        const guest = readGuest();
        if (guest.length) {
          const merged = await cartApi.merge(guest.map(({ variantId, quantity }) => ({ variantId, quantity })));
          writeGuest([]);
          return apply(merged);
        }
        return apply(await cartApi.get());
      }
      return apply(await previewGuest(readGuest()));
    } catch (failure) {
      setError(failure.message);
      return null;
    } finally {
      setLoading(false);
    }
  }, [apply, loggedIn]);

  // Đăng nhập / đăng xuất: tải lại giỏ (gộp giỏ trên trình duyệt nếu vừa đăng nhập).
  useEffect(() => {
    let active = true;
    Promise.resolve().then(() => active && refresh());
    return () => {
      active = false;
    };
  }, [refresh]);

  const mutateGuest = useCallback(async (change) => {
    const next = change(readGuest());
    writeGuest(next);
    return apply(await previewGuest(next));
  }, [apply]);

  const actions = useMemo(() => ({
    refresh,
    add: (variantId, quantity) => (loggedIn ? cartApi.add(variantId, quantity).then(apply)
      : mutateGuest((items) => {
        const existing = items.find((i) => i.variantId === variantId);
        if (existing) return items.map((i) => (i.variantId === variantId
          ? { ...i, quantity: Math.min(99, i.quantity + quantity), selected: true } : i));
        if (items.length >= 50) throw new Error("Giỏ hàng tối đa 50 sản phẩm khác nhau");
        return [...items, { variantId, quantity, selected: true }];
      })),
    setQuantity: (variantId, quantity) => (loggedIn ? cartApi.update(variantId, { quantity }).then(apply)
      : mutateGuest((items) => items.map((i) => (i.variantId === variantId ? { ...i, quantity } : i)))),
    setSelected: (variantId, selected) => (loggedIn ? cartApi.update(variantId, { selected }).then(apply)
      : mutateGuest((items) => items.map((i) => (i.variantId === variantId ? { ...i, selected } : i)))),
    selectAll: (selected) => (loggedIn ? cartApi.selectAll(selected).then(apply)
      : mutateGuest((items) => items.map((i) => ({ ...i, selected })))),
    remove: (variantId) => (loggedIn ? cartApi.remove(variantId).then(apply)
      : mutateGuest((items) => items.filter((i) => i.variantId !== variantId))),
  }), [apply, loggedIn, mutateGuest, refresh]);

  const value = useMemo(() => ({ cart, loading, error, loggedIn, ...actions }), [cart, loading, error, loggedIn, actions]);
  return <CustomerCartContext.Provider value={value}>{children}</CustomerCartContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useCustomerCart() {
  const context = useContext(CustomerCartContext);
  if (!context) throw new Error("useCustomerCart must be used inside CustomerCartProvider");
  return context;
}
