import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bell, CheckCheck, LoaderCircle } from "lucide-react";
import { customerNotificationApi } from "../../api/customerApi";
import { useCustomerRealtime } from "../../realtime/CustomerRealtimeContext";
import {
  badgeLabel, markReadLocally, notificationTarget, prependNotification, relativeTime,
} from "../../pages/customer/customerNotificationLogic";

const PAGE_SIZE = 10;

/** Chuông thông báo của khách: số chưa đọc (realtime), danh sách gần nhất, mở đơn liên quan. */
export default function CustomerNotificationBell() {
  const navigate = useNavigate();
  const { subscribe, reconnectSequence } = useCustomerRealtime();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [items, setItems] = useState(null);
  const [error, setError] = useState("");
  const ref = useRef(null);

  const loadCount = useCallback(() => {
    customerNotificationApi.unreadCount().then((data) => setUnread(data.count || 0)).catch(() => {});
  }, []);

  // Số chưa đọc lúc mở trang và mỗi lần nối lại realtime (có thể đã lỡ sự kiện khi mất kết nối).
  useEffect(() => {
    let active = true;
    Promise.resolve().then(() => active && loadCount());
    return () => {
      active = false;
    };
  }, [loadCount, reconnectSequence]);

  useEffect(() => subscribe({ channel: "NOTIFICATION", eventType: "NOTIFICATION_CREATED" }, (event) => {
    const notification = event?.payload;
    if (!notification) return;
    setUnread((value) => value + (notification.read ? 0 : 1));
    setItems((current) => (current ? prependNotification(current, notification, PAGE_SIZE) : current));
  }), [subscribe]);

  useEffect(() => {
    if (!open) return undefined;
    const close = (event) => { if (!ref.current?.contains(event.target)) setOpen(false); };
    const onKey = (event) => { if (event.key === "Escape") setOpen(false); };
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const toggle = () => {
    const next = !open;
    setOpen(next);
    if (next) {
      setError("");
      customerNotificationApi.list(0, PAGE_SIZE)
        .then((page) => setItems(page.content || []))
        .catch((failure) => setError(failure.message));
      loadCount();
    }
  };

  const openItem = async (notification) => {
    setOpen(false);
    if (!notification.read) {
      setItems((current) => markReadLocally(current, notification.id));
      setUnread((value) => Math.max(0, value - 1));
      customerNotificationApi.markRead(notification.id).catch(() => loadCount());
    }
    navigate(notificationTarget(notification));
  };

  const readAll = async () => {
    setItems((current) => markReadLocally(current));
    setUnread(0);
    customerNotificationApi.markAllRead().catch(() => loadCount());
  };

  return (
    <div ref={ref} className="customer-bell">
      <button type="button" className="storefront-header__icon customer-bell__trigger" onClick={toggle}
        aria-expanded={open} aria-label={unread ? `Thông báo, ${unread} chưa đọc` : "Thông báo"}>
        <Bell size={18} />
        {unread > 0 && <span className="storefront-cart__count">{badgeLabel(unread)}</span>}
      </button>

      {open && (
        <div className="customer-bell__panel" role="dialog" aria-label="Thông báo">
          <header>
            <strong>Thông báo</strong>
            <button type="button" onClick={readAll} disabled={!unread}><CheckCheck size={14} /> Đã đọc tất cả</button>
          </header>
          {!items && !error && <p className="customer-bell__empty"><LoaderCircle className="animate-spin" size={15} /> Đang tải...</p>}
          {error && <p className="customer-bell__empty">{error}</p>}
          {items && items.length === 0 && <p className="customer-bell__empty">Bạn chưa có thông báo nào.</p>}
          {items && items.length > 0 && (
            <ul>
              {items.map((item) => (
                <li key={item.id}>
                  <button type="button" className={item.read ? "" : "is-unread"} onClick={() => openItem(item)}>
                    <strong>{item.title}</strong>
                    {item.message && <span>{item.message}</span>}
                    <small>{relativeTime(item.createdAt)}</small>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
