import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getNotifications } from "../../api/notificationsApi";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import { useRealtime } from "../../realtime/RealtimeContext";
import { mergeNotifications, notificationFromRealtime } from "./notificationState";

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(-1);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [liveDot, setLiveDot] = useState(false);
  const itemsRef = useRef([]);
  const { status } = useAdminAuth();
  const { subscribe, reconnectSequence } = useRealtime();
  const toast = useSystemNotification();
  const navigate = useNavigate();

  const mergeItems = useCallback((incoming) => {
    const merged = mergeNotifications(itemsRef.current, incoming);
    itemsRef.current = merged;
    setItems(merged);
  }, []);

  const loadPage = useCallback(async (requestedPage) => {
    setLoading(true);
    setError("");
    try {
      const response = await getNotifications(requestedPage, 20);
      mergeItems(Array.isArray(response?.content) ? response.content : []);
      setPage((current) => Math.max(current, Number(response?.number ?? requestedPage)));
      setTotalPages(Number(response?.totalPages ?? 0));
    } catch (requestError) {
      setError(requestError?.message || "Không thể tải lịch sử thông báo.");
    } finally {
      setLoading(false);
    }
  }, [mergeItems]);

  useEffect(() => {
    let cancelled = false;
    Promise.resolve().then(() => {
      if (cancelled) return;
      if (status === "authenticated") {
        void loadPage(0);
      } else if (status === "unauthenticated") {
        itemsRef.current = [];
        setItems([]);
        setPage(-1);
        setTotalPages(0);
        setLiveDot(false);
        setError("");
      }
    });
    return () => {
      cancelled = true;
    };
  }, [loadPage, status]);

  useEffect(() => {
    if (status !== "authenticated") return undefined;
    return subscribe(
      { channel: "NOTIFICATION", eventType: "NOTIFICATION_CREATED" },
      (event) => {
        const notification = notificationFromRealtime(event);
        if (!notification || itemsRef.current.some((item) => item.id === notification.id)) return;
        mergeItems([notification]);
        setLiveDot(true);
        toast.reminder({
          title: notification.title,
          message: notification.message,
          actionLabel: notification.actionUrl ? "Mở" : undefined,
          onAction: notification.actionUrl ? () => navigate(notification.actionUrl) : undefined,
        });
      },
    );
  }, [mergeItems, navigate, status, subscribe, toast]);

  useEffect(() => {
    if (status !== "authenticated" || reconnectSequence <= 0) return undefined;
    let cancelled = false;
    Promise.resolve().then(() => {
      if (!cancelled) void loadPage(0);
    });
    return () => {
      cancelled = true;
    };
  }, [loadPage, reconnectSequence, status]);

  const loadMore = useCallback(() => {
    if (!loading && page + 1 < totalPages) return loadPage(page + 1);
    return Promise.resolve();
  }, [loadPage, loading, page, totalPages]);

  const value = useMemo(() => ({
    items,
    loading,
    error,
    liveDot,
    hasMore: page + 1 < totalPages,
    loadMore,
    retry: () => loadPage(0),
    clearLiveDot: () => setLiveDot(false),
  }), [error, items, liveDot, loadMore, loading, page, totalPages, loadPage]);

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) throw new Error("useNotifications phải được dùng bên trong NotificationProvider");
  return context;
}
