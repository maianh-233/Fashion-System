import { useEffect, useRef, useState } from "react";
import { PenSquare, Trash2, Plus } from "lucide-react";
import Button from "../../components/common/Button";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../components/admin/common/AdminDialog";
import { AdminField, AdminFormSection } from "../../components/admin/common/AdminForm";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { requestAdmin } from "../../api/auth/adminSession";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import { ISSUE_TYPES, RECEIPT_STATUSES, RECEIPT_STATUS_STYLES, fixedSource, headerPayload, isReceiptEditable, receiptActions, validateHeader, validateItem } from "./warehouseLogic";

const date = (value) => value ? new Date(value).toLocaleString("vi-VN") : "—";
const money = (value) => `${Number(value || 0).toLocaleString("vi-VN")} ₫`;
const blankItem = (_kind, type) => ({ productId: "", productVariantId: "", quantity: 1, costPrice: "", targetChannel: "OFFLINE", sourceChannel: fixedSource(type) || "OFFLINE" });
const CHANNELS = { OFFLINE: "Offline", ONLINE: "Online" };
const ACTION_STYLES = {
  cancel: "px-4 py-2 rounded bg-red-500/15 text-red-300 hover:bg-red-500/25",
  complete: "px-6 py-2 rounded bg-emerald-500 hover:bg-emerald-600 text-black font-semibold",
};
const primaryClass = "px-6 py-2 rounded bg-orange-500 hover:bg-orange-600 text-black font-semibold disabled:opacity-60";
const secondaryClass = "px-4 py-2 rounded bg-gray-700 hover:bg-gray-600 disabled:opacity-60";

function CatalogSelect({ type, storeId, value, selectedLabel, label, required, onChange, disabled }) {
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
  return <div className="admin-field">
    <span className="admin-field__label">{label}{required && " *"}</span>
    {!disabled && <input aria-label={`Tìm ${label.toLowerCase()}`} className="admin-field__control" placeholder={`Tìm ${label.toLowerCase()}...`} value={keyword} onChange={e => { setKeyword(e.target.value); setPage(0); }} />}
    <select aria-label={label} className="admin-field__control" value={value || ""} disabled={disabled || loading} onChange={e => onChange(e.target.value)}>
      <option value="">{loading ? "Đang tải..." : `Chọn ${label.toLowerCase()}`}</option>
      {value && !data.content?.some(row => row.id === value) && <option value={value}>{selectedLabel || value}</option>}
      {(data.content || []).map(row => <option key={row.id} value={row.id}>{row.name}{row.code ? ` (${row.code})` : ""}</option>)}
    </select>
    {error && <p role="alert" className="text-xs text-red-400">{error}</p>}
    {!disabled && (data.totalPages || 0) > 1 && <div className="flex items-center gap-3 text-xs text-zinc-400">
      <button type="button" className="hover:text-amber-300 disabled:opacity-40" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Trước</button>
      <span>{page + 1}/{data.totalPages}</span>
      <button type="button" className="hover:text-amber-300 disabled:opacity-40" disabled={page + 1 >= data.totalPages} onClick={() => setPage(p => p + 1)}>Sau →</button>
    </div>}
  </div>;
}

export default function WarehouseReceiptEditor({ storeId, storeName, kind, receiptId, readOnly = false, onClose, onChanged }) {
  const notification = useSystemNotification();
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
  const [dirty, setDirty] = useState(false);
  const active = useRef(true);
  const actionLock = useRef(false);
  useEffect(() => { active.current = true; return () => { active.current = false; }; }, []);
  const editable = !readOnly && !loading && (!receiptId || Boolean(header.id)) && isReceiptEditable(header.status) && hasPermission(`${prefix}_${header.id ? "UPDATE" : "CREATE"}`);
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
    actionLock.current = true; setBusy(true); setError("");
    try { await fn(); } catch (e) { if (active.current) setError(e.message || "Không thể lưu phiếu."); }
    finally { actionLock.current = false; if (active.current) setBusy(false); }
  };
  const close = () => { if (!busy) onClose(); };
  const changeHeader = (patch) => { setHeader(h => ({ ...h, ...patch })); setDirty(true); };
  const saveHeader = () => run(async () => {
    const validation = validateHeader(header, kind);
    if (validation) throw new Error(validation);
    const saved = await requestAdmin(header.id ? `${base}/${header.id}` : base, { method: header.id ? "PUT" : "POST", body: headerPayload(header, kind) });
    if (active.current) { setHeader(saved); setDirty(false); }
    notification.success(header.id ? "Đã lưu thông tin phiếu." : `Đã tạo phiếu nháp ${saved.receiptCode || saved.issueCode || ""}. Thêm các mặt hàng trước khi gửi xác nhận.`);
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
    await refresh(header.id); setLine(blankItem(kind, header.issueType)); notification.success("Đã lưu mặt hàng."); onChanged();
  });
  const removeLine = async (item) => {
    const accepted = await notification.confirm({ title: "Xóa mặt hàng", message: `Xóa ${item.productName || item.sku || "mặt hàng"} khỏi phiếu?`, destructive: true });
    if (!accepted) return;
    run(async () => {
      await requestAdmin(`${base}/${header.id}/items/${item.id}`, { method: "DELETE" });
      await refresh(header.id); if (line.id === item.id) setLine(blankItem(kind, header.issueType)); onChanged();
    });
  };
  const transition = async (action, label) => {
    if (dirty) { setError("Lưu thông tin phiếu trước khi chuyển trạng thái."); return; }
    if (action !== "cancel" && !items.length) { setError("Phiếu cần ít nhất một mặt hàng."); return; }
    const accepted = await notification.confirm({
      title: label,
      message: action === "complete" ? "Hoàn tất sẽ cập nhật tồn kho và khóa phiếu. Xác nhận tiếp tục?" : `${label} phiếu ${header.receiptCode || header.issueCode || ""}?`,
      destructive: action === "cancel",
    });
    if (!accepted) return;
    run(async () => {
      await requestAdmin(`${base}/${header.id}/${action}`, { method: "POST" });
      await refresh(header.id); notification.success("Đã cập nhật trạng thái phiếu."); onChanged();
    });
  };

  const code = header.receiptCode || header.issueCode;
  const kindLabel = isImport ? "phiếu nhập" : "phiếu xuất";
  const title = !receiptId && !header.id ? `Tạo ${kindLabel}` : `${editable ? "Chỉnh sửa" : "Chi tiết"} ${kindLabel}`;
  const actions = header.id ? receiptActions(header.status, kind, hasPermission) : [];
  const totalQuantity = items.reduce((sum, i) => sum + Number(i.quantity), 0);
  const totalAmount = items.reduce((sum, i) => sum + Number(i.quantity) * Number(i.costPrice), 0);
  const itemHeaders = [["Sản phẩm / SKU", "text-left"], ["Kênh kho", "text-center"], ["Số lượng", "text-center"], ...(isImport ? [["Giá nhập", "text-right"], ["Thành tiền", "text-right"]] : []), ...(lineEditable ? [["Thao tác", "text-center"]] : [])];

  return <AdminDialog open onClose={close} size="xl">
    <AdminDialogHeader title={title} description={`${code ? `${code} · ` : ""}Cửa hàng: ${storeName}`} onClose={close}>
      {header.id && <span className={`mt-2 inline-block px-4 py-1 rounded-full text-xs whitespace-nowrap ${RECEIPT_STATUS_STYLES[header.status] || "bg-zinc-700"}`}>{RECEIPT_STATUSES[header.status] || header.status}</span>}
    </AdminDialogHeader>

    <AdminDialogBody className="space-y-8">
      {loading ? <p className="text-sm text-zinc-400">Đang tải phiếu...</p> : <>
        <AdminFormSection title="Thông tin phiếu" description="Tồn kho chỉ thay đổi khi phiếu được hoàn tất.">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <AdminField label="Mã phiếu" disabled readOnly value={code || "Hệ thống tự sinh khi lưu"} />
            <AdminField label="Cửa hàng" disabled readOnly value={storeName} />
            {!isImport && <AdminField as="select" label="Loại xuất *" disabled={!editable || busy || items.length > 0} value={header.issueType}
              onChange={e => { changeHeader({ issueType: e.target.value }); setLine(blankItem(kind, e.target.value)); }}>
              {Object.entries(ISSUE_TYPES).map(([id, label]) => <option key={id} value={id}>{label}</option>)}
            </AdminField>}
            {(isImport || header.issueType === "RETURN_TO_SUPPLIER") && <CatalogSelect type="suppliers" label="Nhà cung cấp" required storeId={storeId} value={header.supplierId} selectedLabel={header.supplierName} disabled={!editable || busy} onChange={supplierId => changeHeader({ supplierId })} />}
            {!isImport && <AdminField label={`Lý do${header.issueType === "OTHER" ? " *" : ""}`} disabled={!editable || busy} value={header.reason || ""} onChange={e => changeHeader({ reason: e.target.value })} />}
            <AdminField as="textarea" rows={3} label="Ghi chú" containerClassName="sm:col-span-2" disabled={!editable || busy} value={header.note || ""} onChange={e => changeHeader({ note: e.target.value })} />
          </div>
          {!isImport && items.length > 0 && editable && <p className="mt-2 text-xs text-zinc-400">Xóa các mặt hàng trước khi đổi loại xuất.</p>}
        </AdminFormSection>

        {header.id && <AdminFormSection title="Danh sách mặt hàng" description={`${items.length} dòng · ${totalQuantity.toLocaleString("vi-VN")} sản phẩm${isImport ? ` · ${money(totalAmount)}` : ""}`}>
          <div className="overflow-x-auto rounded-xl border border-zinc-800">
            <table className="w-full min-w-[640px] text-sm">
              <thead className="bg-zinc-800 text-zinc-300"><tr>{itemHeaders.map(([label, align]) => <th key={label} className={`px-4 py-3 ${align}`}>{label}</th>)}</tr></thead>
              <tbody>
                {items.length === 0 && <tr><td colSpan={itemHeaders.length} className="py-12 text-center text-zinc-500">Chưa có mặt hàng. Thêm mặt hàng để gửi xác nhận.</td></tr>}
                {items.map(item => <tr key={item.id} className={`border-t border-zinc-700 ${line.id === item.id ? "bg-amber-500/5" : ""}`}>
                  <td className="px-4 py-4 font-medium">{item.productName || item.productId}<span className="block text-xs text-zinc-400">{item.sku || item.productVariantId}</span></td>
                  <td className="px-4 py-4 text-center">{CHANNELS[item.targetChannel || item.sourceChannel] || item.targetChannel || item.sourceChannel}</td>
                  <td className="px-4 py-4 text-center">{Number(item.quantity).toLocaleString("vi-VN")}</td>
                  {isImport && <><td className="px-4 py-4 text-right whitespace-nowrap">{money(item.costPrice)}</td><td className="px-4 py-4 text-right whitespace-nowrap font-medium">{money(Number(item.costPrice) * Number(item.quantity))}</td></>}
                  {lineEditable && <td className="px-4 py-4"><div className="flex items-center justify-center gap-3">
                    <Button disabled={busy} onClick={() => setLine(item)} className="text-amber-400 hover:text-amber-300 transition-colors" title="Sửa"><PenSquare size={18} /></Button>
                    <Button disabled={busy || dirty} onClick={() => removeLine(item)} className="text-red-400 hover:text-red-300 transition-colors" title="Xóa"><Trash2 size={18} /></Button>
                  </div></td>}
                </tr>)}
              </tbody>
              {items.length > 0 && <tfoot><tr className="border-t border-zinc-700 font-semibold">
                <td className="px-4 py-3" colSpan={2}>Tổng cộng</td>
                <td className="px-4 py-3 text-center">{totalQuantity.toLocaleString("vi-VN")}</td>
                {isImport && <><td /><td className="px-4 py-3 text-right whitespace-nowrap text-amber-300">{money(totalAmount)}</td></>}
                {lineEditable && <td />}
              </tr></tfoot>}
            </table>
          </div>
        </AdminFormSection>}

        {lineEditable && <AdminFormSection title={line.id ? "Sửa mặt hàng" : "Thêm mặt hàng"}>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <CatalogSelect type="products" label="Sản phẩm" required storeId={storeId} value={line.productId} selectedLabel={line.productName} disabled={busy} onChange={productId => setLine(l => ({ ...l, productId, productName: "", productVariantId: "" }))} />
            <AdminField as="select" label="Biến thể / SKU *" value={line.productVariantId} disabled={busy || !line.productId} onChange={e => setLine(l => ({ ...l, productVariantId: e.target.value }))}>
              <option value="">Chọn biến thể</option>
              {variants.map(v => <option key={v.id} value={v.id}>{v.sku} · {v.color} / {v.size}</option>)}
            </AdminField>
            <AdminField label="Số lượng *" type="number" min="1" step="1" disabled={busy} value={line.quantity} onChange={e => setLine(l => ({ ...l, quantity: e.target.value }))} />
            <AdminField as="select" label={isImport ? "Kênh nhập *" : "Kênh xuất *"} disabled={busy || (!isImport && Boolean(fixedSource(header.issueType)))} value={isImport ? line.targetChannel : line.sourceChannel}
              onChange={e => setLine(l => ({ ...l, [isImport ? "targetChannel" : "sourceChannel"]: e.target.value }))}>
              <option value="OFFLINE">Offline</option><option value="ONLINE">Online</option>
            </AdminField>
            {isImport && <AdminField label="Giá nhập (₫) *" type="number" min="0" step="0.01" disabled={busy} value={line.costPrice} onChange={e => setLine(l => ({ ...l, costPrice: e.target.value }))} />}
          </div>
          {!isImport && line.productVariantId && <p className="mt-3 rounded-xl border border-amber-500/40 bg-amber-500/10 px-4 py-3 text-sm text-amber-300">{balance ? `Tồn khả dụng: Offline ${balance.offlineQuantity} · Online ${balance.onlineQuantity}` : "Đang tải tồn khả dụng..."}</p>}
          <div className="mt-4 flex justify-end gap-3">
            {line.id && <Button disabled={busy} onClick={() => setLine(blankItem(kind, header.issueType))} className={secondaryClass}>Bỏ sửa</Button>}
            <Button disabled={busy || !line.productVariantId || (!isImport && !balance)} onClick={saveLine} className="flex items-center gap-2 rounded-lg bg-orange-500 px-4 py-2 text-white hover:bg-orange-600 disabled:opacity-60">
              {!line.id && <Plus size={18} />}{line.id ? "Lưu mặt hàng" : "Thêm vào phiếu"}
            </Button>
          </div>
        </AdminFormSection>}

        {header.id && <AdminFormSection title="Thông tin xử lý">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <AdminField label="Người tạo" disabled readOnly value={header.receivedByName || header.issuedByName || "—"} />
            <AdminField label="Ngày tạo" disabled readOnly value={date(header.createdAt)} />
            <AdminField label="Người xác nhận" disabled readOnly value={header.approvedByName || "—"} />
            <AdminField label="Ngày xác nhận" disabled readOnly value={date(header.confirmedAt)} />
            <AdminField label="Người hoàn tất" disabled readOnly value={header.completedByName || "—"} />
            <AdminField label="Ngày hoàn tất" disabled readOnly value={date(header.completedAt)} />
          </div>
        </AdminFormSection>}
      </>}

      {error && <p role="alert" className="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>}
    </AdminDialogBody>

    <AdminDialogFooter>
      <Button type="button" disabled={busy} onClick={close} className={secondaryClass}>Đóng</Button>
      {actions.map(([action, label]) => <Button key={action} disabled={busy || dirty || loading} onClick={() => transition(action, label)} className={ACTION_STYLES[action] || primaryClass}>{label}</Button>)}
      {editable && <Button disabled={busy || (header.id && !dirty)} onClick={saveHeader} className={primaryClass}>{busy ? "Đang lưu..." : header.id ? "Lưu thông tin phiếu" : "Tạo phiếu nháp"}</Button>}
    </AdminDialogFooter>
  </AdminDialog>;
}
