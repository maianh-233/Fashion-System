import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import {
  AlertTriangle,
  BellRing,
  CheckCircle2,
  CircleAlert,
  Info,
  ShieldQuestion,
  X,
} from "lucide-react";
import Button from "./Button";
import "./SystemNotification.css";

const NotificationContext = createContext(null);
const DEFAULT_DURATION = 5000;

const typeConfig = {
  info: { icon: Info, title: "Thông báo" },
  success: { icon: CheckCircle2, title: "Thành công" },
  warning: { icon: AlertTriangle, title: "Cảnh báo" },
  error: { icon: CircleAlert, title: "Đã xảy ra lỗi" },
  reminder: { icon: BellRing, title: "Nhắc nhở" },
  confirm: { icon: ShieldQuestion, title: "Xác nhận thao tác" },
};

function normalizeContent(content, options = {}) {
  if (content instanceof Error) {
    return { ...options, message: content.message || "Không thể hoàn tất thao tác." };
  }
  if (typeof content === "object" && content !== null) return content;
  return { ...options, message: String(content || "") };
}

/** Hiển thị toast đa trạng thái và dialog xác nhận dùng chung cho toàn hệ thống. */
export function SystemNotificationProvider({ children }) {
  const [notifications, setNotifications] = useState([]);
  const [confirmation, setConfirmation] = useState(null);
  const idRef = useRef(0);
  const timersRef = useRef(new Map());

  const dismiss = useCallback((id) => {
    const timer = timersRef.current.get(id);
    if (timer) window.clearTimeout(timer);
    timersRef.current.delete(id);
    setNotifications((current) => current.filter((item) => item.id !== id));
  }, []);

  const show = useCallback((type, content, options = {}) => {
    const safeType = typeConfig[type] && type !== "confirm" ? type : "info";
    const config = normalizeContent(content, options);
    const id = ++idRef.current;
    const notification = {
      id,
      type: safeType,
      title: config.title || typeConfig[safeType].title,
      message: config.message || "",
      actionLabel: config.actionLabel,
      onAction: config.onAction,
    };

    setNotifications((current) => [...current.slice(-4), notification]);
    if (config.duration !== 0) {
      const duration = Number.isFinite(config.duration) ? Math.max(1000, config.duration) : DEFAULT_DURATION;
      timersRef.current.set(id, window.setTimeout(() => dismiss(id), duration));
    }
    return id;
  }, [dismiss]);

  const closeConfirmation = useCallback((accepted) => {
    setConfirmation((current) => {
      current?.resolve(Boolean(accepted));
      return null;
    });
  }, []);

  const confirm = useCallback((content, options = {}) => {
    const config = normalizeContent(content, options);
    return new Promise((resolve) => {
      setConfirmation((current) => {
        current?.resolve(false);
        return {
          type: config.type && typeConfig[config.type] ? config.type : "confirm",
          title: config.title || typeConfig.confirm.title,
          message: config.message || "Bạn có chắc chắn muốn tiếp tục?",
          confirmText: config.confirmText || "Đồng ý",
          cancelText: config.cancelText || "Hủy",
          destructive: Boolean(config.destructive),
          resolve,
        };
      });
    });
  }, []);

  useEffect(() => () => {
    timersRef.current.forEach((timer) => window.clearTimeout(timer));
    timersRef.current.clear();
  }, []);

  useEffect(() => {
    if (!confirmation) return undefined;
    const previousOverflow = document.body.style.overflow;
    const handleKeyDown = (event) => {
      if (event.key === "Escape") closeConfirmation(false);
    };
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [closeConfirmation, confirmation]);

  const api = useMemo(() => ({
    show,
    dismiss,
    info: (content, options) => show("info", content, options),
    success: (content, options) => show("success", content, options),
    warning: (content, options) => show("warning", content, options),
    error: (content, options) => show("error", content, options),
    reminder: (content, options) => show("reminder", content, options),
    confirm,
  }), [confirm, dismiss, show]);

  return (
    <NotificationContext.Provider value={api}>
      {children}

      <div className="system-notification-stack" aria-live="polite" aria-atomic="false">
        {notifications.map((item) => {
          const Icon = typeConfig[item.type].icon;
          return (
            <section
              key={item.id}
              className={`system-notification system-notification--${item.type}`}
              role={item.type === "error" || item.type === "warning" ? "alert" : "status"}
            >
              <span className="system-notification__icon" aria-hidden="true"><Icon size={20} /></span>
              <div className="system-notification__content">
                <strong>{item.title}</strong>
                {item.message && <p>{item.message}</p>}
                {item.actionLabel && item.onAction && (
                  <Button
                    type="button"
                    variant="unstyled"
                    className="system-notification__action"
                    onClick={() => {
                      item.onAction();
                      dismiss(item.id);
                    }}
                  >
                    {item.actionLabel}
                  </Button>
                )}
              </div>
              <Button
                type="button"
                variant="unstyled"
                className="system-notification__close"
                onClick={() => dismiss(item.id)}
                aria-label="Đóng thông báo"
              >
                <X size={17} />
              </Button>
            </section>
          );
        })}
      </div>

      {confirmation && (() => {
        const Icon = typeConfig[confirmation.type]?.icon || ShieldQuestion;
        return (
          <div className="system-confirmation__overlay" onMouseDown={() => closeConfirmation(false)}>
            <section
              className={`system-confirmation system-confirmation--${confirmation.destructive ? "destructive" : confirmation.type}`}
              role="alertdialog"
              aria-modal="true"
              aria-labelledby="system-confirmation-title"
              aria-describedby="system-confirmation-message"
              onMouseDown={(event) => event.stopPropagation()}
            >
              <span className="system-confirmation__icon" aria-hidden="true"><Icon size={25} /></span>
              <div>
                <h2 id="system-confirmation-title">{confirmation.title}</h2>
                <p id="system-confirmation-message">{confirmation.message}</p>
              </div>
              <footer>
                <Button type="button" variant="secondary" onClick={() => closeConfirmation(false)}>
                  {confirmation.cancelText}
                </Button>
                <Button
                  type="button"
                  variant={confirmation.destructive ? "danger-solid" : "primary"}
                  onClick={() => closeConfirmation(true)}
                  autoFocus
                >
                  {confirmation.confirmText}
                </Button>
              </footer>
            </section>
          </div>
        );
      })()}
    </NotificationContext.Provider>
  );
}

/** Trả về API thông báo dùng chung trong các component React. */
// eslint-disable-next-line react-refresh/only-export-components
export function useSystemNotification() {
  const context = useContext(NotificationContext);
  if (!context) throw new Error("useSystemNotification phải được dùng bên trong SystemNotificationProvider");
  return context;
}
