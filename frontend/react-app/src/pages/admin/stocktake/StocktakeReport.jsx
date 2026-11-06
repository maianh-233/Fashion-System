import { useState } from "react";
import { Empty, ErrorText, Field, Loading, Section, Stat, control, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { stocktakeApi } from "../../../api/admin/stocktakeApi";
import { money, percent, qty } from "./stocktakeLogic";

const pad = (n) => String(n).padStart(2, "0");
const iso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

/** Báo cáo hao hụt từ các phiếu kiểm kê đã duyệt (giá trị theo giá vốn chụp lúc tạo phiếu). */
export default function StocktakeReport({ lookups }) {
  const today = new Date();
  const [range, setRange] = useState({ from: iso(new Date(today.getFullYear(), today.getMonth() - 5, 1)), to: iso(today) });
  const [storeId, setStoreId] = useState(lookups.global ? "" : lookups.scopeStoreId || "");
  const { data, error, loading } = useLoader((signal) => stocktakeApi.report({ ...range, storeId }, { signal }), [range, storeId]);
  const t = data?.totals;
  const maxStore = Math.max(1, ...(data?.stores || []).map((s) => Number(s.shortageValue)));
  const maxCat = Math.max(1, ...(data?.categories || []).map((c) => Number(c.shortageValue)));

  return (
    <div className="space-y-5">
      <Section>
        <div className="grid gap-3 sm:grid-cols-3">
          <Field label="Từ ngày (duyệt)"><input type="date" className={control} value={range.from} max={range.to} onChange={(e) => e.target.value && setRange({ ...range, from: e.target.value })} /></Field>
          <Field label="Đến ngày"><input type="date" className={control} value={range.to} min={range.from} onChange={(e) => e.target.value && setRange({ ...range, to: e.target.value })} /></Field>
          {lookups.global && (
            <Field label="Cửa hàng">
              <select className={control} value={storeId} onChange={(e) => setStoreId(e.target.value)}>
                <option value="">Toàn chuỗi</option>
                {lookups.stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </Field>
          )}
        </div>
      </Section>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : t && (!t.stocktakes ? <Empty>Chưa có phiếu kiểm kê nào được duyệt trong khoảng này.</Empty> : (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
            <Stat label="Phiếu đã duyệt" value={qty(t.stocktakes)} hint={`${qty(t.itemsCounted)} dòng hàng`} />
            <Stat label="Giá trị tồn đã kiểm" value={money(t.systemValue)} hint={`${qty(t.systemQuantity)} sản phẩm`} tone="sky" />
            <Stat label="Hao hụt (thiếu)" value={money(t.shortageValue)} hint={`${qty(t.shortageQuantity)} sản phẩm`} tone="rose" />
            <Stat label="Thừa" value={money(t.surplusValue)} hint={`${qty(t.surplusQuantity)} sản phẩm`} tone="emerald" />
            <Stat label="Chênh lệch ròng" value={money(t.netValue)} tone={Number(t.netValue) < 0 ? "rose" : "emerald"} />
            <Stat label="Tỷ lệ hao hụt" value={percent(t.shrinkageRate)} hint="Thiếu / giá trị tồn đã kiểm" tone={Number(t.shrinkageRate) > 2 ? "rose" : "amber"} />
          </div>

          <div className="grid gap-5 xl:grid-cols-2 [&>*]:min-w-0">
            {lookups.global && !storeId && (
              <Section title="Theo cửa hàng">
                <BarTable rows={data.stores.map((s) => ({ key: s.storeId, label: s.storeName, sub: `${s.stocktakes} phiếu · tỷ lệ ${percent(s.shrinkageRate)}`, value: s.shortageValue, qty: s.shortageQuantity, net: s.netValue }))} max={maxStore} />
              </Section>
            )}
            <Section title="Theo danh mục">
              <BarTable rows={data.categories.map((c) => ({ key: c.categoryId || c.categoryName, label: c.categoryName, sub: `thừa ${qty(c.surplusQuantity)}`, value: c.shortageValue, qty: c.shortageQuantity, net: c.netValue }))} max={maxCat} />
            </Section>
            <Section title="Hao hụt nhiều nhất (top 10)">
              {!data.topShortages.length ? <Empty>Không có mã hàng nào bị thiếu.</Empty> : (
                <ol className="space-y-2 text-sm">
                  {data.topShortages.map((v, i) => (
                    <li key={v.variantId} className="flex items-start justify-between gap-3">
                      <span className="min-w-0"><span className="mr-2 text-zinc-500">{i + 1}.</span><span className="text-zinc-100">{v.productName}</span>
                        <span className="text-zinc-400"> {[v.color, v.size].filter(Boolean).join(" / ")}</span>
                        <span className="block pl-5 text-xs text-zinc-500">{v.sku} · thiếu {v.occurrences} lần kiểm kê</span></span>
                      <span className="shrink-0 text-right tabular-nums"><span className="text-rose-300">−{qty(v.shortageQuantity)}</span><span className="block text-xs text-zinc-400">{money(v.shortageValue)}</span></span>
                    </li>
                  ))}
                </ol>
              )}
            </Section>
            <Section title="Theo tháng">
              <table className="w-full text-sm">
                <thead className="text-left text-xs text-zinc-400"><tr className="border-b border-zinc-800"><th className="py-2 font-medium">Tháng</th><th className="py-2 text-right font-medium">Thiếu</th><th className="py-2 text-right font-medium">Thừa</th><th className="py-2 text-right font-medium">Ròng</th></tr></thead>
                <tbody>
                  {data.months.map((m) => (
                    <tr key={m.month} className="border-b border-zinc-800/70 tabular-nums">
                      <td className="py-2 text-zinc-200">{m.month.slice(5)}/{m.month.slice(0, 4)}</td>
                      <td className="py-2 text-right text-rose-300">{money(m.shortageValue)}</td>
                      <td className="py-2 text-right text-emerald-300">{money(m.surplusValue)}</td>
                      <td className={`py-2 text-right ${Number(m.netValue) < 0 ? "text-rose-300" : "text-emerald-300"}`}>{money(m.netValue)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Section>
          </div>
          <p className="text-xs text-zinc-500">Giá trị tính theo giá nhập gần nhất lúc tạo phiếu (mã chưa có phiếu nhập dùng giá bán). Hao hụt là chênh lệch theo số đếm; số thực điều chỉnh xem trong từng phiếu.</p>
        </>
      ))}
    </div>
  );
}

function BarTable({ rows, max }) {
  if (!rows.length) return <Empty>Không có dữ liệu.</Empty>;
  return (
    <ul className="space-y-3 text-sm">
      {rows.map((r) => (
        <li key={r.key}>
          <div className="flex items-baseline justify-between gap-3">
            <span className="min-w-0 truncate text-zinc-100">{r.label} <span className="text-xs text-zinc-500">{r.sub}</span></span>
            <span className="shrink-0 tabular-nums text-rose-300">{money(r.value)} <span className="text-xs text-zinc-500">(−{qty(r.qty)})</span></span>
          </div>
          <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-zinc-800">
            <div className="h-full rounded-full bg-rose-400" style={{ width: `${Math.max(2, (Number(r.value) / max) * 100)}%` }} />
          </div>
        </li>
      ))}
    </ul>
  );
}
