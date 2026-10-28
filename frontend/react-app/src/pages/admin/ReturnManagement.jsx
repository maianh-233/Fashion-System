import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Eye, RotateCcw, Search } from "lucide-react";
import Pagination from "../../components/common/Pagination";
import Button from "../../components/common/Button";
import { useSystemNotification } from "../../components/common/SystemNotification";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../components/admin/common/AdminDialog";
import { returnAdminApi } from "../../api/admin/returnApi";
import {
  RETURN_STATUS_OPTIONS, RETURN_STATUS_STYLES, availableActions, defaultConditions, formatMoney, normalizeReturnPage,
} from "./returnManagementLogic";

const PAGE_SIZE = 20;
const control = "h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none transition focus:border-amber-400";
const formatTime = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "");
const variant = (color, size) => [color, size].filter(Boolean).join(" / ");
const TYPE_LABELS = { RETURN: "Trả hàng", EXCHANGE: "Đổi size / màu" };

function StatusBadge({ request }) {
  return (
    <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${RETURN_STATUS_STYLES[request.status] || ""}`}>
      {RETURN_STATUS_OPTIONS.find((o) => o.value === request.status)?.label || request.status}
    </span>
  );
}

/** Yêu cầu đổi / trả của khách: duyệt, từ chối, nhận hàng tại quầy (chọn tình trạng từng món) và xác nhận hoàn tiền. */
export default function ReturnManagement() {
  const notification = useSystemNotification();
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(1);
  const [draft, setDraft] = useState({ status: "", keyword: "" });
  const [filters, setFilters] = useState({ status: "", keyword: "" });
  const [version, setVersion] = useState(0);
  const [data, setData] = useState({ rows: [], totalPages: 1, totalElements: 0 });
  const [state, setState] = useState("loading");
  const [error, setError] = useState("");
  const openId = searchParams.get("id");

  useEffect(() => {
    const controller = new AbortController();
    returnAdminApi.list({ ...filters, page: page - 1, size: PAGE_SIZE }, { signal: controller.signal })
      .then((response) => { setData(normalizeReturnPage(response)); setState("ready"); })
      .catch((failure) => {
        if (!controller.signal.aborted) { setError(failure.message || "Không tải được yêu cầu đổi trả."); setState("error"); }
      });
    return () => controller.abort();
  }, [filters, page, version]);

  const reload = () => { setState("loading"); setError(""); setVersion((value) => value + 1); };
  const apply = (event) => { event.preventDefault(); setState("loading"); setPage(1); setFilters({ ...draft }); };
  const open = (id) => setSearchParams({ id }, { replace: true });
  const close = () => setSearchParams({}, { replace: true });

  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100">
      <AdminCatalogPageHeader icon={RotateCcw} eyebrow="Bán hàng" title="Đổi trả"
        description="Khách gửi yêu cầu trong 14 ngày sau khi đơn hoàn thành. Duyệt yêu cầu, nhận hàng tại quầy và hoàn tất để nhập kho, đổi phân loại hoặc hoàn tiền."
        status={`${data.totalElements.toLocaleString("vi-VN")} yêu cầu`} />

      <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900">
        <form className="flex flex-wrap items-end gap-3 border-b border-zinc-800 bg-zinc-950 p-4 sm:p-6" onSubmit={apply}>
          <label className="min-w-[16rem] flex-1 space-y-1.5 text-sm text-zinc-400">
            <span>Tìm kiếm</span>
            <input className={control} value={draft.keyword} placeholder="Mã yêu cầu, mã đơn, tên / SĐT khách"
              onChange={(e) => setDraft((d) => ({ ...d, keyword: e.target.value }))} />
          </label>
          <label className="w-56 space-y-1.5 text-sm text-zinc-400">
            <span>Trạng thái</span>
            <select className={control} value={draft.status} onChange={(e) => setDraft((d) => ({ ...d, status: e.target.value }))}>
              {RETURN_STATUS_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </label>
          <Button type="submit" variant="primary" className="inline-flex h-11 items-center gap-2 rounded-xl px-4 text-sm font-semibold">
            <Search size={16} />Lọc
          </Button>
        </form>

        {error && <p role="alert" className="m-4 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-sm text-rose-200">{error}</p>}
        <div className="overflow-x-auto">
          <table className="w-full min-w-[960px] text-sm">
            <thead className="bg-zinc-800/70 text-zinc-300">
              <tr>{["Yêu cầu", "Khách / đơn", "Sản phẩm", "Trạng thái", ""].map((head) =>
                <th key={head} className="px-4 py-3 text-left font-semibold">{head}</th>)}</tr>
            </thead>
            <tbody>
              {state === "loading" && <tr><td colSpan="5" className="px-4 py-12 text-center text-zinc-400">Đang tải…</td></tr>}
              {state === "ready" && data.rows.length === 0 && (
                <tr><td colSpan="5" className="px-4 py-12 text-center text-zinc-400">Chưa có yêu cầu đổi trả.</td></tr>
              )}
              {state === "ready" && data.rows.map((request) => (
                <tr key={request.id} className="border-t border-zinc-800 align-top transition-colors hover:bg-zinc-800/30">
                  <td className="whitespace-nowrap px-4 py-3">
                    <p className="font-semibold text-zinc-100">{request.requestCode}</p>
                    <p className="text-xs text-zinc-500">{TYPE_LABELS[request.type]} · {formatTime(request.createdAt)}</p>
                  </td>
                  <td className="px-4 py-3">
                    <p className="text-zinc-200">{request.customerName || "Khách hàng"} {request.customerPhone && <span className="text-zinc-500">· {request.customerPhone}</span>}</p>
                    <p className="text-xs text-zinc-500">#{request.orderCode} · {request.storeName}</p>
                  </td>
                  <td className="max-w-[320px] px-4 py-3 text-zinc-300">
                    {request.items.map((item) => (
                      <p key={item.id} className="text-xs">
                        {item.quantity} × {item.productName} ({variant(item.color, item.size)})
                        {item.exchangeVariantId && <> → <span className="text-amber-300">{variant(item.exchangeColor, item.exchangeSize)}</span></>}
                      </p>
                    ))}
                    <p className="mt-1 text-xs text-zinc-500">{request.reasonLabel}</p>
                  </td>
                  <td className="whitespace-nowrap px-4 py-3">
                    <StatusBadge request={request} />
                    {request.refundStatus === "PENDING" && <p className="mt-1 text-xs text-amber-300">Chờ hoàn tiền qua cổng</p>}
                  </td>
                  <td className="px-4 py-3">
                    <button type="button" className="inline-flex items-center gap-1 text-amber-300 hover:text-amber-100" onClick={() => open(request.id)}>
                      <Eye size={15} />Xử lý
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pagination currentPage={page} totalPages={data.totalPages} onPageChange={(next) => { setState("loading"); setPage(next); }} />
      </section>

      {openId && <ReturnDetailDialog id={openId} onClose={close} onChanged={(message) => { notification.success(message); reload(); }} />}
    </div>
  );
}

function ReturnDetailDialog({ id, onClose, onChanged }) {
  const notification = useSystemNotification();
  const [request, setRequest] = useState(null);
  const [error, setError] = useState("");
  const [conditions, setConditions] = useState({});
  const [mode, setMode] = useState(null);
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    returnAdminApi.detail(id, { signal: controller.signal })
      .then((result) => { setRequest(result); setConditions(defaultConditions(result.items)); })
      .catch((failure) => { if (!controller.signal.aborted) setError(failure.message); });
    return () => controller.abort();
  }, [id]);

  const act = async (action, message) => {
    setBusy(true);
    setError("");
    try {
      const updated = await action();
      setRequest(updated);
      setMode(null);
      setText("");
      onChanged(message);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  };

  const actions = availableActions(request);
  const approve = async () => {
    if (!await notification.confirm("Duyệt yêu cầu? Khách sẽ được báo mang hàng tới cửa hàng.", { confirmText: "Duyệt" })) return;
    act(() => returnAdminApi.approve(id), "Đã duyệt yêu cầu.");
  };
  const complete = () => act(() => returnAdminApi.complete(id, conditions),
    request.type === "RETURN" ? "Đã nhận hàng và hoàn tất trả hàng." : "Đã nhận hàng và đổi phân loại cho khách.");
  const reject = () => {
    if (!text.trim()) { setError("Vui lòng nhập lý do từ chối."); return; }
    act(() => returnAdminApi.reject(id, text.trim()), "Đã từ chối yêu cầu.");
  };
  const confirmRefund = () => act(() => returnAdminApi.confirmRefund(id, text.trim()), "Đã xác nhận hoàn tiền.");

  return (
    <AdminDialog open onClose={onClose} size="lg">
      <AdminDialogHeader title={request ? `${request.requestCode} · ${TYPE_LABELS[request.type]}` : "Yêu cầu đổi trả"}
        description={request ? `Đơn #${request.orderCode} · ${request.storeName} · ${request.customerName || "Khách hàng"} ${request.customerPhone || ""}` : ""}
        onClose={onClose} />
      <AdminDialogBody className="space-y-4 text-sm">
        {!request && !error && <p className="text-zinc-400">Đang tải…</p>}
        {request && <>
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge request={request} />
            <span className="text-zinc-400">Lý do: <span className="text-zinc-200">{request.reasonLabel}</span></span>
            <span className="text-zinc-500">Gửi lúc {formatTime(request.createdAt)}</span>
          </div>
          {request.note && <blockquote className="rounded-xl border border-zinc-800 bg-zinc-950 p-3 text-zinc-300">{request.note}</blockquote>}
          {request.rejectReason && <p className="text-rose-300">Lý do từ chối: {request.rejectReason}</p>}

          <div className="overflow-hidden rounded-xl border border-zinc-800">
            <table className="w-full text-sm">
              <thead className="bg-zinc-800/60 text-zinc-300">
                <tr><th className="px-3 py-2 text-left">Sản phẩm trả lại</th><th className="px-3 py-2 text-left">SL</th>
                  {request.type === "EXCHANGE" && <th className="px-3 py-2 text-left">Đổi sang</th>}
                  <th className="px-3 py-2 text-left">Tình trạng khi nhận</th></tr>
              </thead>
              <tbody>
                {request.items.map((item) => (
                  <tr key={item.id} className="border-t border-zinc-800">
                    <td className="px-3 py-2">{item.productName} <span className="text-zinc-500">({variant(item.color, item.size)})</span>
                      <p className="text-xs text-zinc-500">{formatMoney(item.unitPrice)} / món</p></td>
                    <td className="px-3 py-2">{item.quantity}</td>
                    {request.type === "EXCHANGE" && <td className="px-3 py-2 text-amber-300">{variant(item.exchangeColor, item.exchangeSize)}</td>}
                    <td className="px-3 py-2">
                      {request.status === "APPROVED" ? (
                        <select className="rounded-lg border border-zinc-700 bg-zinc-950 px-2 py-1.5" value={conditions[item.id]}
                          onChange={(e) => setConditions((c) => ({ ...c, [item.id]: e.target.value }))}>
                          <option value="GOOD">Còn tốt → tồn tại quầy</option>
                          <option value="DAMAGED">Lỗi / hỏng → tồn hỏng</option>
                        </select>
                      ) : (
                        <span className="text-zinc-400">{item.condition === "GOOD" ? "Còn tốt" : item.condition === "DAMAGED" ? "Lỗi / hỏng" : "—"}</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {request.type === "RETURN" && (
            <p className="text-zinc-300">
              {request.refundAmount != null ? "Đã hoàn" : "Dự kiến hoàn"}: <strong className="text-amber-300">{formatMoney(request.refundAmount ?? request.estimatedRefund)}</strong>
              {" · "}Thanh toán: {request.paymentMethod}
              {request.status === "APPROVED" && (["CASH", "COD"].includes(request.paymentMethod)
                ? " → hoàn tiền mặt tại quầy khi hoàn tất"
                : " → tạo yêu cầu hoàn qua cổng, xác nhận sau khi đã hoàn")}
              {request.refundStatus && <span className="text-zinc-500"> · Hoàn tiền: {request.refundStatus}</span>}
            </p>
          )}

          {(mode === "reject" || mode === "confirmRefund") && (
            <label className="block space-y-1.5 text-zinc-400">
              <span>{mode === "reject" ? "Lý do từ chối (khách sẽ thấy)" : "Mã giao dịch hoàn tiền (không bắt buộc)"}</span>
              <textarea rows={mode === "reject" ? 3 : 1} maxLength={mode === "reject" ? 500 : 100} value={text}
                onChange={(e) => setText(e.target.value)}
                className="w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-zinc-100 outline-none focus:border-amber-400" />
            </label>
          )}
        </>}
        {error && <p role="alert" className="text-rose-300">{error}</p>}
      </AdminDialogBody>
      <AdminDialogFooter>
        <Button type="button" variant="secondary" className="rounded-xl px-4 py-2.5 text-sm" onClick={onClose} disabled={busy}>Đóng</Button>
        {mode === "reject" && <Button type="button" variant="danger-solid" permission="RETURN_MANAGE" className="rounded-xl px-4 py-2.5 text-sm font-semibold" loading={busy} onClick={reject}>Xác nhận từ chối</Button>}
        {mode === "confirmRefund" && <Button type="button" variant="primary" permission="RETURN_MANAGE" className="rounded-xl px-4 py-2.5 text-sm font-semibold" loading={busy} onClick={confirmRefund}>Xác nhận đã hoàn</Button>}
        {!mode && actions.includes("reject") && <Button type="button" variant="danger" permission="RETURN_MANAGE" className="rounded-xl px-4 py-2.5 text-sm" onClick={() => setMode("reject")}>Từ chối</Button>}
        {!mode && actions.includes("approve") && <Button type="button" variant="primary" permission="RETURN_MANAGE" className="rounded-xl px-4 py-2.5 text-sm font-semibold" loading={busy} onClick={approve}>Duyệt</Button>}
        {!mode && actions.includes("complete") && <Button type="button" variant="primary" permission="RETURN_MANAGE" className="rounded-xl px-4 py-2.5 text-sm font-semibold" loading={busy} onClick={complete}>Đã nhận hàng · Hoàn tất</Button>}
        {!mode && actions.includes("confirmRefund") && <Button type="button" variant="primary" permission="RETURN_MANAGE" className="rounded-xl px-4 py-2.5 text-sm font-semibold" onClick={() => setMode("confirmRefund")}>Xác nhận đã hoàn tiền</Button>}
      </AdminDialogFooter>
    </AdminDialog>
  );
}
