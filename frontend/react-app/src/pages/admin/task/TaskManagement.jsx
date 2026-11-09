import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ChartColumn, ListChecks, Plus, Search } from "lucide-react";
import PageGuide from "../../../components/admin/common/PageGuide";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import Pagination from "../../../components/common/Pagination";
import {
  Badge, Empty, ErrorText, Field, Loading, Section, Stat, btnPrimary, control, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { taskApi } from "../../../api/admin/taskApi";
import TaskFormDialog from "./TaskFormDialog";
import { TASK_PRIORITY, TASK_STATUS, dateInput, formatDateTime } from "./taskLogic";

const STAT_PAGE_SIZE = 20;

/** Toàn bộ công việc trong phạm vi và thống kê theo nhân viên. */
export default function TaskManagement() {
  const [params, setParams] = useSearchParams();
  const tab = params.get("tab") === "stats" ? "stats" : "list";
  const lookups = useLoader((signal) => taskApi.lookups({ signal }), []);
  const data = lookups.data;

  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
      <AdminCatalogPageHeader icon={ListChecks} eyebrow="Công việc" title="Quản lý công việc"
        description="Giao việc cho nhân viên, theo dõi ai đã xong, ai còn trễ và xem thống kê theo từng người."
        status={data ? (data.global ? "Toàn hệ thống" : "Cửa hàng của bạn") : ""} />
      <PageGuide id="tasks" />
      <div className="flex flex-wrap gap-2" role="tablist">
        {[["list", "Danh sách", ListChecks], ["stats", "Thống kê", ChartColumn]].map(([key, label, Icon]) => (
          <button key={key} type="button" role="tab" aria-selected={tab === key}
            onClick={() => setParams(key === "stats" ? { tab: "stats" } : {}, { replace: true })}
            className={`inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-sm ${tab === key ? "border-amber-400 bg-amber-500/10 text-amber-200" : "border-zinc-700 text-zinc-400 hover:text-zinc-200"}`}>
            <Icon size={16} />{label}
          </button>
        ))}
      </div>
      <ErrorText>{lookups.error}</ErrorText>
      {!data ? (lookups.loading && <Loading />) : tab === "list" ? <TaskTable lookups={data} /> : <TaskStats lookups={data} />}
    </div>
  );
}

function PersonSelect({ label, people, value, onChange }) {
  return (
    <Field label={label} className="min-w-[11rem] flex-1 sm:flex-none">
      <select className={control} value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">Tất cả</option>
        {people.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}
      </select>
    </Field>
  );
}

function TaskTable({ lookups }) {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ storeId: "", status: "", priority: "", assigneeId: "", assignerId: "" });
  const [draft, setDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [creating, setCreating] = useState(false);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => { setKeyword(draft.trim()); setPage(1); }, 300);
    return () => clearTimeout(timer);
  }, [draft]);

  const setFilter = (key) => (value) => { setFilters((f) => ({ ...f, [key]: value })); setPage(1); };
  const overdue = filters.status === "OVERDUE";
  const { data, error, loading } = useLoader((signal) => taskApi.list({
    ...filters, status: overdue ? "" : filters.status, overdue: overdue || undefined, keyword, page: page - 1, size: 20,
  }, { signal }), [filters, keyword, page, version]);
  const rows = data?.content || [];

  return (
    <div className="space-y-4">
      <Section>
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Tìm kiếm" className="min-w-[14rem] flex-1">
            <div className="relative">
              <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
              <input className={`${control} pl-9`} value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="Tên việc, mã, người nhận…" />
            </div>
          </Field>
          {lookups.global && (
            <Field label="Cửa hàng" className="min-w-[11rem] flex-1 sm:flex-none">
              <select className={control} value={filters.storeId} onChange={(e) => setFilter("storeId")(e.target.value)}>
                <option value="">Tất cả</option>
                {lookups.stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </Field>
          )}
          <Field label="Trạng thái" className="min-w-[9rem] flex-1 sm:flex-none">
            <select className={control} value={filters.status} onChange={(e) => setFilter("status")(e.target.value)}>
              <option value="">Tất cả</option>
              {Object.entries(TASK_STATUS).map(([value, s]) => <option key={value} value={value}>{s.label}</option>)}
              <option value="OVERDUE">Quá hạn</option>
            </select>
          </Field>
          <Field label="Ưu tiên" className="min-w-[9rem] flex-1 sm:flex-none">
            <select className={control} value={filters.priority} onChange={(e) => setFilter("priority")(e.target.value)}>
              <option value="">Tất cả</option>
              {Object.entries(TASK_PRIORITY).map(([value, p]) => <option key={value} value={value}>{p.label}</option>)}
            </select>
          </Field>
          <PersonSelect label="Người nhận" people={lookups.people} value={filters.assigneeId} onChange={setFilter("assigneeId")} />
          <PersonSelect label="Người giao" people={lookups.people} value={filters.assignerId} onChange={setFilter("assignerId")} />
          {lookups.canAssign && <button type="button" className={`${btnPrimary} ml-auto`} onClick={() => setCreating(true)}><Plus size={16} />Giao việc</button>}
        </div>
      </Section>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : !rows.length ? <Empty>Không có công việc phù hợp.</Empty> : (
        <Section className="overflow-x-auto p-0">
          <table className="w-full min-w-[920px] text-sm">
            <thead className="text-left text-xs text-zinc-400">
              <tr className="border-b border-zinc-800">
                <th className="px-4 py-3 font-medium">Công việc</th>
                <th className="px-4 py-3 font-medium">Người nhận</th>
                <th className="px-4 py-3 font-medium">Người giao</th>
                <th className="px-4 py-3 font-medium">Hạn chót</th>
                <th className="px-4 py-3 font-medium">Ưu tiên</th>
                <th className="px-4 py-3 font-medium">Tiến độ</th>
                <th className="px-4 py-3 font-medium">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((t) => (
                <tr key={t.id} className="cursor-pointer border-b border-zinc-800/70 hover:bg-zinc-800/40" onClick={() => navigate(`/admin/me/tasks/${t.id}`)}>
                  <td className="px-4 py-3">
                    <Link to={`/admin/me/tasks/${t.id}`} className="font-semibold text-zinc-100 hover:text-amber-300" onClick={(e) => e.stopPropagation()}>{t.title}</Link>
                    <p className="text-xs text-zinc-500">{t.code}</p>
                  </td>
                  <td className="px-4 py-3 text-zinc-300">{t.assigneeName}{t.storeName && <p className="text-xs text-zinc-500">{t.storeName}</p>}</td>
                  <td className="px-4 py-3 text-zinc-300">{t.assignerName || "Không rõ"}</td>
                  <td className="px-4 py-3 text-xs">
                    {t.dueAt ? <span className={t.overdue ? "font-semibold text-rose-300" : "text-zinc-300"}>{formatDateTime(t.dueAt)}{t.overdue ? " · Quá hạn" : ""}</span> : <span className="text-zinc-500">Không có hạn</span>}
                  </td>
                  <td className="px-4 py-3"><Badge map={TASK_PRIORITY} value={t.priority} /></td>
                  <td className="px-4 py-3 text-xs tabular-nums text-zinc-400">{t.checklistTotal ? `${t.checklistDone}/${t.checklistTotal} việc con` : "—"}</td>
                  <td className="px-4 py-3"><Badge map={TASK_STATUS} value={t.status} />{t.late && <p className="mt-1 text-xs text-rose-300">Trễ hạn</p>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Section>
      )}
      <Pagination currentPage={page} totalPages={data?.page?.totalPages || data?.totalPages || 1} onPageChange={setPage} />
      {creating && <TaskFormDialog assignees={lookups.assignees} onClose={() => setCreating(false)} onSaved={() => { setCreating(false); setPage(1); setVersion((v) => v + 1); }} />}
    </div>
  );
}

function TaskStats({ lookups }) {
  const [storeId, setStoreId] = useState("");
  const [from, setFrom] = useState(() => { const d = new Date(); d.setDate(d.getDate() - 29); return dateInput(d); });
  const [to, setTo] = useState(() => dateInput());
  const [page, setPage] = useState(1);
  const { data, error, loading } = useLoader((signal) => taskApi.stats({ storeId, from, to }, { signal }), [storeId, from, to]);
  const rows = data?.rows || [];
  const totals = data?.totals;
  const shown = rows.slice((page - 1) * STAT_PAGE_SIZE, page * STAT_PAGE_SIZE);
  const rate = (value) => (value == null ? "—" : `${value}%`);

  return (
    <div className="space-y-4">
      <Section>
        <div className="flex flex-wrap items-end gap-3">
          {lookups.global && (
            <Field label="Cửa hàng" className="min-w-[14rem] flex-1 sm:flex-none">
              <select className={control} value={storeId} onChange={(e) => { setStoreId(e.target.value); setPage(1); }}>
                <option value="">Tất cả cửa hàng</option>
                {lookups.stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </Field>
          )}
          <Field label="Giao từ ngày" className="flex-1 sm:flex-none"><input type="date" className={control} value={from} max={to} onChange={(e) => { setFrom(e.target.value); setPage(1); }} /></Field>
          <Field label="Đến ngày" className="flex-1 sm:flex-none"><input type="date" className={control} value={to} min={from} onChange={(e) => { setTo(e.target.value); setPage(1); }} /></Field>
        </div>
      </Section>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : totals && (
        <>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-5">
            <Stat label="Tổng việc đã giao" value={totals.total} hint="Không gồm việc đã hủy" />
            <Stat label="Đang mở" value={totals.open} tone="sky" />
            <Stat label="Hoàn thành" value={totals.done} tone="emerald" />
            <Stat label="Quá hạn" value={totals.overdue} tone="rose" />
            <Stat label="Tỷ lệ đúng hạn" value={rate(totals.onTimeRate)} tone="amber" hint="Trên các việc có hạn đã xong" />
          </div>
          {!rows.length ? <Empty>Chưa có công việc nào trong khoảng này.</Empty> : (
            <Section className="overflow-x-auto p-0">
              <table className="w-full min-w-[720px] text-sm">
                <thead className="text-left text-xs text-zinc-400">
                  <tr className="border-b border-zinc-800">
                    <th className="px-4 py-3 font-medium">Nhân viên</th>
                    <th className="px-4 py-3 text-right font-medium">Tổng</th>
                    <th className="px-4 py-3 text-right font-medium">Đang mở</th>
                    <th className="px-4 py-3 text-right font-medium">Hoàn thành</th>
                    <th className="px-4 py-3 text-right font-medium">Quá hạn</th>
                    <th className="px-4 py-3 text-right font-medium">Trễ hạn</th>
                    <th className="px-4 py-3 text-right font-medium">Đúng hạn</th>
                  </tr>
                </thead>
                <tbody>
                  {shown.map((r) => (
                    <tr key={r.assigneeId} className="border-b border-zinc-800/70">
                      <td className="px-4 py-3 text-zinc-100">{r.assigneeName}{r.storeName && <p className="text-xs text-zinc-500">{r.storeName}</p>}</td>
                      <td className="px-4 py-3 text-right tabular-nums">{r.total}</td>
                      <td className="px-4 py-3 text-right tabular-nums text-sky-300">{r.open}</td>
                      <td className="px-4 py-3 text-right tabular-nums text-emerald-300">{r.done}</td>
                      <td className={`px-4 py-3 text-right tabular-nums ${r.overdue ? "font-semibold text-rose-300" : "text-zinc-400"}`}>{r.overdue}</td>
                      <td className="px-4 py-3 text-right tabular-nums text-zinc-400">{r.late}</td>
                      <td className="px-4 py-3 text-right tabular-nums">{rate(r.onTimeRate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </Section>
          )}
          <Pagination currentPage={page} totalPages={Math.max(1, Math.ceil(rows.length / STAT_PAGE_SIZE))} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
