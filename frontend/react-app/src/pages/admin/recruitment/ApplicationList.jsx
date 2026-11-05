import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Plus, Search } from "lucide-react";
import Pagination from "../../../components/common/Pagination";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { useDebouncedValue } from "../../../hooks/useDebouncedValue";
import { Badge, ErrorText, btnPrimary, card, control, useLoader, useRecruitment } from "../../../components/admin/recruitment/RecruitmentUi";
import { APPLICATION_STATUS, STAGE_STATUS, STATUS_GROUPS, formatDate, pageCount, pageTotal } from "./recruitmentLogic";
import { AddApplicationDialog } from "./ApplicationDialogs";

/** Danh sách hồ sơ ứng tuyển với lọc nhanh theo nhóm trạng thái, vị trí, nguồn, việc được giao. */
export default function ApplicationList() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { can, lookups, sourceLabel } = useRecruitment();
  const [filter, setFilter] = useState({
    keyword: "", status: searchParams.get("status") || "", positionId: "", source: "",
    assignedToMe: searchParams.get("assignedToMe") === "true",
  });
  const [page, setPage] = useState(1);
  const [creating, setCreating] = useState(false);
  const keyword = useDebouncedValue(filter.keyword, 300);
  const params = { ...filter, keyword, page: page - 1, size: 20, assignedToMe: filter.assignedToMe || undefined };
  const { data, error, loading } = useLoader((signal) => recruitmentApi.applications(params, { signal }), [JSON.stringify(params)]);
  const positions = useLoader((signal) => recruitmentApi.positions({}, { signal }), []);
  const set = (patch) => { setFilter((f) => ({ ...f, ...patch })); setPage(1); };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-1.5">
        {STATUS_GROUPS.map((g) => (
          <button key={g.key} type="button" onClick={() => set({ status: g.key })}
            className={`rounded-full border px-3 py-1.5 text-xs ${filter.status === g.key ? "border-amber-400 bg-amber-400/15 text-amber-100" : "border-zinc-700 text-zinc-300 hover:text-white"}`}>
            {g.label}
          </button>
        ))}
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative min-w-[220px] flex-1">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
          <input className={`${control} pl-9`} placeholder="Tên, email, SĐT, mã hồ sơ…" value={filter.keyword} onChange={(e) => set({ keyword: e.target.value })} />
        </div>
        <select className={`${control} w-56`} value={filter.positionId} onChange={(e) => set({ positionId: e.target.value })}>
          <option value="">Mọi vị trí</option>
          {positions.data?.map((p) => <option key={p.id} value={p.id}>{p.title} · {p.campaignName}</option>)}
        </select>
        <select className={`${control} w-48`} value={filter.source} onChange={(e) => set({ source: e.target.value })}>
          <option value="">Mọi nguồn</option>
          {lookups?.sources?.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
        </select>
        <label className="flex items-center gap-2 text-sm text-zinc-300">
          <input type="checkbox" checked={filter.assignedToMe} onChange={(e) => set({ assignedToMe: e.target.checked })} />Được giao cho tôi
        </label>
        {can.manage && <button type="button" className={btnPrimary} onClick={() => setCreating(true)}><Plus size={15} />Thêm hồ sơ</button>}
      </div>
      <p className="text-sm text-zinc-400">{pageTotal(data)} hồ sơ</p>
      <ErrorText>{error}</ErrorText>
      <section className={`${card} overflow-hidden`}>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[1000px] text-sm">
            <thead className="bg-zinc-800/70 text-zinc-300">
              <tr>{["Ứng viên", "Vị trí", "Vòng hiện tại", "Điểm", "Nguồn", "Nộp", "Trạng thái"].map((h) => <th key={h} className="px-4 py-3 text-left font-semibold">{h}</th>)}</tr>
            </thead>
            <tbody>
              {loading && !data && <tr><td colSpan="7" className="px-4 py-12 text-center text-zinc-400">Đang tải…</td></tr>}
              {data?.content?.length === 0 && <tr><td colSpan="7" className="px-4 py-12 text-center text-zinc-400">Không có hồ sơ phù hợp.</td></tr>}
              {data?.content?.map((a) => (
                <tr key={a.id} className="cursor-pointer border-t border-zinc-800 hover:bg-zinc-800/30" onClick={() => navigate(`/admin/recruitment/applications/${a.id}`)}>
                  <td className="px-4 py-3">
                    <Link to={`/admin/recruitment/applications/${a.id}`} onClick={(e) => e.stopPropagation()} className="font-semibold text-zinc-100 hover:text-amber-200">{a.candidateName}</Link>
                    <p className="text-xs text-zinc-500">{a.code} · {a.candidatePhone || a.candidateEmail}</p>
                  </td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{a.positionTitle}<p className="text-zinc-500">{a.campaignName}</p></td>
                  <td className="px-4 py-3 text-xs text-zinc-300">
                    {a.currentStageName ? <>{a.stageIndex}/{a.stageCount} · {a.currentStageName}<p className="text-zinc-500">{STAGE_STATUS[a.currentStageStatus]?.label}</p></> : "—"}
                  </td>
                  <td className="px-4 py-3 tabular-nums text-xs text-amber-300">{a.lastScore != null ? Number(a.lastScore) : ""}</td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{sourceLabel(a.source)}</td>
                  <td className="px-4 py-3 text-xs text-zinc-400">{formatDate(a.appliedAt)}</td>
                  <td className="px-4 py-3"><Badge map={APPLICATION_STATUS} value={a.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pagination currentPage={page} totalPages={pageCount(data)} onPageChange={setPage} />
      </section>
      {creating && <AddApplicationDialog onClose={() => setCreating(false)}
        onSaved={(saved) => navigate(saved?.application ? `/admin/recruitment/applications/${saved.application.id}` : `/admin/recruitment/candidates/${saved.id}`)} />}
    </div>
  );
}
