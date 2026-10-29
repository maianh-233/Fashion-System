import Button from "../../common/Button";
import { useEffect, useRef, useState } from "react";
import { Calendar, LockKeyhole, Save, UserRound, X } from "lucide-react";

/**
 * Sửa hồ sơ; đổi email cần mật khẩu hiện tại (email cũng dùng để nhận mã quên mật khẩu). Ngày sinh đã lưu thì
 * khóa, chỉ cửa hàng sửa được.
 */
export default function EditProfileModal({ profile, onClose, onSave }) {
  const [form, setForm] = useState({
    fullName: profile.fullName || "", phone: profile.phone || "", email: profile.email || "",
    dateOfBirth: profile.dateOfBirth || "", gender: profile.gender || "",
  });
  const [currentPassword, setCurrentPassword] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const dateRef = useRef(null);
  // Ngày sinh quyết định voucher sinh nhật nên khách chỉ tự nhập một lần.
  const birthdayLocked = Boolean(profile.dateOfBirth);
  const emailChanged = form.email.trim().toLowerCase() !== (profile.email || "").toLowerCase();
  const needsPassword = emailChanged && profile.hasPassword;
  const set = (field) => (event) => setForm((current) => ({ ...current, [field]: event.target.value }));

  useEffect(() => {
    const closeOnEscape = (event) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  const submit = async (event) => {
    event.preventDefault();
    if (form.fullName.trim().length < 2) return setError("Vui lòng nhập họ tên.");
    if (!form.phone.trim()) return setError("Vui lòng nhập số điện thoại.");
    if (needsPassword && !currentPassword) return setError("Vui lòng nhập mật khẩu hiện tại để đổi email.");
    setSaving(true);
    setError("");
    try {
      await onSave({
        fullName: form.fullName.trim(), phone: form.phone.trim(), email: form.email.trim() || null,
        dateOfBirth: form.dateOfBirth || null, gender: form.gender || null,
        currentPassword: needsPassword ? currentPassword : null,
      });
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
      <form className="customer-profile-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="edit-profile-title" onSubmit={submit} noValidate>
        <header className="customer-profile-modal__header">
          <span><UserRound size={17} /></span>
          <div><small>Personal details</small><h3 id="edit-profile-title">Chỉnh sửa hồ sơ</h3></div>
          <Button type="button" variant="unstyled" onClick={onClose} aria-label="Đóng"><X size={16} /></Button>
        </header>

        <div className="customer-profile-modal__body">
          <div className="customer-profile-form-grid">
            <label className="is-wide"><span>Họ và tên *</span><input type="text" value={form.fullName} maxLength={255} onChange={set("fullName")} required /></label>
            <label><span>Email</span><input type="email" value={form.email} maxLength={255} onChange={set("email")} /></label>
            <label><span>Số điện thoại *</span><input type="tel" value={form.phone} maxLength={20} onChange={set("phone")} required /></label>
            <label>
              <span>Ngày sinh <small>{birthdayLocked ? "Liên hệ cửa hàng nếu cần sửa" : "Chỉ lưu được một lần, dùng để tặng voucher sinh nhật"}</small></span>
              <div className="customer-profile-date"><input ref={dateRef} type="date" value={form.dateOfBirth} disabled={birthdayLocked} max={new Date().toISOString().slice(0, 10)} onChange={set("dateOfBirth")} /><button type="button" disabled={birthdayLocked} onClick={() => dateRef.current?.showPicker?.()} aria-label="Chọn ngày sinh"><Calendar size={16} /></button></div>
            </label>
            <label><span>Giới tính</span><select value={form.gender} onChange={set("gender")}><option value="">Không muốn nêu</option><option value="FEMALE">Nữ</option><option value="MALE">Nam</option><option value="OTHER">Khác</option></select></label>
            {needsPassword && (
              <label className="is-wide"><span>Mật khẩu hiện tại <small>Bắt buộc khi đổi email</small></span><div className="customer-profile-password"><LockKeyhole size={15} /><input type="password" autoComplete="current-password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} /></div></label>
            )}
          </div>
          {error && <p className="checkout-error">{error}</p>}
        </div>

        <footer className="customer-profile-modal__footer">
          <Button type="button" variant="unstyled" onClick={onClose} disabled={saving}>Hủy</Button>
          <Button type="submit" variant="unstyled" className="is-primary" loading={saving}><Save size={14} /> Lưu thay đổi</Button>
        </footer>
      </form>
    </div>
  );
}
