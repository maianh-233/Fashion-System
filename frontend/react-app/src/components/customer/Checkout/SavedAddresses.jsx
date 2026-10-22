import { useState } from "react";
import { AlertTriangle, Check, Home, MapPin, Pencil, Plus } from "lucide-react";
import Button from "../../common/Button";
import AddressMapPicker from "./AddressMapPicker";
import { formatAddress } from "../../../pages/customer/storefrontLogic";

const EMPTY_FORM = {
  receiverName: "", receiverPhone: "", province: "", district: "", ward: "", addressLine: "",
  latitude: null, longitude: null, isDefault: false, addressType: "HOME",
};

const fields = [
  { name: "receiverName", label: "Người nhận", placeholder: "Họ và tên", span: "half", required: true, max: 255 },
  { name: "receiverPhone", label: "Số điện thoại", placeholder: "Số điện thoại liên hệ", span: "half", type: "tel", required: true, max: 20 },
  { name: "province", label: "Tỉnh / Thành phố", placeholder: "Tỉnh hoặc thành phố", max: 100 },
  { name: "district", label: "Quận / Huyện", placeholder: "Quận hoặc huyện", max: 100 },
  { name: "ward", label: "Phường / Xã", placeholder: "Phường hoặc xã", max: 100 },
];

const hasLocation = (address) => address.latitude != null && address.longitude != null;

/** Sổ địa chỉ của khách: chọn địa chỉ giao, thêm / sửa địa chỉ (kèm chọn vị trí trên bản đồ). */
export default function SavedAddresses({ addresses, selectedId, onSelect, onSave, loading, eyebrow = "Bước 02" }) {
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const startNew = () => {
    setForm({ ...EMPTY_FORM, isDefault: addresses.length === 0 });
    setError("");
  };
  const startEdit = (address) => {
    setForm({ ...EMPTY_FORM, ...address });
    setError("");
  };
  const change = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  const submit = async (event) => {
    event.preventDefault();
    if (!form.receiverName.trim() || !form.receiverPhone.trim() || !form.addressLine.trim()) {
      setError("Vui lòng nhập người nhận, số điện thoại và địa chỉ chi tiết.");
      return;
    }
    if (!hasLocation(form)) {
      setError("Vui lòng chọn vị trí nhận hàng trên bản đồ để hệ thống chọn cửa hàng gần bạn.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      await onSave(form);
      setForm(null);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="checkout-section checkout-addresses">
      <div className="checkout-section__heading">
        <span><Home size={17} /></span>
        <div><small>{eyebrow}</small><h2>Địa chỉ giao hàng</h2></div>
        {!form && <button type="button" className="checkout-addresses__add" onClick={startNew}><Plus size={14} /> Thêm địa chỉ</button>}
      </div>

      {loading && <p className="checkout-muted">Đang tải sổ địa chỉ...</p>}
      {!loading && !form && addresses.length === 0 && (
        <p className="checkout-muted">Bạn chưa có địa chỉ nào. Thêm địa chỉ để đặt hàng.</p>
      )}

      {!form && addresses.length > 0 && (
        <div className="checkout-addresses__list">
          {addresses.map((address) => {
            const active = selectedId === address.id;
            const located = hasLocation(address);
            return (
              <div key={address.id} className={`checkout-addresses__item ${active ? "is-active" : ""}`}>
                <button type="button" className={active ? "is-active" : ""} onClick={() => onSelect(address)} aria-pressed={active}>
                  <MapPin size={16} />
                  <span>
                    <strong>{address.receiverName}{address.isDefault && <em> · Mặc định</em>}</strong>
                    <small>{address.receiverPhone}</small>
                    <p>{formatAddress(address)}</p>
                    {!located && <small className="checkout-addresses__warn"><AlertTriangle size={12} /> Chưa có vị trí trên bản đồ</small>}
                  </span>
                  <i>{active && <Check size={13} />}</i>
                </button>
                <button type="button" className="checkout-addresses__edit" onClick={() => startEdit(address)} aria-label={`Sửa địa chỉ ${address.receiverName}`}>
                  <Pencil size={13} />
                </button>
              </div>
            );
          })}
        </div>
      )}

      {form && (
        <form className="checkout-address-form" onSubmit={submit} noValidate>
          <div className="checkout-form-grid">
            {fields.map((field) => (
              <label className={field.span === "half" ? "is-half" : ""} key={field.name}>
                <span>{field.label}{field.required && " *"}</span>
                <input type={field.type || "text"} name={field.name} value={form[field.name] || ""} placeholder={field.placeholder}
                  maxLength={field.max} onChange={change} required={field.required} />
              </label>
            ))}
            <label className="is-wide">
              <span>Địa chỉ chi tiết *</span>
              <textarea name="addressLine" value={form.addressLine || ""} placeholder="Số nhà, tên đường, tòa nhà..." onChange={change} rows={2} required />
            </label>
          </div>
          <AddressMapPicker value={form} onChange={(position) => setForm((current) => ({ ...current, ...position }))} />
          <label className="checkout-address-form__default">
            <input type="checkbox" checked={Boolean(form.isDefault)} onChange={(e) => setForm((c) => ({ ...c, isDefault: e.target.checked }))} />
            Đặt làm địa chỉ mặc định
          </label>
          {error && <p className="checkout-error">{error}</p>}
          <div className="checkout-address-form__actions">
            <Button type="button" variant="unstyled" className="checkout-secondary-button" onClick={() => setForm(null)} disabled={saving}>Hủy</Button>
            <Button type="submit" variant="unstyled" className="checkout-primary-button" loading={saving}>Lưu địa chỉ</Button>
          </div>
        </form>
      )}
    </section>
  );
}
