import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { wishlistApi } from "../api/customerApi";
import { useCustomerSession } from "../hooks/useCustomerSession";
import { sanitizeIds, toggleId } from "../pages/customer/wishlistLogic";

const GUEST_KEY = "lunaria_guest_wishlist";
const CustomerWishlistContext = createContext(null);

function readGuest() {
  try {
    return sanitizeIds(JSON.parse(window.localStorage.getItem(GUEST_KEY) || "[]"));
  } catch {
    return [];
  }
}

function writeGuest(ids) {
  try {
    if (ids.length) window.localStorage.setItem(GUEST_KEY, JSON.stringify(ids));
    else window.localStorage.removeItem(GUEST_KEY);
  } catch {
    // Trình duyệt chặn lưu trữ: danh sách chỉ còn trong phiên hiện tại.
  }
}

/**
 * Sản phẩm yêu thích: đã đăng nhập lưu trên máy chủ; chưa đăng nhập giữ trên trình duyệt và gộp vào tài khoản
 * ngay khi đăng nhập. Bấm tim cập nhật ngay trên giao diện, lỗi thì trả lại trạng thái cũ.
 */
export function CustomerWishlistProvider({ children }) {
  const { loggedIn } = useCustomerSession();
  const [ids, setIds] = useState(() => (loggedIn ? [] : readGuest()));
  const [loaded, setLoaded] = useState(false);
  const idsRef = useRef(ids);

  const apply = useCallback((next) => {
    idsRef.current = next;
    setIds(next);
    return next;
  }, []);

  const refresh = useCallback(async () => {
    try {
      if (!loggedIn) return apply(readGuest());
      const guest = readGuest();
      const result = guest.length ? await wishlistApi.merge(guest) : await wishlistApi.get();
      if (guest.length) writeGuest([]);
      return apply(result.productIds || []);
    } catch {
      return idsRef.current;
    } finally {
      setLoaded(true);
    }
  }, [apply, loggedIn]);

  // Đăng nhập / đăng xuất: tải lại (gộp danh sách trên trình duyệt nếu vừa đăng nhập).
  useEffect(() => {
    let active = true;
    Promise.resolve().then(() => active && refresh());
    return () => {
      active = false;
    };
  }, [refresh]);

  const toggle = useCallback(async (productId) => {
    const before = idsRef.current;
    const adding = !before.includes(productId);
    const next = toggleId(before, productId);
    apply(next);
    if (!loggedIn) {
      writeGuest(next);
      return adding;
    }
    try {
      const result = adding ? await wishlistApi.add(productId) : await wishlistApi.remove(productId);
      apply(result.productIds || []);
      return adding;
    } catch (failure) {
      apply(before);
      throw failure;
    }
  }, [apply, loggedIn]);

  const value = useMemo(() => ({
    ids, loaded, count: ids.length, has: (productId) => ids.includes(productId), toggle, refresh,
  }), [ids, loaded, toggle, refresh]);
  return <CustomerWishlistContext.Provider value={value}>{children}</CustomerWishlistContext.Provider>;
}

/** Null khi nằm ngoài trang khách (không có provider): nơi gọi ẩn nút yêu thích. */
// eslint-disable-next-line react-refresh/only-export-components
export function useCustomerWishlist() {
  return useContext(CustomerWishlistContext);
}
