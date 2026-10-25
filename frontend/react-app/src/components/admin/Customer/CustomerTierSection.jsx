import { useEffect, useState } from "react";
import Button from "../../common/Button";
import { useSystemNotification } from "../../common/SystemNotification";
import { customerApi } from "../../../api/admin/managementApi";

const input = "rounded-xl border border-zinc-700 bg-zinc-800 px-3 py-2 text-zinc-100";
const dateTime = value => (value ? new Date(value).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" }) : "");

/**
 * Hạng hiện tại và lịch sử hạng của một khách; {@code canManage} (nhân viên cấp chuỗi có CUSTOMER_TIER_MANAGE)
 * được đổi hạng thủ công. Hạng vẫn có thể tự tăng sau đó khi tổng chi tiêu đạt ngưỡng cao hơn.
 */
export default function CustomerTierSection({ customerId, tiers, canManage, onChanged }) {
  const notification = useSystemNotification();
  const [history, setHistory] = useState([]);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);
  const [tierId, setTierId] = useState("");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const names = Object.fromEntries(tiers.map(tier => [tier.id, `${tier.name} (${tier.code})`]));
  const current = history.find(item => !item.expiresAt);

  useEffect(() => {
    const controller = new AbortController();
    customerApi.tierHistory(customerId, { signal: controller.signal })
      .then(page => { setHistory(page.content || []); setError(""); })
      .catch(requestError => { if (!controller.signal.aborted) setError(requestError.message || "Không thể tải lịch sử hạng."); });
    return () => controller.abort();
  }, [customerId, revision]);

  const change = async () => {
    if (!tierId) return;
    setBusy(true);
    try {
      await customerApi.changeTier(customerId, { tierId, note: note.trim() || null });
      notification.success("Đã đổi hạng khách hàng.");
      setTierId(""); setNote(""); setRevision(value => value + 1);
      onChanged?.();
    } catch (requestError) { notification.error(requestError); }
    finally { setBusy(false); }
  };

  return <section>
    <h3 className="mb-3 font-semibold">Hạng thành viên</h3>
    {error ? <p className="text-rose-300">{error}</p> : <div className="space-y-3">
      <p className="text-sm">Hạng hiện tại: <strong className="text-amber-300">{current ? names[current.tierId] || "—" : "Chưa có hạng"}</strong>
        {current && <span className="text-zinc-500"> · từ {dateTime(current.assignedAt)}</span>}</p>
      {canManage && <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-zinc-800 p-3">
        <select aria-label="Hạng mới" className={input} value={tierId} onChange={event => setTierId(event.target.value)}>
          <option value="">Chọn hạng mới</option>
          {tiers.filter(tier => tier.id !== current?.tierId).map(tier => <option key={tier.id} value={tier.id}>{tier.name} ({tier.code})</option>)}
        </select>
        <input aria-label="Lý do đổi hạng" className={`${input} min-w-[200px] flex-1`} maxLength="500" placeholder="Lý do (không bắt buộc)" value={note} onChange={event => setNote(event.target.value)}/>
        <Button type="button" disabled={!tierId || busy} onClick={change} className="rounded-xl bg-amber-500 px-4 py-2 font-semibold text-zinc-950 disabled:opacity-50">{busy ? "Đang đổi…" : "Đổi hạng"}</Button>
      </div>}
      {history.length > 0 && <ol className="space-y-2 text-sm">
        {history.map(item => <li key={item.id} className="rounded-xl border border-zinc-800 p-3">
          <p><strong>{names[item.tierId] || "Hạng đã xóa"}</strong>{!item.expiresAt && <span className="ml-2 text-xs text-emerald-400">Hiện tại</span>}</p>
          <p className="text-xs text-zinc-500">{dateTime(item.assignedAt)}{item.expiresAt ? ` → ${dateTime(item.expiresAt)}` : ""}{item.note ? ` · ${item.note}` : ""}</p>
        </li>)}
      </ol>}
    </div>}
  </section>;
}
