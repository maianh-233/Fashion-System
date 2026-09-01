import Button from "../../common/Button";
import { useEffect, useState } from "react";
import { MapPinned, Plus, ShieldCheck, X } from "lucide-react";

export default function AddAddressModal({ userId, phone = "", onAdd, onClose }) {
  const [form, setForm] = useState({
    receiver_name: "",
    receiver_phone: phone,
    province: "",
    district: "",
    ward: "",
    address_line: "",
    postal_code: "",
    address_type: "HOME",
  });

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const submit = () => {
    if (!form.receiver_name.trim() || !form.receiver_phone.trim()) return;

    onAdd({
      id: crypto.randomUUID(),
      user_id: userId,
      ...form,
      is_default: false,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    });

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
      <section className="customer-profile-modal__dialog is-address" role="dialog" aria-modal="true" aria-labelledby="add-address-title">
        <header className="customer-profile-modal__header">
          <span><MapPinned size={17} /></span>
          <div><small>Delivery address</small><h3 id="add-address-title">Thêm địa chỉ giao hàng</h3></div>
          <Button type="button" variant="unstyled" onClick={onClose} aria-label="Đóng"><X size={16} /></Button>
        </header>

        <div className="customer-profile-modal__body">
          <div className="customer-profile-form-grid">
            <label><span>Tên người nhận</span><input name="receiver_name" value={form.receiver_name} onChange={handleChange} placeholder="Nguyễn Văn A" /></label>
            <label><span>Số điện thoại</span><input name="receiver_phone" type="tel" value={form.receiver_phone} onChange={handleChange} placeholder="0900 000 000" /></label>
            <label><span>Tỉnh / Thành phố</span><input name="province" value={form.province} onChange={handleChange} placeholder="TP. Hồ Chí Minh" /></label>
            <label><span>Quận / Huyện</span><input name="district" value={form.district} onChange={handleChange} placeholder="Quận 1" /></label>
            <label><span>Phường / Xã</span><input name="ward" value={form.ward} onChange={handleChange} placeholder="Phường Bến Nghé" /></label>
            <label><span>Mã bưu điện</span><input name="postal_code" value={form.postal_code} onChange={handleChange} placeholder="700000" /></label>
            <label className="is-wide"><span>Địa chỉ chi tiết</span><textarea name="address_line" value={form.address_line} onChange={handleChange} placeholder="Số nhà, tên đường, tòa nhà..." rows={3} /></label>
            <label className="is-wide"><span>Loại địa chỉ</span><select name="address_type" value={form.address_type} onChange={handleChange}><option value="HOME">Nhà riêng</option><option value="WORK">Công ty</option><option value="OTHER">Khác</option></select></label>
          </div>

          <div className="customer-address-map-placeholder">
            <MapPinned size={20} />
            <span><strong>Định vị bản đồ</strong><small>Sẽ được tích hợp khi kết nối dịch vụ giao hàng</small></span>
            <ShieldCheck size={15} />
          </div>
        </div>

        <footer className="customer-profile-modal__footer">
          <Button type="button" variant="unstyled" onClick={onClose}>Hủy</Button>
          <Button type="button" variant="unstyled" className="is-primary" onClick={submit}><Plus size={14} /> Lưu địa chỉ</Button>
        </footer>
      </section>
    </div>
  );
}
