import { useEffect, useRef, useState } from "react";
import { Bell } from "lucide-react";
import { useNavigate } from "react-router-dom";
import Button from "../../components/common/Button";
import { useNotifications } from "./NotificationContext";
import "./NotificationBell.css";

const relativeTime = new Intl.RelativeTimeFormat("vi", { numeric: "auto" });

function formatRelativeTime(value) {
  const elapsedSeconds = Math.round((Date.parse(value) - Date.now()) / 1000);
  const absolute = Math.abs(elapsedSeconds);
  if (absolute < 60) return relativeTime.format(elapsedSeconds, "second");
  if (absolute < 3_600) return relativeTime.format(Math.round(elapsedSeconds / 60), "minute");
  if (absolute < 86_400) return relativeTime.format(Math.round(elapsedSeconds / 3_600), "hour");
  return relativeTime.format(Math.round(elapsedSeconds / 86_400), "day");
}

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);
  const navigate = useNavigate();
  const { items, loading, error, liveDot, hasMore, loadMore, retry, clearLiveDot } = useNotifications();

  useEffect(() => {
    const closeOutside = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) setOpen(false);
    };
    document.addEventListener("mousedown", closeOutside);
    return () => document.removeEventListener("mousedown", closeOutside);
  }, []);

  const toggle = () => {
    setOpen((current) => !current);
    clearLiveDot();
  };

  const select = (notification) => {
    setOpen(false);
    clearLiveDot();
    if (notification.actionUrl) navigate(notification.actionUrl);
  };

  return (
    <div className="admin-header__notification" ref={rootRef}>
      <Button
        variant="unstyled"
        onClick={toggle}
        className="admin-header__icon-button"
        aria-label="Mở thông báo"
        aria-expanded={open}
      >
        <Bell size={18} />
        {liveDot && <span className="notification-bell__live-dot" aria-label="Có thông báo realtime mới" />}
      </Button>

      {open && (
        <section className="admin-header__notification-panel notification-bell__panel" aria-label="Lịch sử thông báo">
          <header className="notification-bell__header">
            <strong>Thông báo</strong>
            <small>Mới nhất trước</small>
          </header>

          <div className="notification-bell__list">
            {!loading && !error && items.length === 0 && (
              <p className="notification-bell__status">Chưa có thông báo.</p>
            )}
            {error && (
              <div className="notification-bell__status">
                <p>{error}</p>
                <Button type="button" variant="unstyled" onClick={retry}>Thử lại</Button>
              </div>
            )}
            {items.map((item) => (
              <Button
                variant="unstyled"
                type="button"
                key={item.id}
                className="notification-bell__item"
                onClick={() => select(item)}
              >
                <strong>{item.title}</strong>
                <span>{item.message}</span>
                <time dateTime={item.createdAt}>{formatRelativeTime(item.createdAt)}</time>
              </Button>
            ))}
            {loading && <p className="notification-bell__status">Đang tải...</p>}
          </div>

          {hasMore && (
            <footer className="notification-bell__footer">
              <Button type="button" variant="unstyled" disabled={loading} onClick={loadMore}>
                Xem thêm
              </Button>
            </footer>
          )}
        </section>
      )}
    </div>
  );
}
