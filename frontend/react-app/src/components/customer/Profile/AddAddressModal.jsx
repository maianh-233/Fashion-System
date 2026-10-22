import Button from "../../common/Button";
import { useEffect, useState } from "react";
import { MapPinned, Save, X } from "lucide-react";
import AddressMapPicker from "../Checkout/AddressMapPicker";

const EMPTY = {
  receiverName: "", receiverPhone: "", province: "", district: "", ward: "", addressLine: "", postalCode: "",
  addressType: "HOME", latitude: null, longitude: null, isDefault: false,
};

/** Thêm / sửa địa chỉ giao hàng, kèm chọn vị trí trên bản đồ (bắt buộc để hệ thống chọn cửa hàng giao). */
export default function AddAddressModal({ address, defaults, onSave, onClose }) {
  const [form, setForm] = useState(() => ({ ...EMPTY, ...defaults, ...address }));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const editing = Boolean(address?.id);
  const handleChange = (e) => setForm((current) => ({ ...current, [e.target.name]: e.target.value }));

  useEffect(() => {
    const closeOnEscape = (event) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  const submit = async (event) => {
    event.preventDefault();
    if (!form.receiverName.trim() || !form.receiverPhone.trim() || !form.addressLine.trim())
      return setError("Vui lòng nhập người nhận, số điện thoại và địa chỉ chi tiết.");
    if (form.latitude == null || form.longitude == null)
      return setError("Vui lòng chọn vị trí nhận hàng trên bản đồ.");
    setSaving(true);
    setError("");
    try {
      await onSave({
        receiverName: form.receiverName.trim(), receiverPhone: form.receiverPhone.trim(),
        province: form.province || null, district: form.district || null, ward: form.ward || null,
        addressLine: form.addressLine.trim(), postalCode: form.postalCode || null, addressType: form.addressType,
        latitude: form.latitude, longitude: form.longitude, isDefault: Boolean(form.isDefault),
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
      <form className="customer-profile-modal__dialog is-address" role="dialog" aria-modal="true" aria-labelledby="add-address-title" onSubmit={submit} noValidate>
        <header className="customer-profile-modal__header">
          <span><MapPinned size={17} /></span>
          <div><small>Delivery address</small><h3 id="add-address-title">{editing ? "Sửa địa chỉ giao hàng" : "Thêm địa chỉ giao hàng"}</h3></div>
          <Button type="button" variant="unstyled" onClick={onClose} aria-label="Đóng"><X size={16} /></Button>
        </header>

        <div className="customer-profile-modal__body">
          <div className="customer-profile-form-grid">
            <label><span>Tên người nhận *</span><input name="receiverName" value={form.receiverName} maxLength={255} onChange={handleChange} placeholder="Nguyễn Văn A" /></label>
            <label><span>Số điện thoại *</span><input name="receiverPhone" type="tel" value={form.receiverPhone} maxLength={20} onChange={handleChange} placeholder="0900 000 000" /></label>
            <label><span>Tỉnh / Thành phố</span><input name="province" value={form.province || ""} maxLength={100} onChange={handleChange} placeholder="TP. Hồ Chí Minh" /></label>
            <label><span>Quận / Huyện</span><input name="district" value={form.district || ""} maxLength={100} onChange={handleChange} placeholder="Quận 1" /></label>
            <label><span>Phường / Xã</span><input name="ward" value={form.ward || ""} maxLength={100} onChange={handleChange} placeholder="Phường Bến Nghé" /></label>
            <label><span>Mã bưu điện</span><input name="postalCode" value={form.postalCode || ""} maxLength={20} onChange={handleChange} placeholder="700000" /></label>
            <label className="is-wide"><span>Địa chỉ chi tiết *</span><textarea name="addressLine" value={form.addressLine} onChange={handleChange} placeholder="Số nhà, tên đường, tòa nhà..." rows={2} /></label>
            <label className="is-wide"><span>Loại địa chỉ</span><select name="addressType" value={form.addressType} onChange={handleChange}><option value="HOME">Nhà riêng</option><option value="WORK">Công ty</option><option value="OTHER">Khác</option></select></label>
          </div>

          <AddressMapPicker value={form} onChange={(position) => setForm((current) => ({ ...current, ...position }))} />
          <label className="checkout-address-form__default">
            <input type="checkbox" checked={Boolean(form.isDefault)} onChange={(e) => setForm((c) => ({ ...c, isDefault: e.target.checked }))} />
            Đặt làm địa chỉ mặc định
          </label>
          {error && <p className="checkout-error">{error}</p>}
        </div>

        <footer className="customer-profile-modal__footer">
          <Button type="button" variant="unstyled" onClick={onClose} disabled={saving}>Hủy</Button>
          <Button type="submit" variant="unstyled" className="is-primary" loading={saving}><Save size={14} /> Lưu địa chỉ</Button>
        </footer>
      </form>
    </div>
  );
}
