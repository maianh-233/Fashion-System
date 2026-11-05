import { useState } from "react";
import { Link } from "react-router-dom";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import {
  Badge, ErrorText, Field, Loading, Section, Stat, control, useLoader, useRecruitment,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { APPLICATION_STATUS, POSITION_STATUS, CAMPAIGN_STATUS, formatDate, formatDateTime } from "./recruitmentLogic";

function Bar({ value, max, tone = "bg-amber-400" }) {
  const pct = max > 0 ? Math.min(100, (value / max) * 100) : 0;
  return <div className="h-2 w-full overflow-hidden rounded-full bg-zinc-800"><div className={`h-full rounded-full ${tone}`} style={{ width: `${pct}%` }} /></div>;
}

/** Dashboard tuyển dụng: KPI, phễu chuyển đổi, hiệu quả nguồn, tiến độ chiến dịch / vị trí, việc của tôi. */
export default function RecruitmentDashboard() {
  const { lookups, scope, sourceLabel } = useRecruitment();
  const [filter, setFilter] = useState({ storeId: "", campaignId: "", positionId: "", from: "", to: "" });
  const key = JSON.stringify(filter);
  const campaigns = useLoader((signal) => recruitmentApi.campaigns({ size: 100 }, { signal }), []);
  const positions = useLoader((signal) => (filter.campaignId
    ? recruitmentApi.positions({ campaignId: filter.campaignId }, { signal }) : Promise.resolve([])), [filter.campaignId]);
  const { data, error, loading } = useLoader((signal) => recruitmentApi.dashboard(filter, { signal }), [key]);
  const set = (patch) => setFilter((f) => ({ ...f, ...patch }));

  const k = data?.kpis;
  const maxFunnel = data?.funnel?.[0]?.entered || 0;
  return (
    <div className="space-y-5">
      <Section>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          {!scope.storeId && (
            <Field label="Cửa hàng">
              <select className={control} value={filter.storeId} onChange={(e) => set({ storeId: e.target.value, campaignId: "", positionId: "" })}>
                <option value="">Tất cả</option>
                {lookups?.stores?.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
              </select>
            </Field>
          )}
          <Field label="Chiến dịch">
            <select className={control} value={filter.campaignId} onChange={(e) => set({ campaignId: e.target.value, positionId: "" })}>
              <option value="">Tất cả</option>
              {campaigns.data?.content?.filter((c) => !filter.storeId || c.storeId === filter.storeId)
                .map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
          <Field label="Vị trí">
            <select className={control} value={filter.positionId} disabled={!filter.campaignId} onChange={(e) => set({ positionId: e.target.value })}>
              <option value="">Tất cả</option>
              {positions.data?.map((p) => <option key={p.id} value={p.id}>{p.title}</option>)}
            </select>
          </Field>
          <Field label="Nộp từ ngày"><input type="date" className={control} value={filter.from} onChange={(e) => set({ from: e.target.value })} /></Field>
          <Field label="Đến ngày"><input type="date" className={control} value={filter.to} onChange={(e) => set({ to: e.target.value })} /></Field>
        </div>
      </Section>
      <ErrorText>{error}</ErrorText>
      {loading && !data && <Loading />}
      {k && (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-8">
            <Stat label="Chiến dịch đang tuyển" value={k.openCampaigns} hint={`/${k.totalCampaigns} chiến dịch`} tone="amber" />
            <Stat label="Vị trí đang tuyển" value={k.openPositions} hint={`Cần ${k.totalHeadcount} người`} tone="amber" />
            <Stat label="Tổng hồ sơ" value={k.totalApplications} />
            <Stat label="Chờ xử lý" value={k.pendingApplications} tone="sky" />
            <Stat label="Đang trong quy trình" value={k.inProcess} tone="violet" />
            <Stat label="Đạt" value={k.passed} tone="emerald" hint={`${k.offersAccepted}/${k.offersSent} offer được nhận`} />
            <Stat label="Không đạt" value={k.failed} tone="rose" hint={`${k.withdrawn} rút hồ sơ`} />
            <Stat label="Đã tuyển" value={k.hired} tone="emerald" hint={`${k.upcomingInterviews} PV trong 7 ngày`} />
          </div>

          <div className="grid gap-5 xl:grid-cols-2">
            <Section title={filter.positionId ? "Tỷ lệ chuyển đổi qua từng vòng" : "Tỷ lệ chuyển đổi theo loại vòng"}>
              <div className="space-y-3">
                {data.funnel.map((step) => (
                  <div key={step.key}>
                    <div className="mb-1 flex justify-between text-sm">
                      <span className="text-zinc-200">{step.label}</span>
                      <span className="tabular-nums text-zinc-400">
                        {step.key === "APPLIED" || step.key === "HIRED" ? step.entered : `${step.passed}/${step.entered} đạt`}
                        {step.conversionRate != null && <span className="ml-2 text-amber-300">{step.conversionRate}%</span>}
                      </span>
                    </div>
                    <Bar value={step.key === "APPLIED" || step.key === "HIRED" ? step.entered : step.passed} max={maxFunnel}
                      tone={step.key === "HIRED" ? "bg-emerald-400" : "bg-amber-400"} />
                  </div>
                ))}
              </div>
            </Section>
            <Section title="Hiệu quả nguồn tuyển dụng">
              {data.sources.length === 0 ? <p className="text-sm text-zinc-500">Chưa có hồ sơ</p> : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="text-xs text-zinc-400"><tr>
                      {["Nguồn", "Hồ sơ", "Qua sàng lọc", "Offer", "Đã tuyển", "Tỷ lệ tuyển"].map((h) => <th key={h} className="px-2 py-2 text-left font-medium">{h}</th>)}
                    </tr></thead>
                    <tbody>
                      {data.sources.map((s) => (
                        <tr key={s.source} className="border-t border-zinc-800">
                          <td className="px-2 py-2 text-zinc-100">{sourceLabel(s.source)}</td>
                          <td className="px-2 py-2 tabular-nums">{s.applications}</td>
                          <td className="px-2 py-2 tabular-nums">{s.passedScreening}</td>
                          <td className="px-2 py-2 tabular-nums">{s.offers}</td>
                          <td className="px-2 py-2 tabular-nums text-emerald-300">{s.hired}</td>
                          <td className="px-2 py-2 tabular-nums">{s.hireRate ?? 0}%</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Section>
          </div>

          <Section title="Hồ sơ theo trạng thái">
            <div className="flex flex-wrap gap-2">
              {data.statuses.length === 0 && <p className="text-sm text-zinc-500">Chưa có hồ sơ</p>}
              {data.statuses.map((s) => (
                <Link key={s.status} to={`/admin/recruitment/applications?status=${s.status}`}>
                  <Badge map={APPLICATION_STATUS} value={s.status}>{s.label}: {s.count}</Badge>
                </Link>
              ))}
            </div>
          </Section>

          <div className="grid gap-5 xl:grid-cols-2">
            <Section title="Tiến độ theo chiến dịch">
              <ProgressTable rows={data.campaigns} map={CAMPAIGN_STATUS} link={(r) => `/admin/recruitment/campaigns/${r.id}`} parentLabel="Cửa hàng" />
            </Section>
            <Section title="Tiến độ theo vị trí">
              <ProgressTable rows={data.positions} map={POSITION_STATUS} link={(r) => `/admin/recruitment/positions/${r.id}`} parentLabel="Chiến dịch" />
            </Section>
          </div>

          <div className="grid gap-5 xl:grid-cols-2">
            <Section title="Việc đánh giá của tôi" action={<Link to="/admin/recruitment/applications?assignedToMe=true" className="text-xs text-amber-300">Xem tất cả</Link>}>
              {data.myTasks.length === 0 ? <p className="text-sm text-zinc-500">Không có phiếu đánh giá đang chờ bạn.</p> : (
                <ul className="divide-y divide-zinc-800">
                  {data.myTasks.map((t) => (
                    <li key={`${t.applicationId}-${t.stageId}`} className="flex items-center justify-between gap-3 py-2.5">
                      <div className="min-w-0">
                        <p className="truncate text-sm text-zinc-100">{t.candidateName} · {t.positionTitle}</p>
                        <p className="text-xs text-zinc-500">Vòng {t.stageName}{t.nextInterviewAt ? ` · PV ${formatDateTime(t.nextInterviewAt)}` : ""}</p>
                      </div>
                      <Link className="shrink-0 rounded-xl bg-amber-400 px-3 py-1.5 text-xs font-semibold text-zinc-950"
                        to={`/admin/recruitment/applications/${t.applicationId}/evaluate/${t.stageId}`}>
                        {t.evaluationStatus === "DRAFT" ? "Chấm tiếp" : "Chấm điểm"}
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </Section>
            <Section title="Phỏng vấn 7 ngày tới" action={<Link to="/admin/recruitment/interviews" className="text-xs text-amber-300">Lịch phỏng vấn</Link>}>
              {data.upcomingInterviews.length === 0 ? <p className="text-sm text-zinc-500">Không có lịch phỏng vấn.</p> : (
                <ul className="divide-y divide-zinc-800">
                  {data.upcomingInterviews.map((i) => (
                    <li key={i.id} className="py-2.5">
                      <Link to={`/admin/recruitment/applications/${i.applicationId}`} className="text-sm text-zinc-100 hover:text-amber-200">
                        {formatDateTime(i.startsAt)} · {i.candidateName}
                      </Link>
                      <p className="text-xs text-zinc-500">{i.positionTitle} · {i.title} · {i.participants.map((p) => p.fullName).join(", ")}</p>
                    </li>
                  ))}
                </ul>
              )}
            </Section>
          </div>
        </>
      )}
    </div>
  );
}

function ProgressTable({ rows, map, link, parentLabel }) {
  if (!rows.length) return <p className="text-sm text-zinc-500">Không có dữ liệu đang mở.</p>;
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[520px] text-sm">
        <thead className="text-xs text-zinc-400"><tr>
          {["Tên", parentLabel, "Đã tuyển / cần", "Hồ sơ", "Hạn"].map((h) => <th key={h} className="px-2 py-2 text-left font-medium">{h}</th>)}
        </tr></thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id} className="border-t border-zinc-800 align-top">
              <td className="px-2 py-2">
                <Link to={link(r)} className="text-zinc-100 hover:text-amber-200">{r.name}</Link>
                <div className="mt-1"><Badge map={map} value={r.status} /></div>
              </td>
              <td className="px-2 py-2 text-xs text-zinc-400">{r.parentName}</td>
              <td className="w-40 px-2 py-2">
                <p className="mb-1 tabular-nums text-xs text-zinc-300">{r.hired}/{r.headcount}{r.fillRate != null ? ` · ${r.fillRate}%` : ""}</p>
                <Bar value={r.hired} max={r.headcount} tone="bg-emerald-400" />
              </td>
              <td className="px-2 py-2 tabular-nums text-xs text-zinc-300">{r.applications} <span className="text-zinc-500">({r.inProcess} đang xử lý)</span></td>
              <td className="px-2 py-2 text-xs text-zinc-400">{formatDate(r.deadline)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
