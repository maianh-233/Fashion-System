import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Pencil, Plus, Search, Trash2, Zap } from "lucide-react";
import Button from "../../components/common/Button";
import Pagination from "../../components/common/Pagination";
import { useSystemNotification } from "../../components/common/SystemNotification";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../components/admin/common/AdminDialog";
import { flashSaleApi } from "../../api/admin/flashSaleApi";
import { variantApi } from "../../api/admin/catalogApi";
import { FLASH_STATUS, emptyForm, formFromSale, salePayload, validateSaleForm } from "./flashSaleManagementLogic";

const control = "h-10 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-amber-400";
const money = (value) => (value == null ? "" : `${Number(value).toLocaleString("vi-VN")} ₫`);
const formatTime = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "");

/** Flash sale (đơn online): khung giờ, giá flash và số suất từng biến thể; tối đa 2 món / khách / biến thể. */
export default function FlashSaleManagement() {
  const notification = useSystemNotification();
  const [page, setPage] = useState(1);
  const [version, setVersion] = useState(0);
  const [state, setState] = useState({ key: null, rows: [], totalPages: 1, error: "" });
  const [editing, setEditing] = useState(null);
  const key = `${page}|${version}`;

  useEffect(() => {
    const controller = new AbortController();
    flashSaleApi.list({ page: page - 1 }, { signal: controller.signal })
      .then((res) => setState({ key, rows: res.content || [], totalPages: Math.max(1, res.page?.totalPages || 1), error: "" }))
      .catch((failure) => { if (!controller.signal.aborted) setState({ key, rows: [], totalPages: 1, error: failure.message }); });
    return () => controller.abort();
  }, [key, page]);

  const toggle = async (sale) => {
    try {
      await flashSaleApi.setActive(sale.id, !sale.active);
      notification.success(sale.active ? "Đã dừng chương trình." : "Đã bật lại chương trình.");
      setVersion((v) => v + 1);
    } catch (failure) {
      notification.error(failure.message);
    }
  };

  const loading = state.key !== key;
  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100">
      <AdminCatalogPageHeader icon={Zap} eyebrow="Chiến dịch bán hàng" title="Flash sale"
        description="Giảm giá theo khung giờ cho từng biến thể, giới hạn số suất; mỗi khách tối đa 2 món giá flash cho mỗi phân loại. Chỉ áp dụng cho đơn online."
        action={<div className="flex gap-2">
          <Link to="/admin/promotions" className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-sm text-zinc-300 hover:text-white"><ArrowLeft size={15} />Khuyến mãi</Link>
          <Button type="button" variant="primary" permission="PROMOTION_CREATE" className="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold"
            onClick={() => setEditing({ form: emptyForm() })}><Plus size={15} />Tạo flash sale</Button>
        </div>} />

      <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900">
        {state.error && <p role="alert" className="m-4 text-sm text-rose-300">{state.error}</p>}
        <div className="overflow-x-auto">
          <table className="w-full min-w-[900px] text-sm">
            <thead className="bg-zinc-800/70 text-zinc-300">
              <tr>{["Chương trình", "Khung giờ", "Sản phẩm · đã bán / suất", "Trạng thái", ""].map((h) => <th key={h} className="px-4 py-3 text-left font-semibold">{h}</th>)}</tr>
            </thead>
            <tbody>
              {loading && <tr><td colSpan="5" className="px-4 py-12 text-center text-zinc-400">Đang tải…</td></tr>}
              {!loading && state.rows.length === 0 && <tr><td colSpan="5" className="px-4 py-12 text-center text-zinc-400">Chưa có flash sale.</td></tr>}
              {!loading && state.rows.map((sale) => (
                <tr key={sale.id} className="border-t border-zinc-800 align-top hover:bg-zinc-800/30">
                  <td className="px-4 py-3 font-semibold">{sale.name}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-zinc-300">{formatTime(sale.startsAt)}<br /><span className="text-zinc-500">→ {formatTime(sale.endsAt)}</span></td>
                  <td className="px-4 py-3 text-xs text-zinc-300">
                    {sale.items.slice(0, 3).map((i) => <p key={i.variantId}>{i.productName} ({[i.color, i.size].filter(Boolean).join(" / ")}) · {money(i.salePrice)} · {i.sold}/{i.quota}</p>)}
                    {sale.items.length > 3 && <p className="text-zinc-500">+{sale.items.length - 3} biến thể khác</p>}
                  </td>
                  <td className="px-4 py-3"><span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${FLASH_STATUS[sale.status]?.style || ""}`}>{FLASH_STATUS[sale.status]?.label || sale.status}</span></td>
                  <td className="px-4 py-3">
                    <div className="flex flex-col items-start gap-2">
                      {sale.status !== "ENDED" && (
                        <button type="button" className="inline-flex items-center gap-1 text-amber-300" onClick={() => setEditing({ id: sale.id, form: formFromSale(sale) })}>
                          <Pencil size={14} />Sửa
                        </button>
                      )}
                      {sale.status !== "ENDED" && (
                        <Button type="button" variant="ghost" permission="PROMOTION_UPDATE" className="text-sm" onClick={() => toggle(sale)}>
                          {sale.active ? "Dừng" : "Bật lại"}
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pagination currentPage={page} totalPages={state.totalPages} onPageChange={setPage} />
      </section>

      {editing && <SaleDialog editing={editing} onClose={() => setEditing(null)}
        onSaved={() => { setEditing(null); notification.success("Đã lưu flash sale."); setVersion((v) => v + 1); }} />}
    </div>
  );
}

function SaleDialog({ editing, onClose, onSaved }) {
  const [form, setForm] = useState(editing.form);
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const search = async (event) => {
    event.preventDefault();
    if (!keyword.trim()) return;
    try {
      const page = await variantApi.listAll({ keyword: keyword.trim(), active: true, page: 0, size: 10 });
      setResults(page.content || []);
    } catch (failure) {
      setError(failure.message);
    }
  };
  const addVariant = (variant) => {
    if (form.items.some((item) => item.variantId === variant.id)) return;
    const regular = variant.salePrice ?? variant.price;
    setForm((f) => ({ ...f, items: [...f.items, {
      variantId: variant.id, label: `${variant.productName} · ${variant.sku} (${[variant.color, variant.size].filter(Boolean).join(" / ")})`,
      regularPrice: regular, salePrice: String(Math.round(Number(regular) * 0.7 / 1000) * 1000), quota: "20", sold: 0,
    }] }));
  };
  const updateItem = (variantId, patch) => setForm((f) => ({ ...f, items: f.items.map((i) => (i.variantId === variantId ? { ...i, ...patch } : i)) }));

  const save = async () => {
    const problem = validateSaleForm(form);
    setError(problem);
    if (problem) return;
    setSaving(true);
    try {
      if (editing.id) await flashSaleApi.update(editing.id, salePayload(form));
      else await flashSaleApi.create(salePayload(form));
      onSaved();
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <AdminDialog open onClose={onClose} size="lg">
      <AdminDialogHeader title={editing.id ? "Sửa flash sale" : "Tạo flash sale"} description="Giá flash phải thấp hơn giá bán hiện tại; một biến thể không nằm trong hai chương trình trùng giờ." onClose={onClose} />
      <AdminDialogBody className="space-y-4 text-sm">
        <div className="grid gap-3 sm:grid-cols-3">
          <label className="space-y-1 text-zinc-400 sm:col-span-3"><span>Tên chương trình</span>
            <input className={control} value={form.name} maxLength={150} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} placeholder="Giờ vàng cuối tuần" /></label>
          <label className="space-y-1 text-zinc-400"><span>Bắt đầu</span>
            <input type="datetime-local" className={control} value={form.startsAt} onChange={(e) => setForm((f) => ({ ...f, startsAt: e.target.value }))} /></label>
          <label className="space-y-1 text-zinc-400"><span>Kết thúc</span>
            <input type="datetime-local" className={control} value={form.endsAt} onChange={(e) => setForm((f) => ({ ...f, endsAt: e.target.value }))} /></label>
        </div>

        <form className="flex gap-2" onSubmit={search}>
          <input className={control} value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Tìm biến thể theo tên sản phẩm / SKU" />
          <Button type="submit" variant="secondary" className="inline-flex items-center gap-1 rounded-xl px-3 text-sm"><Search size={15} />Tìm</Button>
        </form>
        {results.length > 0 && (
          <ul className="max-h-40 overflow-y-auto rounded-xl border border-zinc-800">
            {results.map((v) => (
              <li key={v.id} className="flex items-center justify-between gap-2 border-b border-zinc-800 px-3 py-2 last:border-0">
                <span>{v.productName} <span className="text-zinc-500">· {v.sku} · {[v.color, v.size].filter(Boolean).join(" / ")} · {money(v.salePrice ?? v.price)}</span></span>
                <button type="button" className="text-amber-300" onClick={() => addVariant(v)}><Plus size={15} /></button>
              </li>
            ))}
          </ul>
        )}

        <table className="w-full text-sm">
          <thead className="text-zinc-400"><tr><th className="py-1 text-left">Biến thể</th><th className="py-1 text-left">Giá thường</th><th className="py-1 text-left">Giá flash</th><th className="py-1 text-left">Suất</th><th /></tr></thead>
          <tbody>
            {form.items.length === 0 && <tr><td colSpan="5" className="py-4 text-center text-zinc-500">Chưa có biến thể nào.</td></tr>}
            {form.items.map((item) => (
              <tr key={item.variantId} className="border-t border-zinc-800">
                <td className="py-2 pr-2">{item.label}{item.sold > 0 && <span className="block text-xs text-zinc-500">Đã bán {item.sold}</span>}</td>
                <td className="py-2 pr-2 text-zinc-400">{money(item.regularPrice)}</td>
                <td className="py-2 pr-2"><input type="number" min="1" className={`${control} w-32`} value={item.salePrice} onChange={(e) => updateItem(item.variantId, { salePrice: e.target.value })} /></td>
                <td className="py-2 pr-2"><input type="number" min="1" className={`${control} w-24`} value={item.quota} onChange={(e) => updateItem(item.variantId, { quota: e.target.value })} /></td>
                <td className="py-2">{item.sold === 0 && (
                  <button type="button" aria-label="Bỏ biến thể" className="text-zinc-500 hover:text-rose-300"
                    onClick={() => setForm((f) => ({ ...f, items: f.items.filter((i) => i.variantId !== item.variantId) }))}><Trash2 size={15} /></button>
                )}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {error && <p role="alert" className="text-rose-300">{error}</p>}
      </AdminDialogBody>
      <AdminDialogFooter>
        <Button type="button" variant="secondary" className="rounded-xl px-4 py-2.5 text-sm" onClick={onClose} disabled={saving}>Hủy</Button>
        <Button type="button" variant="primary" permission={editing.id ? "PROMOTION_UPDATE" : "PROMOTION_CREATE"}
          className="rounded-xl px-4 py-2.5 text-sm font-semibold" loading={saving} onClick={save}>Lưu</Button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}
