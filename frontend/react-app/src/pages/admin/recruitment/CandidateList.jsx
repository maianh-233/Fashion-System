import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FileText, Plus, Search } from "lucide-react";
import Pagination from "../../../components/common/Pagination";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { useDebouncedValue } from "../../../hooks/useDebouncedValue";
import { ErrorText, btnPrimary, card, control, useLoader, useRecruitment } from "../../../components/admin/recruitment/RecruitmentUi";
import { APPLICATION_STATUS, formatDate, pageCount, pageTotal } from "./recruitmentLogic";
import { CandidateDialog } from "./ApplicationDialogs";

/** Kho ứng viên: tìm kiếm, lọc theo nguồn / kỹ năng / trạng thái / vị trí, cảnh báo trùng. */
export default function CandidateList() {
  const navigate = useNavigate();
  const { can, lookups, scope, sourceLabel } = useRecruitment();
  const [filter, setFilter] = useState({ keyword: "", source: "", skill: "", tag: "", status: "", positionId: "", storeId: "", hasCv: "" });
  const [page, setPage] = useState(1);
  const [creating, setCreating] = useState(false);
  const keyword = useDebouncedValue(filter.keyword, 300);
  const skill = useDebouncedValue(filter.skill, 300);
  const params = { ...filter, keyword, skill, page: page - 1, size: 20 };
  const { data, error, loading } = useLoader((signal) => recruitmentApi.candidates(params, { signal }), [JSON.stringify(params)]);
  const positions = useLoader((signal) => recruitmentApi.positions({}, { signal }), []);
  const set = (patch) => { setFilter((f) => ({ ...f, ...patch })); setPage(1); };

  return (
    <div className="space-y-4">
      <div className="grid gap-3 md:grid-cols-4 xl:grid-cols-8">
        <div className="relative md:col-span-2">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
          <input className={`${control} pl-9`} placeholder="Tên, email, SĐT, mã, kỹ năng…" value={filter.keyword} onChange={(e) => set({ keyword: e.target.value })} />
        </div>
        <select className={control} value={filter.source} onChange={(e) => set({ source: e.target.value })}>
          <option value="">Mọi nguồn</option>
          {lookups?.sources?.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
        </select>
        <input className={control} placeholder="Kỹ năng" value={filter.skill} onChange={(e) => set({ skill: e.target.value })} />
        <select className={control} value={filter.status} onChange={(e) => set({ status: e.target.value })}>
          <option value="">Mọi trạng thái hồ sơ</option>
          {Object.entries(APPLICATION_STATUS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
        </select>
        <select className={control} value={filter.positionId} onChange={(e) => set({ positionId: e.target.value })}>
          <option value="">Mọi vị trí</option>
          {positions.data?.map((p) => <option key={p.id} value={p.id}>{p.title}</option>)}
        </select>
        <select className={control} value={filter.hasCv} onChange={(e) => set({ hasCv: e.target.value })}>
          <option value="">CV: tất cả</option><option value="true">Có CV</option><option value="false">Chưa có CV</option>
        </select>
        {!scope.storeId ? (
          <select className={control} value={filter.storeId} onChange={(e) => set({ storeId: e.target.value })}>
            <option value="">Mọi cửa hàng</option>
            {lookups?.stores?.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        ) : <span />}
      </div>
      <div className="flex items-center justify-between">
        <p className="text-sm text-zinc-400">{pageTotal(data)} ứng viên</p>
        {can.manage && <button type="button" className={btnPrimary} onClick={() => setCreating(true)}><Plus size={15} />Thêm ứng viên</button>}
      </div>
      <ErrorText>{error}</ErrorText>
      <section className={`${card} overflow-hidden`}>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[1000px] text-sm">
            <thead className="bg-zinc-800/70 text-zinc-300">
              <tr>{["Ứng viên", "Liên hệ", "Kinh nghiệm / kỹ năng", "Nguồn", "Hồ sơ gần nhất", "Ngày tạo"].map((h) => <th key={h} className="px-4 py-3 text-left font-semibold">{h}</th>)}</tr>
            </thead>
            <tbody>
              {loading && !data && <tr><td colSpan="6" className="px-4 py-12 text-center text-zinc-400">Đang tải…</td></tr>}
              {data?.content?.length === 0 && <tr><td colSpan="6" className="px-4 py-12 text-center text-zinc-400">Không có ứng viên phù hợp.</td></tr>}
              {data?.content?.map((c) => (
                <tr key={c.id} className="cursor-pointer border-t border-zinc-800 align-top hover:bg-zinc-800/30" onClick={() => navigate(`/admin/recruitment/candidates/${c.id}`)}>
                  <td className="px-4 py-3">
                    <Link to={`/admin/recruitment/candidates/${c.id}`} onClick={(e) => e.stopPropagation()} className="font-semibold text-zinc-100 hover:text-amber-200">{c.fullName}</Link>
                    <p className="flex items-center gap-1.5 text-xs text-zinc-500">
                      {c.code}{c.hasCv && <FileText size={12} className="text-amber-300" aria-label="Có CV" />}
                      {c.duplicateCount > 0 && <span className="rounded-full bg-amber-500/15 px-1.5 text-amber-300">trùng {c.duplicateCount}</span>}
                    </p>
                    {c.storeName && <p className="text-xs text-zinc-500">{c.storeName}</p>}
                  </td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{c.email}<p>{c.phone}</p></td>
                  <td className="px-4 py-3 text-xs text-zinc-300">
                    {c.currentTitle}{c.yearsExperience != null && <span className="text-zinc-500"> · {Number(c.yearsExperience)} năm</span>}
                    <div className="mt-1 flex flex-wrap gap-1">{c.skills.slice(0, 4).map((s) => <span key={s} className="rounded-full bg-zinc-800 px-2 py-0.5">{s}</span>)}</div>
                  </td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{sourceLabel(c.source)}</td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{c.latestStatus || "—"}<p className="text-zinc-500">{c.latestPosition}{c.applicationCount > 1 ? ` (+${c.applicationCount - 1})` : ""}</p></td>
                  <td className="px-4 py-3 text-xs text-zinc-400">{formatDate(c.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pagination currentPage={page} totalPages={pageCount(data)} onPageChange={setPage} />
      </section>
      {creating && <CandidateDialog onClose={() => setCreating(false)} onSaved={(c) => navigate(`/admin/recruitment/candidates/${c.id}`)} />}
    </div>
  );
}
