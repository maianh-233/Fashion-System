import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { getAdminSession, refreshAccessToken } from "../api/http/adminSession";
import { createRealtimeClient } from "./realtimeClient";

const RealtimeContext = createContext(null);
const realtimeClient = createRealtimeClient({ refreshToken: refreshAccessToken });

/** Owns the application's single admin realtime connection and generic event dispatcher. */
export function RealtimeProvider({ children }) {
  const [reconnectSequence, setReconnectSequence] = useState(0);

  useEffect(() => {
    const syncConnection = () => {
      const token = getAdminSession()?.token;
      if (token) realtimeClient.connect(token);
      else realtimeClient.disconnect();
    };
    const stopReconnectListener = realtimeClient.onReconnect(() => {
      setReconnectSequence((value) => value + 1);
    });
    syncConnection();
    window.addEventListener("lunaria:admin-session", syncConnection);
    return () => {
      window.removeEventListener("lunaria:admin-session", syncConnection);
      stopReconnectListener();
      realtimeClient.disconnect();
    };
  }, []);

  const subscribe = useCallback((filter, handler) => realtimeClient.subscribe(filter, handler), []);
  const value = useMemo(
    () => ({ subscribe, reconnectSequence }),
    [reconnectSequence, subscribe],
  );

  return <RealtimeContext.Provider value={value}>{children}</RealtimeContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useRealtime() {
  const context = useContext(RealtimeContext);
  if (!context) throw new Error("useRealtime phải được dùng bên trong RealtimeProvider");
  return context;
}
