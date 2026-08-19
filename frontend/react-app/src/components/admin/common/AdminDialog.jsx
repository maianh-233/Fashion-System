import { useEffect, useId } from "react";
import { X } from "lucide-react";
import Button from "../../common/Button";

export default function AdminDialog({
  open = true,
  onClose,
  size = "md",
  children,
  className = "",
  closeOnBackdrop = true,
}) {
  useEffect(() => {
    if (!open) return undefined;

    const previousOverflow = document.body.style.overflow;
    const handleKeyDown = (event) => {
      if (event.key === "Escape") onClose?.();
    };

    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open, onClose]);

  if (!open) return null;

  const handleBackdrop = (event) => {
    if (closeOnBackdrop && event.target === event.currentTarget) onClose?.();
  };

  return (
    <div className="admin-dialog__overlay" onMouseDown={handleBackdrop}>
      <section
        role="dialog"
        aria-modal="true"
        className={`admin-dialog admin-dialog--${size} ${className}`}
        onMouseDown={(event) => event.stopPropagation()}
      >
        {children}
      </section>
    </div>
  );
}

export function AdminDialogHeader({ title, description, onClose, children }) {
  const titleId = useId();

  return (
    <header className="admin-dialog__header">
      <div className="min-w-0">
        {title && <h2 id={titleId} className="admin-dialog__title">{title}</h2>}
        {description && <p className="admin-dialog__description">{description}</p>}
        {children}
      </div>

      {onClose && (
        <Button
          type="button"
          variant="ghost"
          onClick={onClose}
          className="admin-dialog__close"
          aria-label="Đóng hộp thoại"
          title="Đóng"
        >
          <X size={20} />
        </Button>
      )}
    </header>
  );
}

export function AdminDialogBody({ children, className = "" }) {
  return <div className={`admin-dialog__body ${className}`}>{children}</div>;
}

export function AdminDialogFooter({ children, className = "" }) {
  return <footer className={`admin-dialog__footer ${className}`}>{children}</footer>;
}
