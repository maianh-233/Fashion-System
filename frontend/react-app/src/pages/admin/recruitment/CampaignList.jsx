import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Plus, Search } from "lucide-react";
import Pagination from "../../../components/common/Pagination";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { Badge, ErrorText, btnPrimary, card, control, useLoader, useRecruitment } from "../../../components/admin/recruitment/RecruitmentUi";
import { useDebouncedValue } from "../../../hooks/useDebouncedValue";
import { CAMPAIGN_STATUS, formatDate, pageCount } from "./recruitmentLogic";
import { CampaignDialog } from "./CampaignDialogs";

/** Danh sách chiến dịch (chỉ những chiến dịch người dùng được xem theo cửa hàng / phân công). */
export default function CampaignList() {
  const navigate = useNavigate();
  const { can, scope, lookups } = useRecruitment();
  const [filter, setFilter] = useState({ keyword: "", status: "", storeId: "" });
  const [page, setPage] = useState(1);
  const [creating, setCreating] = useState(false);
  const keyword = useDebouncedValue(filter.keyword, 300);
  const params = { keyword, status: filter.status, page: page - 1, size: 15,
    ...(filter.storeId === "HQ" ? { headOffice: true } : { storeId: filter.storeId }) };
  const { data, error, loading } = useLoader((signal) => recruitmentApi.campaigns(params, { signal }), [JSON.stringify(params)]);
  const set = (patch) => { setFilter((f) => ({ ...f, ...patch })); setPage(1); };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative min-w-[220px] flex-1">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
          <input className={`${control} pl-9`} placeholder="Tìm theo tên hoặc mã chiến dịch" value={filter.keyword} onChange={(e) => set({ keyword: e.target.value })} />
        </div>
        <select className={`${control} w-44`} value={filter.status} onChange={(e) => set({ status: e.target.value })}>
          <option value="">Mọi trạng thái</option>
          {Object.entries(CAMPAIGN_STATUS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
        </select>
        {!scope.storeId && (
          <select className={`${control} w-56`} value={filter.storeId} onChange={(e) => set({ storeId: e.target.value })}>
            <option value="">Mọi cửa hàng</option>
            <option value="HQ">Văn phòng / toàn chuỗi</option>
            {lookups?.stores?.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        )}
        {can.manage && <button type="button" className={btnPrimary} onClick={() => setCreating(true)}><Plus size={15} />Tạo chiến dịch</button>}
      </div>
      <ErrorText>{error}</ErrorText>
      <section className={`${card} overflow-hidden`}>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[980px] text-sm">
            <thead className="bg-zinc-800/70 text-zinc-300">
              <tr>{["Chiến dịch", "Cửa hàng / phòng ban", "Người phụ trách", "Thời gian", "Vị trí", "Tiến độ", "Hồ sơ", "Trạng thái"].map((h) =>
                <th key={h} className="px-4 py-3 text-left font-semibold">{h}</th>)}</tr>
            </thead>
            <tbody>
              {loading && !data && <tr><td colSpan="8" className="px-4 py-12 text-center text-zinc-400">Đang tải…</td></tr>}
              {data?.content?.length === 0 && <tr><td colSpan="8" className="px-4 py-12 text-center text-zinc-400">Chưa có chiến dịch nào.</td></tr>}
              {data?.content?.map((c) => (
                <tr key={c.id} className="cursor-pointer border-t border-zinc-800 hover:bg-zinc-800/30" onClick={() => navigate(`/admin/recruitment/campaigns/${c.id}`)}>
                  <td className="px-4 py-3">
                    <Link to={`/admin/recruitment/campaigns/${c.id}`} className="font-semibold text-zinc-100 hover:text-amber-200" onClick={(e) => e.stopPropagation()}>{c.name}</Link>
                    <p className="text-xs text-zinc-500">{c.code}</p>
                  </td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{c.storeName || "Văn phòng"}<p className="text-zinc-500">{c.departmentName}</p></td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{c.ownerName}</td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{formatDate(c.startDate)}{c.endDate ? ` – ${formatDate(c.endDate)}` : ""}</td>
                  <td className="px-4 py-3 tabular-nums text-xs text-zinc-300">{c.openPositionCount}/{c.positionCount} đang mở</td>
                  <td className="px-4 py-3 tabular-nums text-xs text-zinc-300">{c.hired}/{c.headcount} đã tuyển</td>
                  <td className="px-4 py-3 tabular-nums text-xs text-zinc-300">{c.applications} <span className="text-zinc-500">({c.inProcess} đang xử lý)</span></td>
                  <td className="px-4 py-3"><Badge map={CAMPAIGN_STATUS} value={c.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pagination currentPage={page} totalPages={pageCount(data)} onPageChange={setPage} />
      </section>
      {creating && <CampaignDialog onClose={() => setCreating(false)}
        onSaved={(detail) => navigate(`/admin/recruitment/campaigns/${detail.campaign.id}`)} />}
    </div>
  );
}
