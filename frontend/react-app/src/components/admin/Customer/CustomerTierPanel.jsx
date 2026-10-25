import { useEffect, useState } from "react";
import { Crown, Pencil, Plus, Trash2 } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../common/AdminDialog";
import Button from "../../common/Button";
import { useSystemNotification } from "../../common/SystemNotification";
import { customerTierApi } from "../../../api/admin/managementApi";
import { DEFAULT_TIER, spendRange, tierForm, tierPayload, validateTier } from "../../../pages/admin/customerTierLogic";

const input = "w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3 py-2 text-zinc-100 disabled:opacity-60";

/**
 * Cấu hình hạng khách hàng: khách tự lên hạng khi tổng chi tiêu đạt ngưỡng (không tự hạ hạng).
 * {@code onChanged} báo cho trang cha tải lại danh sách hạng dùng ở bộ lọc.
 */
export default function CustomerTierPanel({ onChanged }) {
  const notification = useSystemNotification();
  const [tiers, setTiers] = useState([]);
  const [counts, setCounts] = useState({});
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const [editing, setEditing] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    customerTierApi.ordered({ signal: controller.signal })
      .then(async list => {
        const entries = await Promise.all(list.map(tier =>
          customerTierApi.customerCount(tier.id, { signal: controller.signal }).then(total => [tier.id, total], () => [tier.id, null])));
        if (controller.signal.aborted) return;
        setTiers(list);
        setCounts(Object.fromEntries(entries));
        setError("");
      })
      .catch(requestError => { if (!controller.signal.aborted) setError(requestError.message || "Không thể tải hạng khách hàng."); });
    return () => controller.abort();
  }, [revision]);

  const reload = () => { setRevision(value => value + 1); onChanged?.(); };

  const remove = async tier => {
    const accepted = await notification.confirm({
      title: "Xóa hạng khách hàng", message: `Xóa hạng ${tier.name} (${tier.code})? Chỉ xóa được hạng chưa từng gán cho khách.`, destructive: true,
    });
    if (!accepted) return;
    try { await customerTierApi.remove(tier.id); notification.success("Đã xóa hạng khách hàng."); reload(); }
    catch (requestError) { notification.error(requestError); }
  };

  return <section className="space-y-4">
    <div className="flex flex-wrap items-start justify-between gap-3 rounded-3xl border border-zinc-800 bg-zinc-900 p-5">
      <div>
        <h2 className="font-semibold">Hạng khách hàng</h2>
        <p className="mt-1 text-sm text-zinc-400">
          Khách tự lên hạng khi tổng chi tiêu (đơn đã hoàn thành) đạt ngưỡng; hạng không tự hạ. Nhân viên cấp chuỗi có thể đổi hạng thủ công trong chi tiết khách hàng.
        </p>
      </div>
      <Button permission="CUSTOMER_TIER_MANAGE" globalOnly onClick={() => setEditing({ tier: null })}
        className="flex items-center gap-2 rounded-xl bg-amber-500 px-4 py-2 font-semibold text-zinc-950"><Plus size={17}/>Thêm hạng</Button>
    </div>
    {error && <p role="alert" className="rounded-xl bg-rose-500/10 p-3 text-rose-300">{error}</p>}
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {tiers.map((tier, index) => <article key={tier.id} className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <span className="rounded-2xl bg-amber-500/10 p-3 text-amber-400"><Crown size={20}/></span>
            <div>
              <p className="font-semibold">{tier.name}</p>
              <p className="text-xs text-zinc-500">{tier.code}{tier.code === DEFAULT_TIER ? " · mặc định" : ""}</p>
            </div>
          </div>
          <div className="flex gap-2">
            <Button permission="CUSTOMER_TIER_MANAGE" globalOnly title="Sửa" onClick={() => setEditing({ tier })} className="text-amber-300"><Pencil size={17}/></Button>
            {tier.code !== DEFAULT_TIER && <Button permission="CUSTOMER_TIER_MANAGE" globalOnly title="Xóa" onClick={() => remove(tier)} className="text-rose-300"><Trash2 size={17}/></Button>}
          </div>
        </div>
        <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
          <div className="col-span-2"><dt className="text-xs text-zinc-500">Tổng chi tiêu</dt><dd className="mt-1">{spendRange(tiers, index)}</dd></div>
          <div><dt className="text-xs text-zinc-500">Khách hiện tại</dt><dd className="mt-1 text-lg font-semibold">{counts[tier.id] ?? "—"}</dd></div>
          <div><dt className="text-xs text-zinc-500">Ưu đãi ghi nhận</dt><dd className="mt-1 text-lg font-semibold">{Number(tier.discountPercent || 0).toLocaleString("vi-VN")}%</dd></div>
        </dl>
      </article>)}
      {!error && tiers.length === 0 && <p className="text-sm text-zinc-400">Đang tải hạng khách hàng…</p>}
    </div>
    {editing && <TierDialog tier={editing.tier} tiers={tiers} onClose={() => setEditing(null)}
      onSaved={() => { setEditing(null); reload(); }}/>}
  </section>;
}

function TierDialog({ tier, tiers, onClose, onSaved }) {
  const notification = useSystemNotification();
  const [form, setForm] = useState(() => tierForm(tier));
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const isDefault = tier?.code === DEFAULT_TIER;
  const set = key => event => setForm(current => ({ ...current, [key]: event.target.value }));

  const submit = async event => {
    event.preventDefault();
    const message = validateTier(form, tiers, tier?.id || null);
    setError(message);
    if (message) return;
    setBusy(true);
    try {
      if (tier) await customerTierApi.update(tier.id, tierPayload(form)); else await customerTierApi.create(tierPayload(form));
      notification.success(tier ? "Đã cập nhật hạng khách hàng." : "Đã thêm hạng khách hàng.");
      onSaved();
    } catch (requestError) { setError(requestError.message || "Không thể lưu hạng khách hàng."); }
    finally { setBusy(false); }
  };

  return <AdminDialog open onClose={() => !busy && onClose()} size="md">
    <AdminDialogHeader title={tier ? "Cập nhật hạng khách hàng" : "Thêm hạng khách hàng"} description={tier?.code || "Mã hạng viết hoa, không dấu"} onClose={onClose}/>
    <form onSubmit={submit}>
      <AdminDialogBody className="space-y-4">
        <Field label="Mã hạng *"><input className={input} maxLength="50" required disabled={isDefault} value={form.code} onChange={set("code")} placeholder="VD: GOLD"/></Field>
        <Field label="Tên hiển thị *"><input className={input} maxLength="100" required value={form.name} onChange={set("name")} placeholder="VD: Vàng"/></Field>
        <Field label="Ngưỡng tổng chi tiêu (đ) *" hint={isDefault ? "Hạng mặc định luôn bắt đầu từ 0đ." : "Khách đạt mức này sẽ tự lên hạng."}>
          <input className={input} type="number" min="0" step="1000" required disabled={isDefault} value={form.minTotalSpent} onChange={set("minTotalSpent")}/>
        </Field>
        <Field label="Ưu đãi (%)" hint="Chỉ để ghi nhận, chưa tự trừ vào đơn. Muốn giảm giá theo hạng, hãy tạo khuyến mãi áp dụng cho hạng này.">
          <input className={input} type="number" min="0" max="100" step="0.5" value={form.discountPercent} onChange={set("discountPercent")}/>
        </Field>
        {error && <p role="alert" className="rounded-xl bg-rose-500/10 p-3 text-sm text-rose-300">{error}</p>}
      </AdminDialogBody>
      <AdminDialogFooter className="justify-end gap-3">
        <Button type="button" onClick={onClose} disabled={busy}>Hủy</Button>
        <Button type="submit" disabled={busy} className="bg-amber-500 text-zinc-950">{busy ? "Đang lưu…" : "Lưu"}</Button>
      </AdminDialogFooter>
    </form>
  </AdminDialog>;
}

function Field({ label, hint, children }) {
  return <label className="block"><span className="mb-1 block text-sm text-zinc-400">{label}</span>{children}{hint && <span className="mt-1 block text-xs text-zinc-500">{hint}</span>}</label>;
}
