import { useEffect, useRef, useState } from "react";
import Button from "../../components/common/Button";
import AdminDetailDialog from "../../components/admin/common/AdminDetailDialog";
import { requestAdmin } from "../../api/auth/adminSession";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import { ISSUE_TYPES, RECEIPT_STATUSES, fixedSource, headerPayload, isReceiptEditable, receiptActions, validateHeader, validateItem } from "./warehouseLogic";

const inputClass = "w-full rounded-xl border border-zinc-700 bg-zinc-800 px-3 py-2 text-zinc-100 disabled:opacity-60";
const buttonClass = "rounded-xl bg-zinc-800 px-4 py-2 disabled:opacity-50";
const date = (value) => value ? new Date(value).toLocaleString("vi-VN") : "—";
const blankItem = (_kind, type) => ({ productId: "", productVariantId: "", quantity: 1, costPrice: "", targetChannel: "OFFLINE", sourceChannel: fixedSource(type) || "OFFLINE" });

function CatalogSelect({ type, storeId, value, selectedLabel, label, onChange, disabled }) {
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [] });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    if (disabled) return;
    const controller = new AbortController();
    const timer = setTimeout(() => {
      setLoading(true);
      setError("");
      requestAdmin(`/api/inventory/${type}?${new URLSearchParams({ storeId, keyword, page, size: 20 })}`, { signal: controller.signal })
        .then(setData).catch(e => { if (!controller.signal.aborted) setError(e.message); })
        .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    }, 250);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [type, storeId, keyword, page, disabled]);
  return <div className="space-y-2"><label className="block text-sm">{label}
    {!disabled && <input aria-label={`Tìm ${label}`} className={`${inputClass} mt-1`} placeholder={`Tìm ${label.toLowerCase()}…`} value={keyword} onChange={e => { setKeyword(e.target.value); setPage(0); }} />}
    <select className={`${inputClass} mt-1`} value={value || ""} disabled={disabled || loading} onChange={e => onChange(e.target.value)}>
      <option value="">Chọn {label.toLowerCase()}</option>
      {value && !data.content?.some(row => row.id === value) && <option value={value}>{selectedLabel || value}</option>}
      {(data.content || []).map(row => <option key={row.id} value={row.id}>{row.name}{row.code ? ` (${row.code})` : ""}</option>)}
    </select></label>
    {loading && <p className="text-xs text-zinc-400">Đang tải…</p>}{error && <p role="alert" className="text-sm text-red-300">{error}</p>}
    {!disabled && (data.totalPages || 0) > 1 && <div className="flex items-center gap-2 text-xs"><button type="button" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Trước</button><span>{page + 1}/{data.totalPages}</span><button type="button" disabled={page + 1 >= data.totalPages} onClick={() => setPage(p => p + 1)}>Sau →</button></div>}
  </div>;
}

export default function WarehouseReceiptEditor({ storeId, storeName, kind, receiptId, onClose, onChanged }) {
  const { hasPermission } = useAdminPermissions();
  const isImport = kind === "import";
  const prefix = isImport ? "IMPORT_RECEIPT" : "EXPORT_RECEIPT";
  const base = isImport ? "/api/import-receipts" : "/api/export-receipts";
  const [header, setHeader] = useState({ storeId, supplierId: "", issueType: "ONLINE_TO_OFFLINE", note: "", reason: "", status: "DRAFT" });
  const [items, setItems] = useState([]);
  const [line, setLine] = useState(blankItem(kind, "ONLINE_TO_OFFLINE"));
  const [variantRows, setVariants] = useState([]);
  const variants = variantRows.filter(row => row.productId === line.productId);
  const [balanceResult, setBalance] = useState(null);
  const balance = balanceResult?.variantId === line.productVariantId ? balanceResult.data : null;
  const [loading, setLoading] = useState(Boolean(receiptId));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [confirmAction, setConfirmAction] = useState(null);
  const [dirty, setDirty] = useState(false);
  const active = useRef(true);
  const actionLock = useRef(false);
  useEffect(() => { active.current = true; return () => { active.current = false; }; }, []);
  const editable = !loading && (!receiptId || Boolean(header.id)) && isReceiptEditable(header.status) && hasPermission(`${prefix}_${header.id ? "UPDATE" : "CREATE"}`);
  const lineEditable = Boolean(header.id) && editable && hasPermission(`${prefix}_UPDATE`);
  const refresh = async (id) => {
    const [next, rows] = await Promise.all([requestAdmin(`${base}/${id}`), requestAdmin(`${base}/${id}/items`)]);
    if (next.storeId !== storeId) throw new Error("Phiếu không thuộc cửa hàng đang chọn.");
    if (active.current) { setHeader(next); setItems(rows); setDirty(false); }
  };
  useEffect(() => {
    let cancelled = false;
    if (!receiptId) return;
    Promise.all([requestAdmin(`${base}/${receiptId}`), requestAdmin(`${base}/${receiptId}/items`)])
      .then(([next, rows]) => {
        if (next.storeId !== storeId) throw new Error("Phiếu không thuộc cửa hàng đang chọn.");
        if (!cancelled) { setHeader(next); setItems(rows); }
      }).catch(e => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [base, receiptId, storeId]);
  useEffect(() => {
    if (!line.productId) return;
    const controller = new AbortController();
    requestAdmin(`/api/inventory/products/${line.productId}/variants?${new URLSearchParams({ storeId })}`, { signal: controller.signal })
      .then(rows => { if (!controller.signal.aborted) setVariants(rows.filter(row => row.productId === line.productId && row.active !== false)); })
      .catch(e => { if (!controller.signal.aborted) setError(e.message); });
    return () => controller.abort();
  }, [line.productId, storeId]);
  useEffect(() => {
    if (isImport || !line.productVariantId) return;
    const controller = new AbortController();
    requestAdmin(`/api/inventory/balances/${storeId}/${line.productVariantId}`, { signal: controller.signal })
      .then(row => { if (!controller.signal.aborted) setBalance({ variantId: line.productVariantId, data: row }); }).catch(e => { if (!controller.signal.aborted) setError(e.message); });
    return () => controller.abort();
  }, [line.productVariantId, storeId, isImport]);
  const run = async (fn) => {
    if (actionLock.current) return;
    actionLock.current = true; setBusy(true); setError(""); setMessage("");
    try { await fn(); } catch (e) { if (active.current) setError(e.message || "Không thể lưu phiếu."); }
    finally { actionLock.current = false; if (active.current) setBusy(false); }
  };
  const changeHeader = (patch) => { setHeader(h => ({ ...h, ...patch })); setDirty(true); };
  const saveHeader = () => run(async () => {
    const validation = validateHeader(header, kind);
    if (validation) throw new Error(validation);
    const saved = await requestAdmin(header.id ? `${base}/${header.id}` : base, { method: header.id ? "PUT" : "POST", body: headerPayload(header, kind) });
    if (active.current) { setHeader(saved); setDirty(false); setMessage("Đã lưu thông tin phiếu. Thêm các mặt hàng bên dưới trước khi gửi xác nhận."); }
    onChanged();
  });
  const saveLine = () => run(async () => {
    if (dirty) throw new Error("Lưu thông tin phiếu trước khi lưu mặt hàng.");
    if (!line.productId || !line.productVariantId) throw new Error("Vui lòng chọn sản phẩm và biến thể.");
    const currentBalance = isImport ? null : await requestAdmin(`/api/inventory/balances/${storeId}/${line.productVariantId}`);
    const validation = validateItem(line, kind, items, currentBalance, header.issueType);
    if (validation) throw new Error(validation);
    if (!variants.some(v => v.id === line.productVariantId && v.productId === line.productId)) throw new Error("Chọn biến thể thuộc sản phẩm đang chọn.");
    const body = { productId: line.productId, productVariantId: line.productVariantId, quantity: Number(line.quantity), ...(isImport ? { targetChannel: line.targetChannel, costPrice: Number(line.costPrice) } : { sourceChannel: line.sourceChannel }) };
    await requestAdmin(`${base}/${header.id}/items${line.id ? `/${line.id}` : ""}`, { method: line.id ? "PUT" : "POST", body });
    await refresh(header.id); setLine(blankItem(kind, header.issueType)); setMessage("Đã lưu mặt hàng."); onChanged();
  });
  const transition = (action) => run(async () => {
    if (dirty) throw new Error("Lưu thông tin phiếu trước khi chuyển trạng thái.");
    if (action !== "cancel" && !items.length) throw new Error("Phiếu cần ít nhất một mặt hàng.");
    await requestAdmin(`${base}/${header.id}/${action}`, { method: "POST" });
    await refresh(header.id); setConfirmAction(null); setMessage("Đã cập nhật trạng thái phiếu."); onChanged();
  });
  return <AdminDetailDialog open size="xl" title={`${isImport ? "Phiếu nhập" : "Phiếu xuất"} ${header.receiptCode || header.issueCode || "mới"}`} description={`Cửa hàng: ${storeName}`} onClose={() => { if (!busy) onClose(); }}>
    <div className="space-y-5">
      {error && <p role="alert" className="rounded-xl bg-red-500/10 p-3 text-red-300">{error}</p>}
      {message && <p role="status" className="rounded-xl bg-emerald-500/10 p-3 text-emerald-300">{message}</p>}
      {loading ? <p>Đang tải phiếu…</p> : <>
        <div className="flex flex-wrap justify-between gap-3"><span className="rounded-full bg-amber-500/15 px-3 py-1 text-amber-300">{RECEIPT_STATUSES[header.status] || header.status}</span><span className="text-sm text-zinc-400">Tồn kho chỉ thay đổi khi hoàn tất phiếu.</span></div>
        <div className="grid gap-4 md:grid-cols-2">
          {!isImport && <label>Loại xuất<select className={inputClass} disabled={!editable || busy || items.length > 0} value={header.issueType} onChange={e => { changeHeader({ issueType: e.target.value }); setLine(blankItem(kind, e.target.value)); }}>{Object.entries(ISSUE_TYPES).map(([id, label]) => <option key={id} value={id}>{label}</option>)}</select>{items.length > 0 && editable && <span className="text-xs text-zinc-400">Xóa các mặt hàng trước khi đổi loại xuất.</span>}</label>}
          {(isImport || header.issueType === "RETURN_TO_SUPPLIER") && <CatalogSelect type="suppliers" label="Nhà cung cấp *" storeId={storeId} value={header.supplierId} selectedLabel={header.supplierName} disabled={!editable || busy} onChange={supplierId => changeHeader({ supplierId })} />}
          {!isImport && <label>Lý do{header.issueType === "OTHER" ? " *" : ""}<input className={inputClass} disabled={!editable || busy} value={header.reason || ""} onChange={e => changeHeader({ reason: e.target.value })} /></label>}
          <label className="md:col-span-2">Ghi chú<textarea className={inputClass} disabled={!editable || busy} value={header.note || ""} onChange={e => changeHeader({ note: e.target.value })} /></label>
        </div>
        {editable && <Button className={`${buttonClass} bg-amber-600`} disabled={busy} onClick={saveHeader}>{header.id ? "Lưu thông tin phiếu" : "Tạo phiếu nháp"}</Button>}
        {header.id && <>
          <div className="grid gap-2 rounded-xl bg-zinc-800/50 p-3 text-xs text-zinc-400 md:grid-cols-2"><p>Người tạo: {header.receivedByName || header.issuedByName || header.receivedBy || header.issuedBy || "—"}</p><p>Ngày tạo: {date(header.createdAt)}</p><p>Người xác nhận: {header.approvedByName || header.approvedBy || "—"}</p><p>Ngày xác nhận: {date(header.confirmedAt)}</p><p>Người hoàn tất: {header.completedByName || header.completedBy || "—"}</p><p>Ngày hoàn tất: {date(header.completedAt)}</p></div>
          <div className="overflow-x-auto"><table className="w-full min-w-[640px] text-sm"><thead><tr className="text-left text-zinc-400">{["Sản phẩm / SKU", "Kênh kho", "Số lượng", ...(isImport ? ["Giá nhập", "Thành tiền"] : []), ""].map((title, i) => <th className="p-2" key={i}>{title}</th>)}</tr></thead><tbody>
            {items.length === 0 && <tr><td colSpan={isImport ? 6 : 4} className="p-6 text-center text-zinc-400">Chưa có mặt hàng. Thêm mặt hàng để gửi xác nhận.</td></tr>}
            {items.map(item => <tr key={item.id} className="border-t border-zinc-800"><td className="p-2">{item.productName || item.productId}<div className="text-xs text-zinc-400">{item.sku || item.productVariantId}</div></td><td className="p-2">{item.targetChannel || item.sourceChannel}</td><td className="p-2">{item.quantity}</td>{isImport && <><td className="p-2">{Number(item.costPrice).toLocaleString("vi-VN")} ₫</td><td className="p-2">{(Number(item.costPrice) * Number(item.quantity)).toLocaleString("vi-VN")} ₫</td></>}<td className="p-2">{lineEditable && <div className="flex gap-3"><button type="button" disabled={busy} className="text-amber-300" onClick={() => setLine(item)}>Sửa</button><button type="button" disabled={busy || dirty} className="text-red-300" onClick={() => run(async () => { await requestAdmin(`${base}/${header.id}/items/${item.id}`, { method: "DELETE" }); await refresh(header.id); if (line.id === item.id) setLine(blankItem(kind, header.issueType)); onChanged(); })}>Xóa</button></div>}</td></tr>)}
          </tbody></table></div>
          <p className="text-right font-semibold">Tổng: {items.reduce((sum, i) => sum + Number(i.quantity), 0)} sản phẩm{isImport && ` · ${items.reduce((sum, i) => sum + Number(i.quantity) * Number(i.costPrice), 0).toLocaleString("vi-VN")} ₫`}</p>
          {lineEditable && <section className="space-y-3 rounded-2xl border border-zinc-700 p-4"><h3 className="font-semibold">{line.id ? "Sửa mặt hàng" : "Thêm mặt hàng"}</h3><div className="grid gap-3 md:grid-cols-2">
            <CatalogSelect type="products" label="Sản phẩm" storeId={storeId} value={line.productId} selectedLabel={line.productName} disabled={busy} onChange={productId => setLine(l => ({ ...l, productId, productName: "", productVariantId: "" }))} />
            <label>Biến thể / SKU<select className={inputClass} value={line.productVariantId} disabled={busy || !line.productId} onChange={e => setLine(l => ({ ...l, productVariantId: e.target.value }))}><option value="">Chọn biến thể</option>{variants.map(v => <option key={v.id} value={v.id}>{v.sku} · {v.color} / {v.size}</option>)}</select></label>
            <label>Số lượng<input className={inputClass} disabled={busy} type="number" min="1" step="1" value={line.quantity} onChange={e => setLine(l => ({ ...l, quantity: e.target.value }))} /></label>
            <label>{isImport ? "Kênh nhập" : "Kênh xuất"}<select className={inputClass} disabled={busy || (!isImport && Boolean(fixedSource(header.issueType)))} value={isImport ? line.targetChannel : line.sourceChannel} onChange={e => setLine(l => ({ ...l, [isImport ? "targetChannel" : "sourceChannel"]: e.target.value }))}><option value="OFFLINE">Offline</option><option value="ONLINE">Online</option></select></label>
            {isImport && <label>Giá nhập (₫)<input className={inputClass} disabled={busy} type="number" min="0" step="0.01" value={line.costPrice} onChange={e => setLine(l => ({ ...l, costPrice: e.target.value }))} /></label>}
          </div>{!isImport && line.productVariantId && <p className="text-sm text-amber-300">{balance ? `Tồn khả dụng: Offline ${balance.offlineQuantity} · Online ${balance.onlineQuantity}` : "Đang tải tồn khả dụng…"}</p>}
          <div className="flex gap-3"><Button className={buttonClass} disabled={busy || !line.productVariantId || (!isImport && !balance)} onClick={saveLine}>{line.id ? "Lưu mặt hàng" : "Thêm vào phiếu"}</Button>{line.id && <Button className={buttonClass} disabled={busy} onClick={() => setLine(blankItem(kind, header.issueType))}>Bỏ sửa</Button>}</div></section>}
          <div className="flex flex-wrap gap-3">{receiptActions(header.status, kind, hasPermission).map(([action, label]) => <Button key={action} className={`${buttonClass} ${action === "cancel" ? "text-red-300" : "text-amber-300"}`} disabled={busy || dirty} onClick={() => setConfirmAction([action, label])}>{label}</Button>)}</div>
          {confirmAction && <div className="space-y-3 rounded-xl border border-amber-500/40 p-4"><p>{confirmAction[0] === "complete" ? "Hoàn tất sẽ cập nhật tồn kho và khóa phiếu. Xác nhận tiếp tục?" : `${confirmAction[1]} phiếu này?`}</p><div className="flex gap-3"><Button className={buttonClass} disabled={busy} onClick={() => transition(confirmAction[0])}>Xác nhận thao tác</Button><Button className={buttonClass} disabled={busy} onClick={() => setConfirmAction(null)}>Quay lại</Button></div></div>}
        </>}
      </>}
    </div>
  </AdminDetailDialog>;
}
