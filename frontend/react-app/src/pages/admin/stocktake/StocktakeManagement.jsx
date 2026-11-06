import { useState } from "react";
import PageGuide from "../../../components/admin/common/PageGuide";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ChartColumn, ClipboardCheck, Plus } from "lucide-react";
import Pagination from "../../../components/common/Pagination";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import {
  Badge, Empty, ErrorText, Field, Loading, Section, btnGhost, btnPrimary, control, textarea, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { stocktakeApi } from "../../../api/admin/stocktakeApi";
import { STOCKTAKE_STATUS, formatDateTime, money, qty } from "./stocktakeLogic";
import StocktakeReport from "./StocktakeReport";

/** Kiểm kê kho: danh sách phiếu theo cửa hàng và báo cáo hao hụt. */
export default function StocktakeManagement() {
  const [params, setParams] = useSearchParams();
  const tab = params.get("tab") === "report" ? "report" : "list";
  const lookups = useLoader((signal) => stocktakeApi.lookups({ signal }), []);
  const data = lookups.data;

  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
      <AdminCatalogPageHeader icon={ClipboardCheck} eyebrow="Kho vận" title="Kiểm kê kho"
        description="Đếm hàng thực tế bằng mã vạch, so với tồn hệ thống; quản lý duyệt để sinh phiếu điều chỉnh tồn. Báo cáo hao hụt theo giá vốn."
        status={data ? (data.global ? "Toàn hệ thống" : data.stores[0]?.name || "") : ""} />
      <PageGuide id="stocktakes" />
      <div className="flex gap-2" role="tablist">
        {[["list", "Phiếu kiểm kê", ClipboardCheck], ["report", "Báo cáo hao hụt", ChartColumn]].map(([key, label, Icon]) => (
          <button key={key} type="button" role="tab" aria-selected={tab === key}
            onClick={() => setParams(key === "report" ? { tab: "report" } : {}, { replace: true })}
            className={`inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-sm ${tab === key ? "border-amber-400 bg-amber-500/10 text-amber-200" : "border-zinc-700 text-zinc-400 hover:text-zinc-200"}`}>
            <Icon size={16} />{label}
          </button>
        ))}
      </div>
      <ErrorText>{lookups.error}</ErrorText>
      {!data ? (lookups.loading && <Loading />) : tab === "list" ? <StocktakeList lookups={data} /> : <StocktakeReport lookups={data} />}
    </div>
  );
}

function StocktakeList({ lookups }) {
  const navigate = useNavigate();
  const [storeId, setStoreId] = useState(lookups.global ? "" : lookups.scopeStoreId || "");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(1);
  const [creating, setCreating] = useState(false);
  const { data, error, loading } = useLoader(
    (signal) => stocktakeApi.list({ storeId, status, page: page - 1, size: 20 }, { signal }), [storeId, status, page]);
  const rows = data?.content || [];

  return (
    <div className="space-y-4">
      <Section>
        <div className="flex flex-wrap items-end gap-3">
          {lookups.global && (
            <Field label="Cửa hàng" className="min-w-[14rem]">
              <select className={control} value={storeId} onChange={(e) => { setStoreId(e.target.value); setPage(1); }}>
                <option value="">Tất cả cửa hàng</option>
                {lookups.stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </Field>
          )}
          <Field label="Trạng thái" className="min-w-[10rem]">
            <select className={control} value={status} onChange={(e) => { setStatus(e.target.value); setPage(1); }}>
              <option value="">Tất cả</option>
              {Object.entries(STOCKTAKE_STATUS).map(([value, s]) => <option key={value} value={value}>{s.label}</option>)}
            </select>
          </Field>
          {lookups.canCount && (
            <button type="button" className={`${btnPrimary} ml-auto`} onClick={() => setCreating(true)}><Plus size={16} />Tạo phiếu kiểm kê</button>
          )}
        </div>
      </Section>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : !rows.length ? <Empty>Chưa có phiếu kiểm kê.</Empty> : (
        <Section className="overflow-x-auto p-0">
          <table className="w-full min-w-[860px] text-sm">
            <thead className="text-left text-xs text-zinc-400">
              <tr className="border-b border-zinc-800">
                <th className="px-4 py-3 font-medium">Phiếu</th>
                <th className="px-4 py-3 font-medium">Phạm vi</th>
                <th className="px-4 py-3 font-medium">Tiến độ</th>
                <th className="px-4 py-3 text-right font-medium">Thiếu / thừa</th>
                <th className="px-4 py-3 font-medium">Trạng thái</th>
                <th className="px-4 py-3 font-medium">Tạo lúc</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((s) => {
                const pct = s.summary.itemCount ? Math.round((s.summary.countedCount / s.summary.itemCount) * 100) : 0;
                return (
                  <tr key={s.id} className="cursor-pointer border-b border-zinc-800/70 hover:bg-zinc-800/40" onClick={() => navigate(`/admin/stocktakes/${s.id}`)}>
                    <td className="px-4 py-3">
                      <Link to={`/admin/stocktakes/${s.id}`} className="font-semibold text-zinc-100 hover:text-amber-300" onClick={(e) => e.stopPropagation()}>{s.code}</Link>
                      <p className="text-xs text-zinc-500">{s.storeName}</p>
                    </td>
                    <td className="px-4 py-3 text-zinc-300">{s.categoryName || "Toàn cửa hàng"}{s.blindCount && <span className="ml-2 text-xs text-zinc-500">· đếm mù</span>}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="h-1.5 w-24 overflow-hidden rounded-full bg-zinc-800"><div className="h-full rounded-full bg-amber-400" style={{ width: `${pct}%` }} /></div>
                        <span className="text-xs tabular-nums text-zinc-400">{qty(s.summary.countedCount)}/{qty(s.summary.itemCount)}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      {s.systemVisible && s.summary.shortageQuantity != null ? (
                        <><span className="text-rose-300">−{qty(s.summary.shortageQuantity)}</span> / <span className="text-emerald-300">+{qty(s.summary.surplusQuantity)}</span>
                          <p className="text-xs text-zinc-500">{money(s.summary.shortageValue)}</p></>
                      ) : <span className="text-zinc-500">Ẩn (đếm mù)</span>}
                    </td>
                    <td className="px-4 py-3"><Badge map={STOCKTAKE_STATUS} value={s.status} /></td>
                    <td className="px-4 py-3 text-xs text-zinc-400">{formatDateTime(s.createdAt)}<p>{s.createdByName}</p></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </Section>
      )}
      <Pagination currentPage={page} totalPages={data?.page?.totalPages || data?.totalPages || 1} onPageChange={setPage} />
      {creating && <CreateDialog lookups={lookups} defaultStore={storeId} onClose={() => setCreating(false)} onCreated={(s) => navigate(`/admin/stocktakes/${s.id}`)} />}
    </div>
  );
}

function CreateDialog({ lookups, defaultStore, onClose, onCreated }) {
  const [form, setForm] = useState({ storeId: defaultStore || lookups.scopeStoreId || lookups.stores[0]?.id || "", scope: "STORE", categoryId: "", blindCount: true, note: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      onCreated(await stocktakeApi.create({
        storeId: form.storeId, categoryId: form.scope === "CATEGORY" ? form.categoryId : null, blindCount: form.blindCount, note: form.note.trim() || null,
      }));
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title="Tạo phiếu kiểm kê" description="Tồn hệ thống được chụp lại ngay khi tạo phiếu. Bán hàng trong lúc đếm vẫn được tính đúng khi duyệt." onClose={onClose} />
        <AdminDialogBody className="space-y-4">
          <Field label="Cửa hàng" required>
            <select className={control} value={form.storeId} onChange={(e) => setForm({ ...form, storeId: e.target.value })} disabled={!lookups.global} required>
              {lookups.stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <div className="flex gap-2">
            {[["STORE", "Toàn cửa hàng"], ["CATEGORY", "Theo danh mục"]].map(([value, label]) => (
              <button key={value} type="button" onClick={() => setForm({ ...form, scope: value })}
                className={`rounded-xl border px-3 py-1.5 text-sm ${form.scope === value ? "border-amber-400 bg-amber-400/10 text-amber-300" : "border-zinc-700 text-zinc-300"}`}>{label}</button>
            ))}
          </div>
          {form.scope === "CATEGORY" && (
            <Field label="Danh mục" hint="Gồm cả các danh mục con" required>
              <select className={control} value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} required>
                <option value="">Chọn danh mục…</option>
                {lookups.categories.map((c) => <option key={c.id} value={c.id}>{"  ".repeat(c.depth)}{c.depth ? "└ " : ""}{c.name}</option>)}
              </select>
            </Field>
          )}
          <label className="flex items-start gap-2 text-sm text-zinc-300">
            <input type="checkbox" className="mt-0.5" checked={form.blindCount} onChange={(e) => setForm({ ...form, blindCount: e.target.checked })} />
            <span>Đếm mù<span className="block text-xs text-zinc-500">Nhân viên đếm không thấy số tồn hệ thống, tránh đếm “cho khớp”. Người duyệt vẫn thấy.</span></span>
          </label>
          <Field label="Ghi chú"><textarea className={textarea} rows={2} maxLength={1000} value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} placeholder="Ví dụ: kiểm kê cuối tháng 11" /></Field>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving || !form.storeId}>{saving ? "Đang tạo…" : "Tạo phiếu và bắt đầu đếm"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
