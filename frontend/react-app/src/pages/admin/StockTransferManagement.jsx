import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { ArrowLeftRight, Eye, PackageSearch, Send } from "lucide-react";
import Pagination from "../../components/common/Pagination";
import Button from "../../components/common/Button";
import StoreSelector from "../../components/common/StoreSelector";
import { useSystemNotification } from "../../components/common/SystemNotification";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../components/admin/common/AdminDialog";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import useStoreOptions from "../../hooks/useStoreOptions";
import { transferApi } from "../../api/admin/transferApi";
import { TRANSFER_STATUS, groupRequests, suggestedQuantity, transferActions, variantText } from "./transferLogic";

const PAGE_SIZE = 20;
const control = "h-11 rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-amber-400";
const formatTime = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "");
const DIRECTIONS = [["", "Tất cả"], ["IN", "Nhận về"], ["OUT", "Gửi đi"]];

function StatusBadge({ status }) {
  const meta = TRANSFER_STATUS[status] || { label: status, style: "" };
  return <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${meta.style}`}>{meta.label}</span>;
}

/** Điều chuyển tồn tại quầy giữa cửa hàng: hàng sắp hết (tạo yêu cầu theo gợi ý) và phiếu điều chuyển. */
export default function StockTransferManagement() {
  const permissions = useAdminPermissions();
  const { stores, loading: storesLoading } = useStoreOptions({ enabled: permissions.isGlobal });
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = searchParams.get("tab") === "low" ? "low" : "transfers";
  const [chosenStore, setChosenStore] = useState("");
  const storeId = permissions.isGlobal ? chosenStore : "";
  const openId = searchParams.get("id");
  const [version, setVersion] = useState(0);

  const setTab = (next) => setSearchParams(next === "low" ? { tab: "low" } : {}, { replace: true });
  const open = (id) => setSearchParams({ ...(tab === "low" ? { tab: "low" } : {}), id }, { replace: true });
  const close = () => setSearchParams(tab === "low" ? { tab: "low" } : {}, { replace: true });

  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100">
      <AdminCatalogPageHeader icon={ArrowLeftRight} eyebrow="Kho vận" title="Điều chuyển hàng"
        description="Cửa hàng thiếu hàng xin hàng từ cửa hàng còn dư; nơi gửi duyệt và xuất, nơi nhận xác nhận khi hàng tới. Chỉ điều chuyển tồn tại quầy."
        status={permissions.isGlobal ? "Toàn hệ thống" : permissions.currentStoreName || ""} />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex gap-2" role="tablist">
          {[["transfers", "Phiếu điều chuyển", ArrowLeftRight], ["low", "Hàng sắp hết", PackageSearch]].map(([key, label, Icon]) => (
            <button key={key} type="button" role="tab" aria-selected={tab === key} onClick={() => setTab(key)}
              className={`inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-sm ${tab === key ? "border-amber-400 bg-amber-500/10 text-amber-200" : "border-zinc-700 text-zinc-400 hover:text-zinc-200"}`}>
              <Icon size={16} />{label}
            </button>
          ))}
        </div>
        {permissions.isGlobal && (
          <StoreSelector stores={stores} value={chosenStore} onChange={setChosenStore} loading={storesLoading} label=""
            placeholder={tab === "low" ? "Chọn cửa hàng để xem" : "Tất cả cửa hàng"} className={control} />
        )}
      </div>

      {tab === "transfers"
        ? <TransferList storeId={storeId} version={version} onOpen={open} />
        : <LowStockList storeId={storeId} needStore={permissions.isGlobal && !chosenStore}
            onCreated={(codes) => { setVersion((v) => v + 1); setTab("transfers"); return codes; }} />}

      {openId && <TransferDialog id={openId} permissions={permissions} onClose={close} onChanged={() => setVersion((v) => v + 1)} />}
    </div>
  );
}

function TransferList({ storeId, version, onOpen }) {
  const [filters, setFilters] = useState({ direction: "", status: "" });
  const [page, setPage] = useState(1);
  const [state, setState] = useState({ key: null, rows: [], totalPages: 1, error: "" });
  const key = [storeId, filters.direction, filters.status, page, version].join("|");

  useEffect(() => {
    const controller = new AbortController();
    transferApi.list({ storeId, ...filters, page: page - 1, size: PAGE_SIZE }, { signal: controller.signal })
      .then((res) => setState({ key, rows: res.content || [], totalPages: Math.max(1, res.page?.totalPages || 1), error: "" }))
      .catch((failure) => { if (!controller.signal.aborted) setState({ key, rows: [], totalPages: 1, error: failure.message }); });
    return () => controller.abort();
  }, [key, storeId, filters, page]);

  const loading = state.key !== key;
  return (
    <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900">
      <div className="flex flex-wrap gap-3 border-b border-zinc-800 bg-zinc-950 p-4">
        <select className={control} aria-label="Chiều" value={filters.direction}
          onChange={(e) => { setPage(1); setFilters((f) => ({ ...f, direction: e.target.value })); }}>
          {DIRECTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
        <select className={control} aria-label="Trạng thái" value={filters.status}
          onChange={(e) => { setPage(1); setFilters((f) => ({ ...f, status: e.target.value })); }}>
          <option value="">Mọi trạng thái</option>
          {Object.entries(TRANSFER_STATUS).map(([value, meta]) => <option key={value} value={value}>{meta.label}</option>)}
        </select>
      </div>
      {state.error && <p role="alert" className="m-4 text-sm text-rose-300">{state.error}</p>}
      <div className="overflow-x-auto">
        <table className="w-full min-w-[860px] text-sm">
          <thead className="bg-zinc-800/70 text-zinc-300">
            <tr>{["Phiếu", "Từ → Đến", "Sản phẩm", "Trạng thái", ""].map((h) => <th key={h} className="px-4 py-3 text-left font-semibold">{h}</th>)}</tr>
          </thead>
          <tbody>
            {loading && <tr><td colSpan="5" className="px-4 py-12 text-center text-zinc-400">Đang tải…</td></tr>}
            {!loading && state.rows.length === 0 && <tr><td colSpan="5" className="px-4 py-12 text-center text-zinc-400">Chưa có phiếu điều chuyển.</td></tr>}
            {!loading && state.rows.map((t) => (
              <tr key={t.id} className="border-t border-zinc-800 align-top hover:bg-zinc-800/30">
                <td className="whitespace-nowrap px-4 py-3"><p className="font-semibold">{t.transferCode}</p><p className="text-xs text-zinc-500">{formatTime(t.createdAt)}</p></td>
                <td className="px-4 py-3 text-zinc-300">{t.fromStore.name} → {t.toStore.name}</td>
                <td className="px-4 py-3 text-xs text-zinc-300">
                  {t.items.slice(0, 3).map((i) => <p key={i.variantId}>{i.shipped ?? i.requested} × {i.productName} ({variantText(i)})</p>)}
                  {t.items.length > 3 && <p className="text-zinc-500">+{t.items.length - 3} sản phẩm khác</p>}
                </td>
                <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
                <td className="px-4 py-3"><button type="button" className="inline-flex items-center gap-1 text-amber-300" onClick={() => onOpen(t.id)}><Eye size={15} />Xem</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination currentPage={page} totalPages={state.totalPages} onPageChange={setPage} />
    </section>
  );
}

function LowStockList({ storeId, needStore, onCreated }) {
  const notification = useSystemNotification();
  const [keyword, setKeyword] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(1);
  const [state, setState] = useState({ key: null, rows: [], totalPages: 1, total: 0, error: "" });
  const [picked, setPicked] = useState({});
  const [sending, setSending] = useState(false);
  const key = [storeId, query, page].join("|");

  useEffect(() => {
    if (needStore) return undefined;
    const controller = new AbortController();
    transferApi.lowStock({ storeId, keyword: query, page: page - 1, size: PAGE_SIZE }, { signal: controller.signal })
      .then((res) => setState({ key, rows: res.content || [], totalPages: Math.max(1, res.page?.totalPages || 1), total: res.page?.totalElements || 0, error: "" }))
      .catch((failure) => { if (!controller.signal.aborted) setState({ key, rows: [], totalPages: 1, total: 0, error: failure.message }); });
    return () => controller.abort();
  }, [key, storeId, query, page, needStore]);

  const groups = useMemo(() => groupRequests(state.rows, picked), [state.rows, picked]);
  if (needStore) return <p className="rounded-2xl border border-zinc-800 bg-zinc-900 p-6 text-sm text-zinc-400">Chọn một cửa hàng để xem hàng sắp hết.</p>;

  const toggle = (row) => setPicked((current) => {
    const next = { ...current };
    if (next[row.variantId]) delete next[row.variantId];
    else next[row.variantId] = Math.max(1, suggestedQuantity(row));
    return next;
  });

  const send = async () => {
    setSending(true);
    try {
      const codes = [];
      for (const group of groups) {
        const created = await transferApi.create({ fromStoreId: group.fromStoreId, toStoreId: storeId || null, items: group.items });
        codes.push(created.transferCode);
      }
      notification.success(`Đã gửi ${codes.length} yêu cầu điều chuyển: ${codes.join(", ")}.`);
      setPicked({});
      onCreated(codes);
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setSending(false);
    }
  };

  const loading = state.key !== key;
  return (
    <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900">
      <form className="flex flex-wrap items-center gap-3 border-b border-zinc-800 bg-zinc-950 p-4"
        onSubmit={(e) => { e.preventDefault(); setPage(1); setQuery(keyword.trim()); }}>
        <input className={`${control} min-w-[16rem] flex-1`} value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Tìm tên sản phẩm hoặc SKU" />
        <span className="text-sm text-zinc-500">{state.total} biến thể còn ≤ 5 (tại quầy + online)</span>
        <Button type="button" variant="primary" permission="TRANSFER_MANAGE" disabled={groups.length === 0} loading={sending}
          className="ml-auto inline-flex h-11 items-center gap-2 rounded-xl px-4 text-sm font-semibold" onClick={send}>
          <Send size={15} />Gửi yêu cầu ({groups.reduce((n, g) => n + g.items.length, 0)})
        </Button>
      </form>
      {state.error && <p role="alert" className="m-4 text-sm text-rose-300">{state.error}</p>}
      <div className="overflow-x-auto">
        <table className="w-full min-w-[900px] text-sm">
          <thead className="bg-zinc-800/70 text-zinc-300">
            <tr>{["", "Sản phẩm", "Tại quầy / Online", "Đang về", "Gợi ý lấy từ", "Số xin"].map((h) => <th key={h} className="px-4 py-3 text-left font-semibold">{h}</th>)}</tr>
          </thead>
          <tbody>
            {loading && <tr><td colSpan="6" className="px-4 py-12 text-center text-zinc-400">Đang tải…</td></tr>}
            {!loading && state.rows.length === 0 && <tr><td colSpan="6" className="px-4 py-12 text-center text-zinc-400">Không có biến thể nào sắp hết.</td></tr>}
            {!loading && state.rows.map((row) => {
              const selected = picked[row.variantId] !== undefined;
              return (
                <tr key={row.variantId} className="border-t border-zinc-800 hover:bg-zinc-800/30">
                  <td className="px-4 py-3">
                    <input type="checkbox" aria-label={`Chọn ${row.sku}`} disabled={!row.suggestedStore} checked={selected} onChange={() => toggle(row)} />
                  </td>
                  <td className="px-4 py-3"><p className="text-zinc-100">{row.productName}</p><p className="text-xs text-zinc-500">{row.sku} · {variantText(row)}</p></td>
                  <td className="px-4 py-3"><span className={row.total === 0 ? "text-rose-300" : "text-amber-200"}>{row.counter} / {row.online}</span></td>
                  <td className="px-4 py-3 text-zinc-400">{row.incoming || "—"}</td>
                  <td className="px-4 py-3">{row.suggestedStore
                    ? <span className="text-zinc-200">{row.suggestedStore.name} <span className="text-zinc-500">({row.suggestedStock} tại quầy)</span></span>
                    : <span className="text-zinc-500">Không cửa hàng nào dư</span>}</td>
                  <td className="px-4 py-3">
                    {selected && (
                      <input type="number" min="1" className="w-20 rounded-lg border border-zinc-700 bg-zinc-950 px-2 py-1"
                        value={picked[row.variantId]} aria-label={`Số xin ${row.sku}`}
                        onChange={(e) => setPicked((c) => ({ ...c, [row.variantId]: Math.max(1, Number.parseInt(e.target.value, 10) || 1) }))} />
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <Pagination currentPage={page} totalPages={state.totalPages} onPageChange={setPage} />
    </section>
  );
}

function TransferDialog({ id, permissions, onClose, onChanged }) {
  const notification = useSystemNotification();
  const [transfer, setTransfer] = useState(null);
  const [error, setError] = useState("");
  const [shipped, setShipped] = useState({});
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    transferApi.detail(id, { signal: controller.signal })
      .then((result) => {
        setTransfer(result);
        setShipped(Object.fromEntries(result.items.map((i) => [i.variantId, Math.min(i.requested, i.sourceStock)])));
      })
      .catch((failure) => { if (!controller.signal.aborted) setError(failure.message); });
    return () => controller.abort();
  }, [id]);

  const actions = transferActions(transfer, permissions);
  const act = async (action, message) => {
    setBusy(true);
    setError("");
    try {
      setTransfer(await action());
      setRejecting(false);
      notification.success(message);
      onChanged();
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  };
  const editable = transfer?.status === "REQUESTED" && actions.includes("ship");

  return (
    <AdminDialog open onClose={onClose} size="lg">
      <AdminDialogHeader title={transfer ? `Phiếu ${transfer.transferCode}` : "Phiếu điều chuyển"}
        description={transfer ? `${transfer.fromStore.name} → ${transfer.toStore.name}` : ""} onClose={onClose} />
      <AdminDialogBody className="space-y-4 text-sm">
        {!transfer && !error && <p className="text-zinc-400">Đang tải…</p>}
        {transfer && <>
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge status={transfer.status} />
            <span className="text-zinc-500">Tạo {formatTime(transfer.createdAt)}</span>
            {transfer.shippedAt && <span className="text-zinc-500">· Xuất {formatTime(transfer.shippedAt)}</span>}
            {transfer.receivedAt && <span className="text-zinc-500">· Nhận {formatTime(transfer.receivedAt)}</span>}
          </div>
          {transfer.note && <blockquote className="rounded-xl border border-zinc-800 bg-zinc-950 p-3 text-zinc-300">{transfer.note}</blockquote>}
          {transfer.rejectReason && <p className="text-rose-300">Lý do từ chối: {transfer.rejectReason}</p>}
          <table className="w-full overflow-hidden rounded-xl border border-zinc-800 text-sm">
            <thead className="bg-zinc-800/60 text-zinc-300"><tr>
              <th className="px-3 py-2 text-left">Sản phẩm</th><th className="px-3 py-2 text-left">Xin</th>
              <th className="px-3 py-2 text-left">Tồn nơi gửi</th><th className="px-3 py-2 text-left">Xuất</th></tr></thead>
            <tbody>
              {transfer.items.map((item) => (
                <tr key={item.variantId} className="border-t border-zinc-800">
                  <td className="px-3 py-2">{item.productName}<p className="text-xs text-zinc-500">{item.sku} · {variantText(item)}</p></td>
                  <td className="px-3 py-2">{item.requested}</td>
                  <td className="px-3 py-2">{item.sourceStock}</td>
                  <td className="px-3 py-2">{editable
                    ? <input type="number" min="0" max={item.requested} className="w-20 rounded-lg border border-zinc-700 bg-zinc-950 px-2 py-1"
                        value={shipped[item.variantId] ?? 0} aria-label={`Số xuất ${item.sku}`}
                        onChange={(e) => setShipped((s) => ({ ...s, [item.variantId]: Math.max(0, Number.parseInt(e.target.value, 10) || 0) }))} />
                    : (item.shipped ?? "—")}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {rejecting && (
            <label className="block space-y-1.5 text-zinc-400"><span>Lý do từ chối</span>
              <textarea rows={2} maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)}
                className="w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-zinc-100 outline-none focus:border-amber-400" /></label>
          )}
        </>}
        {error && <p role="alert" className="text-rose-300">{error}</p>}
      </AdminDialogBody>
      <AdminDialogFooter>
        <Button type="button" variant="secondary" className="rounded-xl px-4 py-2.5 text-sm" onClick={onClose} disabled={busy}>Đóng</Button>
        {rejecting && <Button type="button" variant="danger-solid" permission="TRANSFER_MANAGE" className="rounded-xl px-4 py-2.5 text-sm font-semibold" loading={busy}
          onClick={() => (reason.trim() ? act(() => transferApi.reject(id, reason.trim()), "Đã từ chối yêu cầu.") : setError("Vui lòng nhập lý do."))}>Xác nhận từ chối</Button>}
        {!rejecting && actions.includes("cancel") && <Button type="button" variant="danger" permission="TRANSFER_MANAGE" className="rounded-xl px-4 py-2.5 text-sm" loading={busy}
          onClick={async () => { if (await notification.confirm("Hủy yêu cầu điều chuyển này?", { confirmText: "Hủy yêu cầu" })) act(() => transferApi.cancel(id), "Đã hủy yêu cầu."); }}>Hủy yêu cầu</Button>}
        {!rejecting && actions.includes("reject") && <Button type="button" variant="danger" permission="TRANSFER_MANAGE" className="rounded-xl px-4 py-2.5 text-sm" onClick={() => setRejecting(true)}>Từ chối</Button>}
        {!rejecting && actions.includes("ship") && <Button type="button" variant="primary" permission="TRANSFER_MANAGE" className="rounded-xl px-4 py-2.5 text-sm font-semibold" loading={busy}
          onClick={() => act(() => transferApi.ship(id, shipped), "Đã xuất hàng, nơi nhận sẽ được thông báo.")}>Duyệt & xuất hàng</Button>}
        {actions.includes("receive") && <Button type="button" variant="primary" permission="TRANSFER_MANAGE" className="rounded-xl px-4 py-2.5 text-sm font-semibold" loading={busy}
          onClick={() => act(() => transferApi.receive(id), "Đã nhận hàng, tồn kho đã được cộng.")}>Đã nhận hàng</Button>}
      </AdminDialogFooter>
    </AdminDialog>
  );
}
