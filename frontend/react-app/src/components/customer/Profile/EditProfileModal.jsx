import Button from "../../common/Button";
import { useEffect, useRef, useState } from "react";
import { Calendar, LockKeyhole, Save, UserRound, X } from "lucide-react";

export default function EditProfileModal({ user, onClose, onSave }) {
  const [form, setForm] = useState(user);
  const [password, setPassword] = useState("");
  const dateRef = useRef(null);

  const submit = () => {
    onSave(password ? { ...form, password } : form);
    onClose();
  };

  useEffect(() => {
    const closeOnEscape = (event) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  return (
    <div className="customer-profile-modal" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <section className="customer-profile-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="edit-profile-title">
        <header className="customer-profile-modal__header">
          <span><UserRound size={17} /></span>
          <div><small>Personal details</small><h3 id="edit-profile-title">Chỉnh sửa hồ sơ</h3></div>
          <Button type="button" variant="unstyled" onClick={onClose} aria-label="Đóng"><X size={16} /></Button>
        </header>

        <div className="customer-profile-modal__body">
          <div className="customer-profile-form-grid">
            <label className="is-wide"><span>Họ và tên</span><input type="text" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
            <label><span>Email</span><input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
            <label><span>Số điện thoại</span><input type="tel" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></label>
            <label className="is-wide"><span>Ngày sinh</span><div className="customer-profile-date"><input ref={dateRef} type="date" value={form.birthDate} onChange={(e) => setForm({ ...form, birthDate: e.target.value })} /><button type="button" onClick={() => dateRef.current?.showPicker()} aria-label="Chọn ngày sinh"><Calendar size={16} /></button></div></label>
            <label className="is-wide"><span>Mật khẩu mới <small>Không bắt buộc</small></span><div className="customer-profile-password"><LockKeyhole size={15} /><input type="password" placeholder="Để trống nếu không muốn thay đổi" value={password} onChange={(e) => setPassword(e.target.value)} /></div></label>
          </div>
        </div>

        <footer className="customer-profile-modal__footer">
          <Button type="button" variant="unstyled" onClick={onClose}>Hủy</Button>
          <Button type="button" variant="unstyled" className="is-primary" onClick={submit}><Save size={14} /> Lưu thay đổi</Button>
        </footer>
      </section>
    </div>
  );
}
