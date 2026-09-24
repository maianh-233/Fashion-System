import { useCallback, useEffect, useState } from "react";
import { Eye, Link, Pencil, Plus, RotateCcw, Trash2 } from "lucide-react";
import Button from "../../common/Button";
import Pagination from "../../common/Pagination";
import AdminCatalogPageHeader from "../common/AdminCatalogPageHeader";
import { useSystemNotification } from "../../common/SystemNotification";
import { useAdminPermissions } from "../../../contexts/AdminPermissionsContext";
import CatalogImageField from "./CatalogImageField";
import CatalogNoteField from "./CatalogNoteField";
import {
  catalogPageMetadata,
  initialCatalogStatusFilter,
  isCatalogFieldVisible,
  isInactiveCatalogRow,
} from "./catalogPageLogic";

const PAGE_SIZE = 20;

function CatalogSelect({ field, value, onChange, disabled }) {
  const [keyword, setKeyword] = useState("");
  const [options, setOptions] = useState([]);
  useEffect(() => {
    if (disabled) return undefined;
    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      field.api.list({ keyword, status: "ACTIVE", active: true, size: 30, page: 0, sort: "name,asc" },
        { signal: controller.signal }).then((page) => setOptions(page?.content || []))
        .catch(() => setOptions([]));
    }, 250);
    return () => { window.clearTimeout(timer); controller.abort(); };
  }, [disabled, field.api, keyword]);
  useEffect(() => {
    if (!value || options.some((item) => item.id === value)) return undefined;
    const controller = new AbortController();
    field.api.detail(value, { signal: controller.signal }).then((item) =>
      setOptions((current) => current.some((option) => option.id === item.id) ? current : [item, ...current]))
      .catch(() => {});
    return () => controller.abort();
  }, [field.api, options, value]);
  return <div className="space-y-2"><input value={keyword} disabled={disabled} onChange={(event) => setKeyword(event.target.value)} placeholder={`Tìm ${field.label.toLowerCase()}…`} className="h-10 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3" /><select disabled={disabled} value={value || ""} onChange={(event) => onChange(event.target.value || null)} className="h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3"><option value="">-- Chọn --</option>{options.map((item) => <option key={item.id} value={item.id}>{item.code ? `${item.code} - ` : ""}{item.name}</option>)}</select></div>;
}

export default function ApiCatalogPage({
  title, description, icon, api, permissions, columns, fields,
  initialValues = {}, extraFilters = null, normalize = (value) => value,
  onOpenRelated, relatedLabel = "Dữ liệu liên quan", relatedPermission, relatedIconOnly = false, renderDetails, rowClassName,
  renderActions, canCreate = true, sort = "createdAt,desc",
  statusParam, statusOptions,
  filterFields = [],
  tableMinWidth,
  dialogMaxWidth = "max-w-2xl",
  continueEditingAfterCreate = false,
  detailsTabLabel,
  detailsPosition = "after",
  dialogComponent: DialogComponent,
  validate,
  isRestorable = isInactiveCatalogRow,
}) {
  const notification = useSystemNotification();
  const { isGlobal, hasPermission } = useAdminPermissions();
  const [rows, setRows] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState(null);
  const selectedStatus = statusFilter ?? initialCatalogStatusFilter(statusParam, isGlobal);
  const [filters, setFilters] = useState({});
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [dialog, setDialog] = useState(null);
  const [form, setForm] = useState(initialValues);
  const [uploading, setUploading] = useState(false);
  const [imageError, setImageError] = useState("");
  const [dialogTab, setDialogTab] = useState("info");
  const resetFilters = () => { setKeyword(""); setStatusFilter(null); setFilters({}); setPage(1); };

  const load = useCallback(async (signal) => {
    setLoading(true);
    setError("");
    try {
      const result = await api.list({ keyword: keyword.trim(), page: page - 1, size: PAGE_SIZE, sort,
        ...(statusParam && selectedStatus !== "" ? { [statusParam]: selectedStatus } : {}), ...filters, ...extraFilters }, { signal });
      const metadata = catalogPageMetadata(result);
      setRows(result?.content || []);
      setTotalPages(metadata.totalPages);
      setTotalElements(metadata.totalElements);
    } catch (requestError) {
      if (requestError.name !== "AbortError") {
        setRows([]);
        setError(requestError.message || "Không thể tải dữ liệu.");
      }
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [api, extraFilters, filters, keyword, page, selectedStatus, sort, statusParam]);

  useEffect(() => {
    const controller = new AbortController();
    Promise.resolve().then(() => {
      if (!controller.signal.aborted) load(controller.signal);
    });
    return () => controller.abort();
  }, [load]);

  const openCreate = () => { setForm({ ...initialValues }); setImageError(""); setDialogTab("info"); setDialog("create"); };
  const openView = (row) => { setForm({ ...row }); setImageError(""); setDialogTab("info"); setDialog("view"); };
  const openEdit = (row) => { setForm({ ...row }); setImageError(""); setDialogTab("info"); setDialog("edit"); };

  const save = async (event) => {
    event.preventDefault();
    setError("");
    if (imageError) return;
    setUploading(true);
    try {
      const { imageFile, ...values } = form;
      const payload = normalize(values);
      const validationMessage = validate?.(payload, rows, dialog);
      if (validationMessage) { notification.warning(validationMessage); return; }
      const saved = dialog === "create" ? await api.create(payload) : await api.update(form.id, payload, form);
      if (dialog === "create" && continueEditingAfterCreate) {
        setForm({ ...saved, imageFile });
        setDialog("edit");
      }
      const uploadedImage = imageFile && api.uploadImage ? await api.uploadImage(saved.id, imageFile) : null;
      notification.success(dialog === "create" ? `Thêm ${title.toLowerCase()} thành công` : `Cập nhật ${title.toLowerCase()} thành công`);
      if (dialog === "create" && continueEditingAfterCreate) {
        setForm(imageFile && api.detail ? await api.detail(saved.id) : { ...saved, imageUrl: uploadedImage?.imageUrl || saved.imageUrl });
        setDialog("edit");
      } else setDialog(null);
      await load();
    } catch (requestError) {
      setError(requestError.message || "Không thể lưu dữ liệu.");
      if (requestError.status === 409) notification.warning(requestError.message || "Dữ liệu đã tồn tại.");
      else notification.error(requestError.message || "Không thể lưu dữ liệu.");
    } finally {
      setUploading(false);
    }
  };

  const remove = async (row) => {
    try {
      const impact = api.impact ? await api.impact(row.id) : null;
      const affected = impact ? ` ${impact.products ?? 0} sản phẩm${impact.categories == null ? "" : `, ${impact.categories} danh mục con`}${impact.variants == null ? "" : ` và ${impact.variants} biến thể`} sẽ bị ảnh hưởng.` : "";
      const accepted = await notification.confirm({ title: `Vô hiệu hóa ${title.toLowerCase()}`,
        message: `Bạn có chắc muốn vô hiệu hóa “${row.name || row.code || row.sku || row.id}”?${affected}`,
        confirmText: "Vô hiệu hóa", destructive: true });
      if (!accepted) return;
      await api.remove(row.id, row);
      notification.success(`Vô hiệu hóa ${title.toLowerCase()} thành công`);
      await load();
    } catch (requestError) { setError(requestError.message || "Không thể vô hiệu hóa dữ liệu."); notification.error(requestError.message || "Không thể vô hiệu hóa dữ liệu."); }
  };

  const restore = async (row) => {
    const accepted = await notification.confirm({ title: `Khôi phục ${title.toLowerCase()}`,
      message: `Khôi phục “${row.name || row.code || row.sku || row.id}”?`, confirmText: "Khôi phục" });
    if (!accepted) return;
    try { await api.restore(row.id, row); notification.success(`Khôi phục ${title.toLowerCase()} thành công`); await load(); }
    catch (requestError) { setError(requestError.message || "Không thể khôi phục dữ liệu."); notification.error(requestError.message || "Không thể khôi phục dữ liệu."); }
  };

  return <div className="admin-catalog-page text-zinc-100 space-y-6">
    <AdminCatalogPageHeader icon={icon} eyebrow="Product master data" title={title} description={description} status={`${totalElements.toLocaleString("vi-VN")} bản ghi`} />
    <section className="admin-catalog-toolbar space-y-4 rounded-3xl border border-zinc-800 bg-zinc-900 p-6">
      <div className="flex flex-wrap items-center gap-4">
        <input value={keyword} onChange={(event) => { setKeyword(event.target.value); setPage(1); }} placeholder="Tìm kiếm…" className="min-w-0 rounded-2xl border border-zinc-700 bg-zinc-800 px-4 sm:w-80" />
        {statusParam && isGlobal && <select value={selectedStatus} onChange={(event) => { setStatusFilter(event.target.value); setPage(1); }} className="min-w-0 rounded-2xl border border-zinc-700 bg-zinc-800 px-5"><option value="">{statusParam === "active" ? "Tất cả trạng thái" : "Hoạt động"}</option>{statusParam === "active" && <option value="true">Hoạt động</option>}{statusOptions?.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>}
        <Button type="button" onClick={resetFilters} className="flex items-center gap-2 rounded-2xl bg-blue-500 px-6 py-3 font-medium"><RotateCcw size={18} /> Reset</Button>
        {canCreate && <Button permission={permissions.create} type="button" onClick={openCreate} className="flex items-center gap-2 rounded-2xl bg-amber-500 px-6 py-3 font-medium"><Plus size={18} /> Thêm {title.toLowerCase()}</Button>}
      </div>
      {filterFields.length > 0 && <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">{filterFields.map((field) => <div key={field.key} className="min-w-0"><label className="mb-1 block text-xs font-medium text-zinc-400">{field.label}</label><CatalogSelect field={field} value={filters[field.key]} onChange={(value) => { setFilters((current) => ({ ...current, [field.key]: value })); setPage(1); }} /></div>)}</div>}
    </section>
    {error && <p className="rounded-2xl border border-red-500/30 bg-red-500/10 p-3 text-red-200">{error}</p>}
    <section className="admin-catalog-table overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900"><div className="overflow-x-auto"><table className="w-full min-w-[760px] text-sm" style={tableMinWidth ? { minWidth: tableMinWidth } : undefined}>
      <thead className="bg-zinc-800/70"><tr>{columns.map((column) => <th key={column.key} className="px-4 py-3 text-left" style={column.minWidth ? { minWidth: column.minWidth } : undefined}>{column.label}</th>)}<th className="px-4 py-3 text-left" style={onOpenRelated && !relatedIconOnly ? { minWidth: 310 } : undefined}>Thao tác</th></tr></thead>
      <tbody>{loading ? <tr><td colSpan={columns.length + 1} className="py-14 text-center text-zinc-400">Đang tải…</td></tr> : rows.length === 0 ? <tr><td colSpan={columns.length + 1} className="py-14 text-center text-zinc-400">Không có dữ liệu.</td></tr> : rows.map((row, index) => <tr key={row.id} className={`border-t border-zinc-800 ${rowClassName?.(row, index, rows) || ""}`}>
        {columns.map((column) => <td key={column.key} className="px-4 py-3" style={column.minWidth ? { minWidth: column.minWidth } : undefined}>{column.render ? column.render(row) : row[column.key] ?? "—"}</td>)}
        <td className="px-4 py-3" style={onOpenRelated && !relatedIconOnly ? { minWidth: 310 } : undefined}><div className="flex items-center gap-3"><Button type="button" onClick={() => openView(row)} title="Xem" aria-label={`Xem ${row.name || row.code || row.sku || row.id}`} className="text-blue-400"><Eye size={17} /></Button>{onOpenRelated && <Button permission={relatedPermission} type="button" onClick={() => onOpenRelated(row)} title={relatedLabel} aria-label={`${relatedLabel} của ${row.name || row.code || row.sku || row.id}`} className="admin-catalog-related-action inline-flex items-center gap-1 whitespace-nowrap text-cyan-400"><Link size={17} />{!relatedIconOnly && relatedLabel}</Button>}{renderActions?.(row)}<Button permission={permissions.update} type="button" onClick={() => openEdit(row)} title="Sửa" className="text-amber-400"><Pencil size={17} /></Button>{isRestorable(row) ? <Button permission={permissions.update} type="button" onClick={() => restore(row)} title="Khôi phục" className="text-emerald-400"><RotateCcw size={17} /></Button> : <Button permission={permissions.delete} type="button" onClick={() => remove(row)} title="Vô hiệu hóa" className="text-red-400"><Trash2 size={17} /></Button>}</div></td>
      </tr>)}</tbody>
    </table></div><Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} /></section>

    {dialog && DialogComponent && <DialogComponent mode={dialog} product={form} onProductChange={setForm} onImageChange={(file, message) => { setForm((current) => ({ ...current, imageFile: file })); setImageError(message); }} onSubmit={save} onClose={() => setDialog(null)} uploading={uploading} imageError={imageError} permissions={permissions} hasPermission={hasPermission} renderDetails={(record, mode) => renderDetails?.(record, mode, load)} />}
    {dialog && !DialogComponent && <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/70 p-4" role="dialog" aria-modal="true"><div className={`max-h-[90vh] w-full ${dialogMaxWidth} overflow-y-auto rounded-3xl border border-zinc-700 bg-zinc-900 p-6`}>
      <div className="mb-5 flex items-center justify-between"><h2 className="text-xl font-semibold">{dialog === "create" ? "Thêm mới" : dialog === "edit" ? "Chỉnh sửa" : "Chi tiết"} {title}</h2><Button type="button" onClick={() => setDialog(null)}>Đóng</Button></div>
      {detailsTabLabel && form.id && <div className="mb-5 flex gap-2 border-b border-zinc-700 pb-3" role="tablist"><Button type="button" role="tab" aria-selected={dialogTab === "info"} onClick={() => setDialogTab("info")} className={dialogTab === "info" ? "rounded-lg bg-amber-500 px-4 py-2 text-black" : "rounded-lg px-4 py-2 text-zinc-300"}>Thông tin</Button><Button type="button" role="tab" aria-selected={dialogTab === "details"} onClick={() => setDialogTab("details")} className={dialogTab === "details" ? "rounded-lg bg-amber-500 px-4 py-2 text-black" : "rounded-lg px-4 py-2 text-zinc-300"}>{detailsTabLabel}</Button></div>}
      {(!detailsTabLabel || dialogTab === "info") && <>
      {detailsPosition === "before" && form.id && renderDetails?.(form, dialog, load)}
      <form onSubmit={save}>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">{fields.filter((field) => isCatalogFieldVisible(field, dialog, hasPermission)).map((field) => <label key={field.key} className={`min-w-0 space-y-1 text-sm ${field.fullWidth || ["textarea", "image"].includes(field.type) ? "md:col-span-2" : ""}`}><span className="text-zinc-300">{field.label}</span>{field.type === "image" ? <CatalogImageField file={form.imageFile} currentUrl={form[field.key]} disabled={dialog === "view"} onChange={(file, message) => { setForm((current) => ({ ...current, imageFile: file })); setImageError(message); }} /> : field.type === "catalog" ? <CatalogSelect field={field} value={form[field.key]} disabled={dialog === "view"} onChange={(value) => setForm((current) => ({ ...current, [field.key]: value }))} /> : field.type === "select" ? <select disabled={dialog === "view" || field.generated} required={field.required} value={form[field.key] ?? ""} onChange={(event) => setForm((value) => ({ ...value, [field.key]: event.target.value || null }))} className="h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3"><option value="">-- Chọn --</option>{field.options?.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select> : field.type === "textarea" ? <CatalogNoteField disabled={dialog === "view" || field.generated} required={field.required} value={form[field.key]} onChange={(note) => setForm((value) => ({ ...value, [field.key]: note }))} /> : <input disabled={dialog === "view" || field.generated} required={field.required} type={field.type || "text"} value={form[field.key] ?? ""} onChange={(event) => setForm((value) => ({ ...value, [field.key]: field.type === "number" ? (event.target.value === "" ? "" : Number(event.target.value)) : event.target.value }))} className="h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3" />}</label>)}</div>
      {imageError && <p className="mt-3 text-sm text-red-300">{imageError}</p>}
      {dialog !== "view" && <div className="mt-6 flex justify-end"><Button permission={dialog === "create" ? permissions.create : permissions.update} type="submit" loading={uploading} className="rounded-xl bg-amber-500 px-5 py-2.5 text-black">{uploading ? "Đang lưu và tải ảnh…" : "Lưu dữ liệu"}</Button></div>}
    </form>{detailsPosition === "after" && !detailsTabLabel && form.id && renderDetails?.(form, dialog, load)}</>}
      {detailsTabLabel && dialogTab === "details" && form.id && renderDetails?.(form, dialog, load)}
    </div></div>}
  </div>;
}
