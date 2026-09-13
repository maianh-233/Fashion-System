import { useCallback, useEffect, useState } from "react";
import { Eye, Link, Pencil, Plus, RefreshCw, Trash2 } from "lucide-react";
import Button from "../../common/Button";
import Pagination from "../../common/Pagination";
import AdminCatalogPageHeader from "../common/AdminCatalogPageHeader";

const PAGE_SIZE = 20;

export default function ApiCatalogPage({
  title, description, icon, api, permissions, columns, fields,
  initialValues = {}, extraFilters = null, normalize = (value) => value,
  onOpenRelated, relatedLabel = "Dữ liệu liên quan", relatedPermission, renderDetails,
}) {
  const [rows, setRows] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [dialog, setDialog] = useState(null);
  const [form, setForm] = useState(initialValues);

  const load = useCallback(async (signal) => {
    setLoading(true);
    setError("");
    try {
      const result = await api.list({ keyword: keyword.trim(), page: page - 1, size: PAGE_SIZE, sort: "createdAt,desc", ...extraFilters }, { signal });
      setRows(result?.content || []);
      setTotalPages(Math.max(1, result?.totalPages || 1));
      setTotalElements(result?.totalElements || 0);
    } catch (requestError) {
      if (requestError.name !== "AbortError") {
        setRows([]);
        setError(requestError.message || "Không thể tải dữ liệu.");
      }
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [api, extraFilters, keyword, page]);

  useEffect(() => {
    const controller = new AbortController();
    Promise.resolve().then(() => {
      if (!controller.signal.aborted) load(controller.signal);
    });
    return () => controller.abort();
  }, [load]);

  const openCreate = () => { setForm({ ...initialValues }); setDialog("create"); };
  const openView = (row) => { setForm({ ...row }); setDialog("view"); };
  const openEdit = (row) => { setForm({ ...row }); setDialog("edit"); };

  const save = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const payload = normalize(form);
      if (dialog === "create") await api.create(payload);
      else await api.update(form.id, payload);
      setDialog(null);
      await load();
    } catch (requestError) {
      setError(requestError.message || "Không thể lưu dữ liệu.");
    }
  };

  const remove = async (row) => {
    if (!window.confirm(`Xóa “${row.name || row.code || row.id}”?`)) return;
    try { await api.remove(row.id); await load(); }
    catch (requestError) { setError(requestError.message || "Không thể xóa dữ liệu."); }
  };

  return <div className="admin-catalog-page text-zinc-100 space-y-6">
    <AdminCatalogPageHeader icon={icon} eyebrow="Product master data" title={title} description={description} status={`${totalElements.toLocaleString("vi-VN")} bản ghi`} />
    <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-4 flex flex-wrap gap-3">
      <input value={keyword} onChange={(event) => { setKeyword(event.target.value); setPage(1); }} placeholder="Tìm kiếm…" className="h-11 min-w-72 flex-1 rounded-2xl border border-zinc-700 bg-zinc-800 px-4" />
      <Button type="button" onClick={() => load()} className="inline-flex items-center gap-2 rounded-2xl bg-zinc-800 px-4"><RefreshCw size={17} /> Làm mới</Button>
      <Button permission={permissions.create} type="button" onClick={openCreate} className="inline-flex items-center gap-2 rounded-2xl bg-amber-500 px-4 text-black"><Plus size={17} /> Thêm mới</Button>
    </section>
    {error && <p className="rounded-2xl border border-red-500/30 bg-red-500/10 p-3 text-red-200">{error}</p>}
    <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900"><div className="overflow-x-auto"><table className="w-full min-w-[760px] text-sm">
      <thead className="bg-zinc-800/70"><tr>{columns.map((column) => <th key={column.key} className="px-4 py-3 text-left">{column.label}</th>)}<th className="px-4 py-3 text-left">Thao tác</th></tr></thead>
      <tbody>{loading ? <tr><td colSpan={columns.length + 1} className="py-14 text-center text-zinc-400">Đang tải…</td></tr> : rows.length === 0 ? <tr><td colSpan={columns.length + 1} className="py-14 text-center text-zinc-400">Không có dữ liệu.</td></tr> : rows.map((row) => <tr key={row.id} className="border-t border-zinc-800">
        {columns.map((column) => <td key={column.key} className="px-4 py-3">{column.render ? column.render(row) : row[column.key] ?? "—"}</td>)}
        <td className="px-4 py-3"><div className="flex gap-3"><Button type="button" onClick={() => openView(row)} title="Xem"><Eye size={17} /></Button>{onOpenRelated && <Button permission={relatedPermission} type="button" onClick={() => onOpenRelated(row)} title={relatedLabel}><Link size={17} /></Button>}<Button permission={permissions.update} type="button" onClick={() => openEdit(row)} title="Sửa"><Pencil size={17} /></Button><Button permission={permissions.delete} type="button" onClick={() => remove(row)} title="Xóa"><Trash2 size={17} /></Button></div></td>
      </tr>)}</tbody>
    </table></div><Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} /></section>

    {dialog && <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/70 p-4" role="dialog" aria-modal="true"><form onSubmit={save} className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-3xl border border-zinc-700 bg-zinc-900 p-6">
      <div className="mb-5 flex items-center justify-between"><h2 className="text-xl font-semibold">{dialog === "create" ? "Thêm mới" : dialog === "edit" ? "Chỉnh sửa" : "Chi tiết"} {title}</h2><Button type="button" onClick={() => setDialog(null)}>Đóng</Button></div>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">{fields.map((field) => <label key={field.key} className="space-y-1 text-sm"><span className="text-zinc-300">{field.label}</span>{field.type === "select" ? <select disabled={dialog === "view"} required={field.required} value={form[field.key] ?? ""} onChange={(event) => setForm((value) => ({ ...value, [field.key]: event.target.value || null }))} className="h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3"><option value="">-- Chọn --</option>{field.options?.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select> : field.type === "textarea" ? <textarea disabled={dialog === "view"} required={field.required} value={form[field.key] ?? ""} onChange={(event) => setForm((value) => ({ ...value, [field.key]: event.target.value }))} className="min-h-24 w-full rounded-xl border border-zinc-700 bg-zinc-800 p-3" /> : <input disabled={dialog === "view"} required={field.required} type={field.type || "text"} value={form[field.key] ?? ""} onChange={(event) => setForm((value) => ({ ...value, [field.key]: field.type === "number" ? (event.target.value === "" ? "" : Number(event.target.value)) : event.target.value }))} className="h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3" />}</label>)}</div>
      {form.id && renderDetails?.(form, dialog)}
      {dialog !== "view" && <div className="mt-6 flex justify-end"><Button permission={dialog === "create" ? permissions.create : permissions.update} type="submit" className="rounded-xl bg-amber-500 px-5 py-2.5 text-black">Lưu dữ liệu</Button></div>}
    </form></div>}
  </div>;
}
