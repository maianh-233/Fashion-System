import { useState } from "react";
import { Info } from "lucide-react";
import { Empty, ErrorText, Loading, Section, btnPrimary, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { marketingApi } from "../../../api/admin/marketingApi";
import { CAMPAIGN_STATUS, formatMoney, formatRatio, formatWindow } from "./marketingLogic";

const MAX = 5;

/** So sánh 2–5 chiến dịch; dòng tổng tính mỗi đơn một lần nên nhỏ hơn tổng các cột khi có đơn dùng chung. */
export default function CampaignCompare() {
  const options = useLoader((signal) => marketingApi.list({ size: 100 }, { signal }), []);
  const [selected, setSelected] = useState([]);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const campaigns = options.data?.content || [];
  const toggle = (id) => setSelected((s) => (s.includes(id) ? s.filter((x) => x !== id) : s.length >= MAX ? s : [...s, id]));
  const names = Object.fromEntries((result?.rows || []).map((r) => [r.id, r.code]));

  const run = async () => {
    setBusy(true);
    setError("");
    try {
      setResult(await marketingApi.compare(selected));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-4">
      <Section title={`Chọn chiến dịch (${selected.length}/${MAX})`}
        action={<button type="button" className={btnPrimary} disabled={selected.length < 2 || busy} onClick={run}>{busy ? "Đang tính…" : "So sánh"}</button>}>
        <ErrorText>{options.error}</ErrorText>
        {options.loading && !options.data ? <Loading /> : campaigns.length === 0 ? <Empty>Chưa có chiến dịch nào.</Empty> : (
          <div className="flex flex-wrap gap-2">
            {campaigns.map((c) => (
              <label key={c.id} className={`flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-1.5 text-sm ${selected.includes(c.id) ? "border-amber-400 bg-amber-500/10 text-amber-200" : "border-zinc-700 text-zinc-300"}`}>
                <input type="checkbox" className="accent-amber-400" checked={selected.includes(c.id)} onChange={() => toggle(c.id)}
                  disabled={!selected.includes(c.id) && selected.length >= MAX} />
                {c.code} · {c.name}
              </label>
            ))}
          </div>
        )}
      </Section>
      <ErrorText>{error}</ErrorText>
      {result && (
        <Section className="overflow-x-auto p-0">
          <table className="w-full min-w-[760px] text-sm">
            <thead className="text-left text-xs text-zinc-400">
              <tr className="border-b border-zinc-800">
                <th className="px-4 py-3 font-medium">Chiến dịch</th>
                <th className="px-4 py-3 text-right font-medium">Số đơn</th>
                <th className="px-4 py-3 text-right font-medium">Doanh thu</th>
                <th className="px-4 py-3 text-right font-medium">Doanh thu thuần</th>
                <th className="px-4 py-3 text-right font-medium">Tổng chi phí</th>
                <th className="px-4 py-3 text-right font-medium">ROAS</th>
                <th className="px-4 py-3 text-right font-medium">Khách mua</th>
              </tr>
            </thead>
            <tbody>
              {result.rows.map((r) => (
                <tr key={r.id} className="border-b border-zinc-800/70">
                  <td className="px-4 py-3"><p className="font-semibold text-zinc-100">{r.code} · {r.name}</p><p className="text-xs text-zinc-500">{CAMPAIGN_STATUS[r.status]?.label} · {formatWindow(r.startsAt, r.endsAt)}</p></td>
                  <td className="px-4 py-3 text-right tabular-nums">{r.orders}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{formatMoney(r.revenue)}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{formatMoney(r.netRevenue)}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{formatMoney(r.totalCost)}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{formatRatio(r.roas)}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{r.buyers}</td>
                </tr>
              ))}
              <tr className="bg-zinc-950/60 font-semibold">
                <td className="px-4 py-3">Tổng (mỗi đơn tính một lần)</td>
                <td className="px-4 py-3 text-right tabular-nums">{result.distinctOrders}</td>
                <td className="px-4 py-3 text-right tabular-nums">{formatMoney(result.distinctRevenue)}</td>
                <td colSpan={4} />
              </tr>
            </tbody>
          </table>
          {result.overlaps.length > 0 && (
            <ul className="space-y-1 border-t border-zinc-800 px-4 py-3 text-xs text-amber-200">
              {result.overlaps.map((o) => <li key={`${o.first}-${o.second}`}>{names[o.first]} và {names[o.second]} có {o.sharedOrders} đơn dùng chung ưu đãi.</li>)}
            </ul>
          )}
          <ul className="space-y-1 border-t border-zinc-800 px-4 py-3 text-xs text-zinc-400">
            {result.dataNotes.map((n) => <li key={n} className="flex gap-2"><Info size={13} className="mt-0.5 shrink-0 text-sky-300" />{n}</li>)}
          </ul>
        </Section>
      )}
    </div>
  );
}
