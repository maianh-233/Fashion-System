import Button from "../../common/Button";
import { useEffect, useState } from "react";
import { KeyRound, LockKeyhole, Save, X } from "lucide-react";

/** Đổi mật khẩu (hoặc đặt mật khẩu lần đầu cho tài khoản đăng nhập Google). */
export default function ChangePasswordModal({ hasPassword, onClose, onSave }) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const closeOnEscape = (event) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  const submit = async (event) => {
    event.preventDefault();
    if (hasPassword && !currentPassword) return setError("Vui lòng nhập mật khẩu hiện tại.");
    if (newPassword.length < 8) return setError("Mật khẩu mới cần ít nhất 8 ký tự.");
    if (newPassword !== confirm) return setError("Mật khẩu nhập lại không khớp.");
    setSaving(true);
    setError("");
    try {
      await onSave({ currentPassword: hasPassword ? currentPassword : null, newPassword });
      onClose();
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="customer-profile-modal" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <form className="customer-profile-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="change-password-title" onSubmit={submit} noValidate>
        <header className="customer-profile-modal__header">
          <span><KeyRound size={17} /></span>
          <div><small>Account security</small><h3 id="change-password-title">{hasPassword ? "Đổi mật khẩu" : "Đặt mật khẩu"}</h3></div>
          <Button type="button" variant="unstyled" onClick={onClose} aria-label="Đóng"><X size={16} /></Button>
        </header>

        <div className="customer-profile-modal__body">
          {!hasPassword && <p className="customer-dialog__hint">Tài khoản của bạn đang đăng nhập bằng Google. Đặt mật khẩu để có thể đăng nhập bằng tên đăng nhập.</p>}
          <div className="customer-profile-form-grid">
            {hasPassword && (
              <label className="is-wide"><span>Mật khẩu hiện tại</span><div className="customer-profile-password"><LockKeyhole size={15} /><input type="password" autoComplete="current-password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} /></div></label>
            )}
            <label className="is-wide"><span>Mật khẩu mới <small>8–72 ký tự</small></span><div className="customer-profile-password"><LockKeyhole size={15} /><input type="password" autoComplete="new-password" maxLength={72} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} /></div></label>
            <label className="is-wide"><span>Nhập lại mật khẩu mới</span><div className="customer-profile-password"><LockKeyhole size={15} /><input type="password" autoComplete="new-password" maxLength={72} value={confirm} onChange={(e) => setConfirm(e.target.value)} /></div></label>
          </div>
          {error && <p className="checkout-error">{error}</p>}
        </div>

        <footer className="customer-profile-modal__footer">
          <Button type="button" variant="unstyled" onClick={onClose} disabled={saving}>Hủy</Button>
          <Button type="submit" variant="unstyled" className="is-primary" loading={saving}><Save size={14} /> Lưu mật khẩu</Button>
        </footer>
      </form>
    </div>
  );
}
