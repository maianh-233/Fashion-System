import { useEffect, useState } from "react";
import { Activity, Database, Eye, RotateCcw, Search, ShieldCheck } from "lucide-react";
import Pagination from "../../components/common/Pagination";
import Button from "../../components/common/Button";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDetailDialog from "../../components/admin/common/AdminDetailDialog";
import {
  buildSystemLogQuery,
  fetchBusinessAuditLogs,
  fetchAuthAuditLogs,
  normalizeAdminLogsPage,
  presentSystemLog,
} from "../../api/adminLogsApi";

const EMPTY_FILTERS = { action: "", actorUserId: "", username: "", table: "", rowId: "", requestId: "", fromAt: "", toAt: "" };
const control = "h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none transition focus:border-cyan-400";
const severityStyle = {
  INFO: "border-cyan-400/30 bg-cyan-500/10 text-cyan-200",
  WARN: "border-amber-400/30 bg-amber-500/10 text-amber-200",
};
const actionSuggestions = {
  business: ["CREATE", "UPDATE", "DELETE", "IMPORT", "GOODS_RECEIPT", "GOODS_ISSUE"],
  auth: ["LOGIN_SUCCESS", "LOGIN_FAILED", "LOGOUT"],
};

const formatTime = value => value ? new Date(value).toLocaleString("vi-VN") : "—";
const shortId = value => value ? `${value.slice(0, 8)}…${value.slice(-4)}` : "—";
const json = value => value == null ? "—" : JSON.stringify(value, null, 2);

function FilterField({ label, children }) {
  return <label className="min-w-0 space-y-1 text-xs font-medium text-zinc-400"><span>{label}</span>{children}</label>;
}

function LogDetail({ log, view, onClose }) {
  if (!log) return null;
  const item = presentSystemLog(log, view);
  const isBusiness = view === "business";
  return <AdminDetailDialog open size="xl" title={log.action || "Chi tiết nhật ký"} description={log.eventId || log.id} onClose={onClose} showFooter>
    <div className="space-y-5 text-sm text-zinc-200">
      <div className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-950 p-4 sm:grid-cols-2">
        <p><span className="text-zinc-500">Thời gian</span><strong className="mt-1 block font-medium">{formatTime(item.timestamp)}</strong></p>
        <p><span className="text-zinc-500">Người thực hiện</span><strong className="mt-1 block break-all font-medium">{item.actor}</strong></p>
        <p><span className="text-zinc-500">Nguồn</span><strong className="mt-1 block break-all font-medium">{item.source}</strong></p>
        <p><span className="text-zinc-500">IP</span><strong className="mt-1 block font-medium">{log.ipAddress || "—"}</strong></p>
        {isBusiness && <><p><span className="text-zinc-500">Request ID</span><strong className="mt-1 block break-all font-medium">{log.requestId || "—"}</strong></p><p><span className="text-zinc-500">Số dòng</span><strong className="mt-1 block font-medium">{log.rowCount ?? log.changes?.length ?? 0}</strong></p></>}
        <p className="sm:col-span-2"><span className="text-zinc-500">User agent</span><strong className="mt-1 block break-all font-medium">{log.userAgent || "—"}</strong></p>
      </div>
      {!isBusiness && <section className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4"><h3 className="mb-2 font-semibold">Nội dung</h3><p className="text-zinc-300">{log.description || "Không có mô tả."}</p></section>}
      {isBusiness && <section className="space-y-3">
        <div><h3 className="font-semibold">Thay đổi dữ liệu</h3><p className="text-xs text-zinc-500">{log.migratedFromAuthAudit ? "Bản ghi được chuyển từ lịch sử xác thực cũ; không có snapshot từng dòng." : `${log.changes?.length || 0} thay đổi chi tiết.`}</p></div>
        {(log.changes || []).length === 0 ? <p className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4 text-zinc-400">Không có dữ liệu thay đổi từng dòng.</p> : (log.changes || []).map((change, index) => <article key={`${change.table}:${change.rowId}:${index}`} className="space-y-3 rounded-2xl border border-zinc-800 bg-zinc-950 p-4">
          <div className="flex flex-wrap items-center gap-2"><span className="rounded-full border border-cyan-400/30 bg-cyan-500/10 px-2 py-1 text-xs text-cyan-200">{change.operation || "CHANGE"}</span><strong>{change.table || "—"}</strong><span className="break-all text-zinc-500">{change.rowId || "—"}</span></div>
          {change.changedFields?.length > 0 && <div className="flex flex-wrap gap-2">{change.changedFields.map(field => <span key={field} className="rounded-lg bg-zinc-800 px-2 py-1 text-xs text-zinc-300">{field}</span>)}</div>}
          <div className="grid gap-3 lg:grid-cols-2"><div><p className="mb-1 text-xs text-zinc-500">Trước</p><pre className="max-h-72 overflow-auto whitespace-pre-wrap break-all rounded-xl bg-black/30 p-3 text-xs">{json(change.oldValues)}</pre></div><div><p className="mb-1 text-xs text-zinc-500">Sau</p><pre className="max-h-72 overflow-auto whitespace-pre-wrap break-all rounded-xl bg-black/30 p-3 text-xs">{json(change.newValues)}</pre></div></div>
        </article>)}
      </section>}
    </div>
  </AdminDetailDialog>;
}

export default function LoManagement() {
  const [view, setView] = useState("business");
  const [page, setPage] = useState(1);
  const [draft, setDraft] = useState(EMPTY_FILTERS);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [data, setData] = useState({ rows: [], totalPages: 1, totalElements: 0 });
  const [state, setState] = useState("loading");
  const [error, setError] = useState("");
  const [detail, setDetail] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    const request = view === "business" ? fetchBusinessAuditLogs : fetchAuthAuditLogs;
    request(buildSystemLogQuery(view, filters, page), { signal: controller.signal })
      .then(response => { if (!controller.signal.aborted) { setData(normalizeAdminLogsPage(response)); setState("ready"); } })
      .catch(requestError => { if (!controller.signal.aborted) { setError(requestError.message || "Không thể tải nhật ký hệ thống."); setState("error"); } });
    return () => controller.abort();
  }, [view, page, filters]);

  const startLoading = () => { setState("loading"); setError(""); };
  const updateFilter = key => event => setDraft(current => ({ ...current, [key]: event.target.value }));
  const applyFilters = event => {
    event.preventDefault();
    if (draft.fromAt && draft.toAt && draft.fromAt >= draft.toAt) {
      setError("Thời điểm kết thúc phải sau thời điểm bắt đầu.");
      setState("error");
      return;
    }
    startLoading();
    setPage(1);
    setFilters({ ...draft });
  };
  const resetFilters = () => { startLoading(); setDraft(EMPTY_FILTERS); setFilters({ ...EMPTY_FILTERS }); setPage(1); };
  const changeView = nextView => {
    startLoading();
    setView(nextView);
    setPage(1);
    setDraft(EMPTY_FILTERS);
    setFilters({ ...EMPTY_FILTERS });
    setData({ rows: [], totalPages: 1, totalElements: 0 });
    setDetail(null);
  };
  const changePage = nextPage => { startLoading(); setPage(nextPage); };

  return <div className="admin-catalog-page admin-catalog-page--logs space-y-5 text-zinc-100">
    <AdminCatalogPageHeader icon={Activity} eyebrow="Giám sát hệ thống" title="Nhật ký hệ thống" description="Tra cứu thao tác dữ liệu và lịch sử xác thực được ghi nhận bởi backend." status={`${data.totalElements.toLocaleString("vi-VN")} bản ghi`} />
    <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900">
      <div className="border-b border-zinc-800 bg-zinc-950 p-4 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex gap-2" role="group" aria-label="Loại nhật ký">
            {[["business", "Thao tác dữ liệu", Database], ["auth", "Đăng nhập / đăng xuất", ShieldCheck]].map(([id, label, Icon]) => <button key={id} type="button" aria-pressed={view === id} onClick={() => changeView(id)} className={`inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-sm ${view === id ? "border-cyan-400 bg-cyan-500/10 text-cyan-200" : "border-zinc-700 text-zinc-400 hover:text-zinc-200"}`}><Icon size={16} />{label}</button>)}
          </div>
          <p className="text-sm text-zinc-500">Tối đa 25 bản ghi mỗi trang</p>
        </div>
        <form className="mt-5 space-y-3" onSubmit={applyFilters}>
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <FilterField label="Hành động"><input list={`log-actions-${view}`} className={control} value={draft.action} onChange={updateFilter("action")} placeholder={view === "business" ? "UPDATE, DELETE…" : "LOGIN_FAILED…"} /><datalist id={`log-actions-${view}`}>{actionSuggestions[view].map(action => <option key={action} value={action} />)}</datalist></FilterField>
            <FilterField label="User ID"><input className={control} value={draft.actorUserId} onChange={updateFilter("actorUserId")} placeholder="UUID người thực hiện" /></FilterField>
            {view === "business" && <><FilterField label="Tên đăng nhập"><input className={control} value={draft.username} onChange={updateFilter("username")} placeholder="Tìm gần đúng username" /></FilterField><FilterField label="Request ID"><input className={control} value={draft.requestId} onChange={updateFilter("requestId")} placeholder="Mã request chính xác" /></FilterField><FilterField label="Bảng dữ liệu"><input className={control} value={draft.table} onChange={updateFilter("table")} placeholder="users, orders…" /></FilterField><FilterField label="Row ID"><input className={control} value={draft.rowId} onChange={updateFilter("rowId")} placeholder="Khóa dòng dữ liệu" /></FilterField></>}
            <FilterField label="Từ thời điểm"><input type="datetime-local" className={control} value={draft.fromAt} onChange={updateFilter("fromAt")} /></FilterField>
            <FilterField label="Trước thời điểm"><input type="datetime-local" className={control} value={draft.toAt} onChange={updateFilter("toAt")} /></FilterField>
          </div>
          <div className="flex flex-wrap gap-2"><Button type="submit" className="inline-flex items-center gap-2 bg-cyan-700"><Search size={16} />Lọc nhật ký</Button><Button type="button" className="inline-flex items-center gap-2 bg-zinc-800" onClick={resetFilters}><RotateCcw size={16} />Đặt lại</Button></div>
        </form>
      </div>
      {error && <p role="alert" className="m-4 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-sm text-rose-200">{error}</p>}
      <div className="overflow-x-auto">
        <table className="w-full min-w-[1050px] text-sm">
          <thead className="bg-zinc-800/70 text-zinc-300"><tr>{["Thời gian", "Hành động", "Người thực hiện", view === "business" ? "Request / nguồn" : "IP", "Nội dung", "Chi tiết"].map(head => <th key={head} className="px-4 py-3 text-left font-semibold">{head}</th>)}</tr></thead>
          <tbody>
            {state === "loading" && <tr><td colSpan="6" className="px-4 py-12 text-center text-zinc-400">Đang tải nhật ký…</td></tr>}
            {state === "error" && data.rows.length === 0 && <tr><td colSpan="6" className="px-4 py-12 text-center text-zinc-500">Không có dữ liệu để hiển thị.</td></tr>}
            {state === "ready" && data.rows.length === 0 && <tr><td colSpan="6" className="px-4 py-12 text-center text-zinc-400">Không tìm thấy nhật ký phù hợp.</td></tr>}
            {state === "ready" && data.rows.map(log => {
              const item = presentSystemLog(log, view);
              return <tr key={log.id} className="border-t border-zinc-800 align-top transition-colors hover:bg-zinc-800/30">
                <td className="whitespace-nowrap px-4 py-3 text-zinc-300">{formatTime(item.timestamp)}</td>
                <td className="px-4 py-3"><span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${severityStyle[item.severity]}`}>{log.action || "UNKNOWN"}</span></td>
                <td className="max-w-[200px] break-all px-4 py-3 text-zinc-200">{item.actor}</td>
                <td className="max-w-[280px] break-all px-4 py-3 text-zinc-400">{item.source}</td>
                <td className="max-w-[320px] px-4 py-3 text-zinc-300"><p className="line-clamp-2">{item.summary}</p>{view === "business" && <p className="mt-1 text-xs text-zinc-500">{log.rowCount ?? log.changes?.length ?? 0} dòng · {shortId(log.requestId || log.eventId)}</p>}</td>
                <td className="px-4 py-3"><button type="button" className="inline-flex items-center gap-1 text-cyan-300 hover:text-cyan-100" onClick={() => setDetail(log)}><Eye size={15} />Xem</button></td>
              </tr>;
            })}
          </tbody>
        </table>
      </div>
      <Pagination currentPage={page} totalPages={data.totalPages} onPageChange={changePage} />
    </section>
    <LogDetail log={detail} view={view} onClose={() => setDetail(null)} />
  </div>;
}
