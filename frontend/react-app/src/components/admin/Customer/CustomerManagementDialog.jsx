import { useEffect, useState } from "react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../common/AdminDialog";
import Button from "../../common/Button";
import StoreSelector from "../../common/StoreSelector";
import { customerApi } from "../../../api/adminManagementApi";

const empty = { fullName: "", phone: "", email: "", birthday: "", gender: "", note: "", originStoreId: "" };
const input = "w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3 py-2 text-zinc-100 disabled:opacity-60";

export default function CustomerManagementDialog({ mode, customer, stores, isGlobal, currentStoreId, busy, onClose, onSave }) {
  const view = mode === "view";
  const create = mode === "create";
  const [form, setForm] = useState(() => customer ? {
    fullName: customer.fullName || "", phone: customer.phone || "", email: customer.email || "",
    birthday: customer.birthday || "", gender: customer.gender || "", note: customer.note || "",
    originStoreId: customer.originStoreId || currentStoreId || "",
  } : { ...empty, originStoreId: currentStoreId || "" });
  const [addresses, setAddresses] = useState([]);
  const [addressError, setAddressError] = useState("");

  useEffect(() => {
    if (!view || !customer?.id) return undefined;
    const controller = new AbortController();
    customerApi.addresses(customer.id, { signal: controller.signal })
      .then(result => setAddresses(result.content || []))
      .catch(error => { if (!controller.signal.aborted) setAddressError(error.message); });
    return () => controller.abort();
  }, [customer?.id, view]);

  const set = key => event => setForm(current => ({ ...current, [key]: event.target.value }));
  const submit = event => {
    event.preventDefault();
    onSave({ ...form, email: form.email || null, birthday: form.birthday || null, gender: form.gender || null,
      note: form.note || null, originStoreId: create && isGlobal ? form.originStoreId || null : undefined });
  };
  return <AdminDialog open onClose={onClose} size="lg">
    <AdminDialogHeader title={create ? "Thêm Store Member" : view ? "Chi tiết khách hàng" : "Cập nhật khách hàng"}
      description={customer?.customerCode || "Customer code được sinh ở backend"} onClose={onClose} />
    <form onSubmit={submit}>
      <AdminDialogBody className="space-y-6">
        {customer && <section className="grid gap-3 rounded-2xl border border-zinc-800 p-4 sm:grid-cols-3">
          <Meta label="Nguồn" value={customer.source} /><Meta label="Hạng" value={customer.tier || "REGULAR"} />
          <Meta label="Web Account" value={customer.hasWebAccount ? "Đã liên kết" : "Chưa liên kết"} />
          <Meta label="Cửa hàng nguồn" value={customer.originStoreName || "Website / Không có"} />
          <Meta label="Trạng thái" value={customer.active ? "Hoạt động" : "Ngưng hoạt động"} />
        </section>}
        <section><h3 className="mb-3 font-semibold">Thông tin khách hàng</h3><div className="grid gap-4 sm:grid-cols-2">
          <Field label="Họ tên *"><input className={input} required maxLength="255" disabled={view} value={form.fullName} onChange={set("fullName")} /></Field>
          <Field label="Số điện thoại *"><input className={input} required={create} maxLength="20" disabled={view} value={form.phone} onChange={set("phone")} /></Field>
          <Field label="Email"><input className={input} type="email" maxLength="255" disabled={view} value={form.email} onChange={set("email")} /></Field>
          <Field label="Ngày sinh"><input className={input} type="date" disabled={view} value={form.birthday} onChange={set("birthday")} /></Field>
          <Field label="Giới tính"><select className={input} disabled={view} value={form.gender} onChange={set("gender")}><option value="">Chưa chọn</option><option value="FEMALE">Nữ</option><option value="MALE">Nam</option><option value="OTHER">Khác</option></select></Field>
          {create && <StoreSelector stores={stores} value={form.originStoreId} onChange={value => setForm(current => ({ ...current, originStoreId: value }))}
            required readOnly={!isGlobal} label="Cửa hàng nguồn *" className={input} />}
          <Field label="Ghi chú" wide><textarea className={input} rows="3" maxLength="2000" disabled={view} value={form.note} onChange={set("note")} /></Field>
        </div></section>
        {view && <section><h3 className="mb-3 font-semibold">Địa chỉ</h3>{addressError ? <p className="text-rose-300">{addressError}</p> : addresses.length === 0 ? <p className="text-zinc-400">Chưa có địa chỉ.</p> : <div className="space-y-2">{addresses.map(address => <article key={address.id} className="rounded-xl border border-zinc-800 p-3"><div className="flex justify-between"><strong>{address.receiverName}</strong><span className="text-xs text-amber-300">{address.managementSource === "WEB" ? "Website · chỉ xem" : "Cửa hàng"}</span></div><p className="text-sm text-zinc-300">{address.addressLine}, {[address.ward, address.district, address.province].filter(Boolean).join(", ")}</p><p className="text-xs text-zinc-400">{address.receiverPhone}</p></article>)}</div>}</section>}
      </AdminDialogBody>
      <AdminDialogFooter className="justify-end gap-3"><Button type="button" onClick={onClose}>Đóng</Button>{!view && <Button type="submit" disabled={busy} className="bg-amber-500 text-zinc-950">{busy ? "Đang lưu…" : "Lưu"}</Button>}</AdminDialogFooter>
    </form>
  </AdminDialog>;
}

function Field({ label, wide, children }) { return <label className={wide ? "sm:col-span-2" : ""}><span className="mb-1 block text-sm text-zinc-400">{label}</span>{children}</label>; }
function Meta({ label, value }) { return <div><p className="text-xs text-zinc-500">{label}</p><p className="mt-1 text-sm">{value || "—"}</p></div>; }
