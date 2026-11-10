import { useState } from "react";
import { Link } from "react-router-dom";
import { AlertTriangle } from "lucide-react";
import { Empty, ErrorText, Field, Loading, Section, control, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { marketingApi } from "../../../api/admin/marketingApi";
import { CALENDAR_KINDS, defaultCalendarRange, formatWindow, shortDate, timelineBar, weekTicks } from "./marketingLogic";

/**
 * Lịch marketing dạng trục thời gian: mỗi chiến dịch một nhóm dòng gồm thời gian chiến dịch, thời gian hiệu lực của từng
 * ưu đãi và giờ gửi từng đợt — ba loại mốc tách riêng để thấy ưu đãi chạy lệch so với chiến dịch.
 */
export default function MarketingCalendar() {
  const [range, setRange] = useState(() => defaultCalendarRange());
  const { data, error, loading } = useLoader((signal) => marketingApi.calendar(range.from, range.to, { signal }), [range.from, range.to]);
  const groups = groupByCampaign(data?.events || []);
  const ticks = weekTicks(range.from, range.to);

  return (
    <div className="space-y-4">
      <Section>
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Từ ngày" className="flex-1 sm:flex-none"><input type="date" className={control} value={range.from} max={range.to} onChange={(e) => setRange({ ...range, from: e.target.value })} /></Field>
          <Field label="Đến ngày" className="flex-1 sm:flex-none"><input type="date" className={control} value={range.to} min={range.from} onChange={(e) => setRange({ ...range, to: e.target.value })} /></Field>
          <div className="flex flex-wrap gap-3 text-xs text-zinc-400">
            {Object.entries(CALENDAR_KINDS).map(([key, k]) => <span key={key} className="inline-flex items-center gap-1.5"><span className={`h-2.5 w-4 rounded ${k.bar}`} />{k.label}</span>)}
          </div>
        </div>
      </Section>
      <ErrorText>{error}</ErrorText>
      {data?.conflicts?.length > 0 && (
        <ul className="space-y-2" aria-label="Cảnh báo chồng lấn">
          {data.conflicts.map((c) => (
            <li key={c} className="flex items-start gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
              <AlertTriangle size={16} className="mt-0.5 shrink-0" /><span className="break-words">{c}</span>
            </li>
          ))}
        </ul>
      )}
      {loading && !data ? <Loading /> : groups.length === 0 ? <Empty>Không có chiến dịch nào trong khoảng này.</Empty> : (
        <Section className="overflow-x-auto p-0">
          <div className="min-w-[760px] p-4" data-table-mode="scroll">
            <div className="relative mb-2 ml-[220px] h-5 border-b border-zinc-800 text-[10px] text-zinc-500">
              {ticks.map((t) => {
                const { left } = timelineBar(`${t}T00:00:00`, `${t}T00:00:00`, range.from, range.to, 0);
                return <span key={t} className="absolute -translate-x-1/2" style={{ left: `${left}%` }}>{shortDate(t)}</span>;
              })}
            </div>
            {groups.map((g) => (
              <div key={g.campaignId} className="border-b border-zinc-800/70 py-2">
                {g.events.map((e, index) => {
                  const bar = timelineBar(e.startsAt, e.endsAt, range.from, range.to);
                  const kind = CALENDAR_KINDS[e.kind];
                  return (
                    <div key={`${e.kind}-${e.label}-${index}`} className="flex items-center gap-3 py-0.5">
                      <div className="w-[208px] shrink-0 truncate text-xs">
                        {e.kind === "CAMPAIGN"
                          ? <Link to={`/admin/marketing/campaigns/${e.campaignId}`} className="font-semibold text-zinc-100 hover:text-amber-300">{e.campaignCode} · {e.label}</Link>
                          : <span className="pl-3 text-zinc-400">{kind.label}: {e.label}</span>}
                      </div>
                      <div className="relative h-4 flex-1 rounded bg-zinc-900">
                        <div className={`absolute inset-y-0 rounded ${kind.bar} ${e.kind === "CAMPAIGN" && e.campaignStatus !== "APPROVED" ? "opacity-50" : ""}`}
                          style={{ left: `${bar.left}%`, width: `${bar.width}%` }} title={`${e.label}: ${formatWindow(e.startsAt, e.endsAt)}`} />
                      </div>
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        </Section>
      )}
      <p className="text-xs text-zinc-500">Thanh mờ là chiến dịch chưa được duyệt. Lịch xem tối đa 93 ngày mỗi lần.</p>
    </div>
  );
}

function groupByCampaign(events) {
  const groups = new Map();
  events.forEach((e) => {
    if (!groups.has(e.campaignId)) groups.set(e.campaignId, { campaignId: e.campaignId, events: [] });
    groups.get(e.campaignId).events.push(e);
  });
  // Dòng chiến dịch đứng đầu nhóm, sau đó ưu đãi rồi đợt gửi.
  const order = { CAMPAIGN: 0, OFFER: 1, DISPATCH: 2 };
  return [...groups.values()].map((g) => ({ ...g, events: g.events.sort((a, b) => order[a.kind] - order[b.kind]) }));
}
