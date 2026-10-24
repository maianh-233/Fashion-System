import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { CUSTOMER_SESSION_EVENT, getCustomerToken, refreshCustomerToken } from "../api/http/customerSession";
import { createRealtimeClient } from "./realtimeClient";

const CustomerRealtimeContext = createContext(null);
const customerClient = createRealtimeClient({ refreshToken: refreshCustomerToken });

/**
 * Kết nối realtime của khách (thông báo đơn hàng, tin chat hỗ trợ), độc lập với kết nối của trang quản trị.
 * Kết nối khi khách đăng nhập, ngắt khi đăng xuất.
 */
export function CustomerRealtimeProvider({ children }) {
  const [reconnectSequence, setReconnectSequence] = useState(0);

  useEffect(() => {
    const sync = () => {
      const token = getCustomerToken();
      if (token) customerClient.connect(token);
      else customerClient.disconnect();
    };
    const stopReconnect = customerClient.onReconnect(() => setReconnectSequence((value) => value + 1));
    sync();
    window.addEventListener(CUSTOMER_SESSION_EVENT, sync);
    return () => {
      window.removeEventListener(CUSTOMER_SESSION_EVENT, sync);
      stopReconnect();
      customerClient.disconnect();
    };
  }, []);

  const subscribe = useCallback((filter, handler) => customerClient.subscribe(filter, handler), []);
  const value = useMemo(() => ({ subscribe, reconnectSequence }), [reconnectSequence, subscribe]);
  return <CustomerRealtimeContext.Provider value={value}>{children}</CustomerRealtimeContext.Provider>;
}

/** Realtime của khách; ngoài provider (ví dụ trong test) trả về bản rỗng để component vẫn chạy. */
// eslint-disable-next-line react-refresh/only-export-components
export function useCustomerRealtime() {
  return useContext(CustomerRealtimeContext) || { subscribe: () => () => {}, reconnectSequence: 0 };
}
