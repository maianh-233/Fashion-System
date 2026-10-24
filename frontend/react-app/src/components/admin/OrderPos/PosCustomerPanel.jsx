import { useState } from "react";
import { UserRound, UserPlus, X } from "lucide-react";
import Button from "../../common/Button";
import { customerLookupApi } from "../../../api/admin/orderApi";
import { customerApi } from "../../../api/admin/managementApi";

/**
 * Khách hàng của đơn: mặc định khách vãng lai. Tìm theo SĐT để gắn khách thành viên (dùng voucher
 * cá nhân); không tìm thấy thì nhân viên có thể lưu nhanh khách mới khi khách đồng ý.
 */
export default function PosCustomerPanel({ storeId, customer, onChange, canCreate }) {
  const [phone, setPhone] = useState("");
  const [error, setError] = useState("");
  const [searching, setSearching] = useState(false);
  const [notFound, setNotFound] = useState(false);
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);

  const search = async event => {
    event.preventDefault();
    const value = phone.trim();
    if (!value) return;
    setSearching(true); setError(""); setNotFound(false); setForm(null);
    try {
      onChange(await customerLookupApi.lookup(value));
      setPhone("");
    } catch (failure) {
      if (failure.status === 404) setNotFound(true);
      else setError(failure.message);
    } finally {
      setSearching(false);
    }
  };

  const save = async event => {
    event.preventDefault();
    setSaving(true); setError("");
    try {
      const created = await customerApi.create({ fullName: form.fullName.trim(), phone: form.phone.trim(), originStoreId: storeId });
      onChange({ id: created.id, fullName: created.fullName, phone: created.phone, customerCode: created.customerCode });
      setForm(null); setNotFound(false); setPhone("");
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  return <section className="pos-card">
    <div className="pos-card__title"><span><UserRound size={18} /> Khách hàng</span>{!customer && <small>Khách vãng lai</small>}</div>
    {customer ? <div className="pos-customer">
      <div className="min-w-0">
        <strong className="block text-sm">{customer.fullName || "Khách hàng"}</strong>
        <span className="pos-muted">{customer.phone}{customer.customerCode ? ` · ${customer.customerCode}` : ""}</span>
      </div>
      <button type="button" className="pos-icon-button" aria-label="Bỏ chọn khách" onClick={() => onChange(null)}><X size={16} /></button>
    </div> : <form className="pos-scan" onSubmit={search}>
      <input className="pos-input" inputMode="tel" value={phone} placeholder="Số điện thoại khách" aria-label="Số điện thoại khách"
        onChange={event => { setPhone(event.target.value); setNotFound(false); setError(""); }} />
      <Button type="submit" variant="secondary" loading={searching} disabled={!phone.trim()}>Tìm</Button>
    </form>}
    {notFound && !form && <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
      <span className="pos-muted">Không tìm thấy khách với SĐT này.</span>
      {canCreate && <Button type="button" variant="ghost" size="sm" onClick={() => setForm({ fullName: "", phone: phone.trim() })}><UserPlus size={14} /> Lưu khách mới</Button>}
    </div>}
    {form && <form className="pos-form-grid" onSubmit={save}>
      <input className="pos-input" required maxLength={255} placeholder="Họ tên khách" aria-label="Họ tên khách" value={form.fullName}
        onChange={event => setForm({ ...form, fullName: event.target.value })} />
      <input className="pos-input" required maxLength={20} inputMode="tel" placeholder="Số điện thoại" aria-label="Số điện thoại khách mới" value={form.phone}
        onChange={event => setForm({ ...form, phone: event.target.value })} />
      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" size="sm" onClick={() => setForm(null)}>Hủy</Button>
        <Button type="submit" variant="primary" size="sm" loading={saving} disabled={!form.fullName.trim() || !form.phone.trim()}>Lưu khách</Button>
      </div>
    </form>}
    {error && <p className="pos-error mt-2">{error}</p>}
  </section>;
}
