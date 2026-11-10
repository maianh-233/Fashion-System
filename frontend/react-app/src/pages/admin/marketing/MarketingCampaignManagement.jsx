import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { CalendarDays, ChartColumn, ListChecks, Megaphone, Plus, Search } from "lucide-react";
import CampaignCompare from "./CampaignCompare";
import MarketingCalendar from "./MarketingCalendar";
import PageGuide from "../../../components/admin/common/PageGuide";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import Pagination from "../../../components/common/Pagination";
import {
  Badge, Empty, ErrorText, Field, Loading, Section, btnPrimary, control, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { marketingApi } from "../../../api/admin/marketingApi";
import CampaignFormDialog from "./CampaignFormDialog";
import { CAMPAIGN_PHASE, CAMPAIGN_STATUS, OBJECTIVES, budgetUsage, formatMoney, formatWindow } from "./marketingLogic";

/** Chiến dịch marketing: danh sách, lịch marketing và so sánh (so sánh chỉ cho nhân viên cấp chuỗi). */
export default function MarketingCampaignManagement() {
  const [params, setParams] = useSearchParams();
  const lookups = useLoader((signal) => marketingApi.lookups({ signal }), []);
  const can = lookups.data;
  const tabs = [["list", "Danh sách", ListChecks], ["calendar", "Lịch marketing", CalendarDays],
    ...(can?.global ? [["compare", "So sánh", ChartColumn]] : [])];
  const tab = tabs.some(([key]) => key === params.get("tab")) ? params.get("tab") : "list";

  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
      <AdminCatalogPageHeader icon={Megaphone} eyebrow="Marketing" title="Chiến dịch marketing"
        description="Lập kế hoạch chiến dịch, gắn các khuyến mãi / voucher / flash sale có sẵn, gửi duyệt và theo dõi ngân sách."
        status={can ? (can.global ? "Toàn chuỗi" : "Chỉ xem chiến dịch đã duyệt") : ""} />
      <PageGuide id="marketingCampaigns" />
      <ErrorText>{lookups.error}</ErrorText>
      <div className="flex flex-wrap gap-2" role="tablist">
        {tabs.map(([key, label, Icon]) => (
          <button key={key} type="button" role="tab" aria-selected={tab === key}
            onClick={() => setParams(key === "list" ? {} : { tab: key }, { replace: true })}
            className={`inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-sm ${tab === key ? "border-amber-400 bg-amber-500/10 text-amber-200" : "border-zinc-700 text-zinc-400 hover:text-zinc-200"}`}>
            <Icon size={16} />{label}
          </button>
        ))}
      </div>
      {tab === "calendar" ? <MarketingCalendar /> : tab === "compare" ? <CampaignCompare /> : <CampaignList can={can} />}
    </div>
  );
}

function CampaignList({ can }) {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ status: "", phase: "", objective: "" });
  const [draft, setDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => { setKeyword(draft.trim()); setPage(1); }, 300);
    return () => clearTimeout(timer);
  }, [draft]);

  const setFilter = (key) => (event) => { setFilters((f) => ({ ...f, [key]: event.target.value })); setPage(1); };
  const { data, error, loading } = useLoader((signal) => marketingApi.list({ ...filters, q: keyword, page: page - 1, size: 20 }, { signal }),
    [filters, keyword, page]);
  const rows = data?.content || [];
  const global = Boolean(can?.global);

  return (
    <div className="space-y-5 [&>*]:min-w-0">
      <Section>
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Tìm kiếm" className="min-w-[14rem] flex-1">
            <div className="relative">
              <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
              <input className={`${control} pl-9`} value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="Tên hoặc mã chiến dịch…" />
            </div>
          </Field>
          {global && (
            <Field label="Trạng thái" className="min-w-[9rem] flex-1 sm:flex-none">
              <select className={control} value={filters.status} onChange={setFilter("status")}>
                <option value="">Tất cả</option>
                {Object.entries(CAMPAIGN_STATUS).map(([value, s]) => <option key={value} value={value}>{s.label}</option>)}
              </select>
            </Field>
          )}
          <Field label="Giai đoạn" className="min-w-[9rem] flex-1 sm:flex-none">
            <select className={control} value={filters.phase} onChange={setFilter("phase")}>
              <option value="">Tất cả</option>
              {Object.entries(CAMPAIGN_PHASE).map(([value, p]) => <option key={value} value={value}>{p.label}</option>)}
            </select>
          </Field>
          <Field label="Mục tiêu" className="min-w-[10rem] flex-1 sm:flex-none">
            <select className={control} value={filters.objective} onChange={setFilter("objective")}>
              <option value="">Tất cả</option>
              {Object.entries(OBJECTIVES).map(([value, o]) => <option key={value} value={value}>{o.label}</option>)}
            </select>
          </Field>
          {can?.canManage && <button type="button" className={`${btnPrimary} ml-auto`} onClick={() => setCreating(true)}><Plus size={16} />Tạo chiến dịch</button>}
        </div>
      </Section>

      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : !rows.length ? <Empty>Chưa có chiến dịch phù hợp.</Empty> : (
        <Section className="overflow-x-auto p-0">
          <table className="w-full min-w-[880px] text-sm">
            <thead className="text-left text-xs text-zinc-400">
              <tr className="border-b border-zinc-800">
                <th className="px-4 py-3 font-medium">Chiến dịch</th>
                <th className="px-4 py-3 font-medium">Mục tiêu</th>
                <th className="px-4 py-3 font-medium">Phạm vi</th>
                <th className="px-4 py-3 font-medium">Thời gian</th>
                <th className="px-4 py-3 font-medium">Phụ trách</th>
                <th className="px-4 py-3 text-right font-medium">Ưu đãi</th>
                {global && <th className="px-4 py-3 text-right font-medium">Chi phí / Ngân sách</th>}
                <th className="px-4 py-3 font-medium">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((c) => {
                const usage = budgetUsage(c.expenseTotal, c.plannedBudget);
                return (
                  <tr key={c.id} className="cursor-pointer border-b border-zinc-800/70 hover:bg-zinc-800/40" onClick={() => navigate(`/admin/marketing/campaigns/${c.id}`)}>
                    <td className="px-4 py-3">
                      <Link to={`/admin/marketing/campaigns/${c.id}`} className="font-semibold text-zinc-100 hover:text-amber-300" onClick={(e) => e.stopPropagation()}>{c.name}</Link>
                      <p className="text-xs text-zinc-500">{c.code}</p>
                    </td>
                    <td className="px-4 py-3 text-zinc-300">{OBJECTIVES[c.objective]?.label || c.objective}</td>
                    <td className="px-4 py-3 text-zinc-300">{c.storeName ? `Cửa hàng ${c.storeName}` : "Toàn chuỗi"}</td>
                    <td className="px-4 py-3 text-xs text-zinc-300">{formatWindow(c.startsAt, c.endsAt)}</td>
                    <td className="px-4 py-3 text-zinc-300">{c.ownerName || "—"}</td>
                    <td className="px-4 py-3 text-right tabular-nums text-zinc-300">{c.offerCount}</td>
                    {global && (
                      <td className="px-4 py-3 text-right text-xs tabular-nums">
                        <span className={usage !== null && usage > 100 ? "font-semibold text-rose-300" : "text-zinc-300"}>{formatMoney(c.expenseTotal)}</span>
                        <span className="text-zinc-500"> / {formatMoney(c.plannedBudget)}</span>
                      </td>
                    )}
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1.5">
                        <Badge map={CAMPAIGN_STATUS} value={c.status} />
                        {c.phase && <Badge map={CAMPAIGN_PHASE} value={c.phase} />}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </Section>
      )}
      <Pagination currentPage={page} totalPages={data?.page?.totalPages || data?.totalPages || 1} onPageChange={setPage} />

      {creating && can && (
        <CampaignFormDialog owners={can.owners} stores={can.stores} ownStoreName={can.storeName} onClose={() => setCreating(false)}
          onSaved={(detail) => navigate(`/admin/marketing/campaigns/${detail.campaign.id}`)} />
      )}
    </div>
  );
}
