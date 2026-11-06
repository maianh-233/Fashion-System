import { useEffect, useRef, useState } from "react";
import { Bell, ChevronLeft, ChevronRight } from "lucide-react";
import { useNavigate } from "react-router-dom";
import Button from "../../components/common/Button";
import { useNotifications } from "./NotificationContext";
import { unreadBadge } from "./notificationState";
import "./NotificationBell.css";

const PAGE_SIZE = 20;
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
  const { items, loading, error, liveDot, unreadCount, markRead, markAllRead, hasMore, totalPages, loadMore, retry, clearLiveDot } = useNotifications();
  // Phân trang bằng nút: mỗi trang 20 thông báo; sang trang chưa tải thì tải thêm từ máy chủ.
  const [pageIndex, setPageIndex] = useState(0);
  const pageCount = Math.max(1, totalPages || 0, Math.ceil(items.length / PAGE_SIZE));
  const current = Math.min(pageIndex, pageCount - 1);
  const visible = items.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);
  const canNext = (current + 1) * PAGE_SIZE < items.length || hasMore;
  const next = async () => {
    if (items.length < (current + 2) * PAGE_SIZE && hasMore) await loadMore();
    setPageIndex(current + 1);
  };
  const badge = unreadBadge(unreadCount);

  useEffect(() => {
    const closeOutside = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) setOpen(false);
    };
    document.addEventListener("mousedown", closeOutside);
    return () => document.removeEventListener("mousedown", closeOutside);
  }, []);

  const toggle = () => {
    setOpen((current) => !current);
    setPageIndex(0);
    clearLiveDot();
  };

  const select = (notification) => {
    setOpen(false);
    clearLiveDot();
    markRead(notification.id);
    if (notification.actionUrl) navigate(notification.actionUrl);
  };

  return (
    <div className="admin-header__notification" ref={rootRef}>
      <Button
        variant="unstyled"
        onClick={toggle}
        className="admin-header__icon-button"
        aria-label={badge ? `Mở thông báo, ${unreadCount} chưa đọc` : "Mở thông báo"}
        aria-expanded={open}
      >
        <Bell size={18} />
        {badge
          ? <span className="notification-bell__badge" aria-hidden="true">{badge}</span>
          : liveDot && <span className="notification-bell__live-dot" aria-label="Có thông báo realtime mới" />}
      </Button>

      {open && (
        <section className="admin-header__notification-panel notification-bell__panel" aria-label="Lịch sử thông báo">
          <header className="notification-bell__header">
            <strong>Thông báo</strong>
            {unreadCount > 0
              ? <Button type="button" variant="unstyled" className="notification-bell__read-all" onClick={markAllRead}>Đọc tất cả</Button>
              : <small>Mới nhất trước</small>}
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
            {visible.map((item) => (
              <Button
                variant="unstyled"
                type="button"
                key={item.id}
                className={`notification-bell__item${item.read ? "" : " is-unread"}`}
                onClick={() => select(item)}
              >
                <strong>{item.title}</strong>
                <span>{item.message}</span>
                <time dateTime={item.createdAt}>{formatRelativeTime(item.createdAt)}</time>
              </Button>
            ))}
            {loading && <p className="notification-bell__status">Đang tải...</p>}
          </div>

          {pageCount > 1 && (
            <footer className="notification-bell__footer notification-bell__pager">
              <Button type="button" variant="unstyled" disabled={loading || current === 0} onClick={() => setPageIndex(current - 1)} aria-label="Trang thông báo trước">
                <ChevronLeft size={15} aria-hidden="true" />Trước
              </Button>
              <span aria-live="polite">Trang {current + 1} / {pageCount}</span>
              <Button type="button" variant="unstyled" disabled={loading || !canNext} onClick={next} aria-label="Trang thông báo sau">
                Sau<ChevronRight size={15} aria-hidden="true" />
              </Button>
            </footer>
          )}
        </section>
      )}
    </div>
  );
}
