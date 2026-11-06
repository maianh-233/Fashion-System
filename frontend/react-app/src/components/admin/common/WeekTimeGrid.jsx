import { useEffect, useMemo, useState } from "react";
import { CalendarDays, List } from "lucide-react";

/**
 * Lịch tuần theo giờ ("thời khóa biểu"): cột là ngày, trục dọc là giờ, mỗi sự kiện nằm đúng khung giờ; sự kiện trùng
 * giờ được chia làn cạnh nhau. Có vạch giờ hiện tại và chế độ danh sách (mặc định trên màn hình hẹp).
 *
 * events: [{ id, date: "yyyy-MM-dd", start: "HH:mm", end: "HH:mm", title, subtitle?, meta?, className?, badge?, onClick? }]
 * Ca qua đêm (end ≤ start) hiển thị tới cuối ngày.
 */
export default function WeekTimeGrid({ days, events, minHour = 7, maxHour = 22, hourHeight = 52, emptyText = "Không có lịch trong tuần này.", renderEmptyDay }) {
  const [view, setView] = useState(() => (typeof window !== "undefined" && window.matchMedia?.("(max-width: 767px)").matches ? "list" : "grid"));
  const now = useNow();
  const today = isoOf(now);

  const parsed = useMemo(() => events.map((e) => {
    const start = toMinutes(e.start);
    let end = toMinutes(e.end);
    const overnight = end <= start;
    if (overnight) end = 24 * 60;
    return { ...e, startMin: start, endMin: end, overnight };
  }), [events]);

  // Khung giờ hiển thị: mặc định minHour–maxHour, nới ra nếu có sự kiện ngoài khung.
  const range = useMemo(() => {
    let lo = minHour * 60;
    let hi = maxHour * 60;
    parsed.forEach((e) => { lo = Math.min(lo, Math.floor(e.startMin / 60) * 60); hi = Math.max(hi, Math.ceil(e.endMin / 60) * 60); });
    return { lo, hi: Math.min(hi, 24 * 60) };
  }, [parsed, minHour, maxHour]);

  const byDay = useMemo(() => {
    const map = new Map(days.map((d) => [d, []]));
    parsed.forEach((e) => map.get(e.date)?.push(e));
    map.forEach((list, d) => map.set(d, layoutLanes(list.sort((a, b) => a.startMin - b.startMin || b.endMin - a.endMin))));
    return map;
  }, [days, parsed]);

  // Cột ngày rộng theo số làn trùng giờ: ngày trống co lại, ngày đông người giãn ra (cuộn ngang nếu cần).
  const weights = days.map((d) => Math.max(1, ...(byDay.get(d) || []).map((e) => e.lanes)));
  const columns = `3.25rem ${weights.map((w) => `minmax(${w * 4.5}rem, ${w}fr)`).join(" ")}`;
  const minWidth = `calc(3.25rem + ${weights.reduce((a, b) => a + b, 0) * 4.5}rem)`;
  const hours = [];
  for (let m = range.lo; m < range.hi; m += 60) hours.push(m / 60);
  const height = ((range.hi - range.lo) / 60) * hourHeight;
  const nowMin = now.getHours() * 60 + now.getMinutes();

  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <div className="inline-flex rounded-xl border border-zinc-700 p-0.5" role="group" aria-label="Kiểu hiển thị lịch">
          {[["grid", "Lịch theo giờ", CalendarDays], ["list", "Danh sách", List]].map(([key, label, Icon]) => (
            <button key={key} type="button" onClick={() => setView(key)} aria-pressed={view === key}
              className={`inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1 text-xs ${view === key ? "bg-amber-400 font-semibold text-zinc-950" : "text-zinc-400 hover:text-zinc-200"}`}>
              <Icon size={13} />{label}
            </button>
          ))}
        </div>
      </div>

      {view === "list" ? (
        <AgendaList days={days} byDay={byDay} today={today} emptyText={emptyText} renderEmptyDay={renderEmptyDay} />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-zinc-800">
          <div style={{ minWidth }}>
            <div className="sticky top-0 z-20 grid border-b border-zinc-800 bg-zinc-900" style={{ gridTemplateColumns: columns }}>
              <div />
              {days.map((d) => {
                const { weekday, dm } = label(d);
                const isToday = d === today;
                return (
                  <div key={d} className={`border-l border-zinc-800 px-2 py-2 text-center ${isToday ? "bg-amber-400/10" : ""}`}>
                    <p className={`text-[11px] font-semibold uppercase tracking-wide ${isToday ? "text-amber-300" : "text-zinc-500"}`}>{weekday}</p>
                    <p className={`text-sm font-semibold tabular-nums ${isToday ? "text-amber-300" : "text-zinc-200"}`}>{dm}</p>
                  </div>
                );
              })}
            </div>
            <div className="relative grid" style={{ height, gridTemplateColumns: columns }}>
              <div className="relative">
                {hours.map((h, i) => (
                  <span key={h} className="absolute right-1.5 -translate-y-1/2 text-[11px] tabular-nums text-zinc-500" style={{ top: i * hourHeight }}>
                    {i === 0 ? "" : `${String(h).padStart(2, "0")}:00`}
                  </span>
                ))}
              </div>
              {days.map((d) => (
                <div key={d} className={`relative border-l border-zinc-800 ${d === today ? "bg-amber-400/[0.04]" : ""}`}>
                  {hours.map((h, i) => (
                    <div key={h} className="absolute inset-x-0 border-t border-zinc-800/70" style={{ top: i * hourHeight }}>
                      <div className="absolute inset-x-0 border-t border-dashed border-zinc-800/40" style={{ top: hourHeight / 2 }} />
                    </div>
                  ))}
                  {d === today && nowMin >= range.lo && nowMin <= range.hi && (
                    <div className="absolute inset-x-0 z-10 flex items-center" style={{ top: ((nowMin - range.lo) / 60) * hourHeight }} aria-hidden="true">
                      <span className="-ml-1 h-2 w-2 rounded-full bg-rose-500" />
                      <span className="h-px flex-1 bg-rose-500" />
                    </div>
                  )}
                  {byDay.get(d)?.map((e) => {
                    const top = ((Math.max(e.startMin, range.lo) - range.lo) / 60) * hourHeight;
                    const h = Math.max(22, ((Math.min(e.endMin, range.hi) - Math.max(e.startMin, range.lo)) / 60) * hourHeight - 2);
                    const width = 100 / e.lanes;
                    const Tag = e.onClick ? "button" : "div";
                    return (
                      <Tag key={e.id} type={e.onClick ? "button" : undefined} onClick={e.onClick}
                        title={[`${e.start}–${e.end}`, e.title, e.subtitle, e.meta].filter(Boolean).join("\n")}
                        className={`absolute flex flex-col items-stretch justify-start overflow-hidden rounded-lg border px-1.5 py-1 text-left text-xs leading-tight shadow-sm ${e.className || "shift-chip shift-chip--amber"} ${e.onClick ? "cursor-pointer hover:brightness-110 focus-visible:outline focus-visible:outline-2 focus-visible:outline-amber-400" : ""}`}
                        style={{ top: top + 1, height: h, left: `calc(${e.lane * width}% + 2px)`, width: `calc(${width}% - 4px)` }}>
                        <p className="truncate font-semibold tabular-nums">{e.start}–{e.overnight ? `${e.end} (+1)` : e.end}</p>
                        <p className="truncate font-medium">{e.title}</p>
                        {h > 52 && e.subtitle && <p className="truncate opacity-80">{e.subtitle}</p>}
                        {h > 70 && e.meta && <p className="truncate opacity-70">{e.meta}</p>}
                        {h > 88 && e.badge && <div className="mt-1">{e.badge}</div>}
                      </Tag>
                    );
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function AgendaList({ days, byDay, today, emptyText, renderEmptyDay }) {
  const total = days.reduce((n, d) => n + (byDay.get(d)?.length || 0), 0);
  if (!total && !renderEmptyDay) return <p className="py-10 text-center text-sm text-zinc-500">{emptyText}</p>;
  return (
    <ol className="divide-y divide-zinc-800 rounded-2xl border border-zinc-800">
      {days.map((d) => {
        const list = byDay.get(d) || [];
        const { weekday, dm } = label(d);
        const isToday = d === today;
        return (
          <li key={d} className={`flex gap-3 px-3 py-3 ${isToday ? "bg-amber-400/5" : ""}`}>
            <div className="w-12 shrink-0">
              <p className={`text-xs font-semibold uppercase ${isToday ? "text-amber-300" : "text-zinc-500"}`}>{weekday}</p>
              <p className={`text-sm font-semibold tabular-nums ${isToday ? "text-amber-300" : "text-zinc-200"}`}>{dm}</p>
              {isToday && <p className="text-[10px] font-semibold uppercase text-amber-300">Hôm nay</p>}
            </div>
            <div className="min-w-0 flex-1 space-y-2">
              {!list.length && (renderEmptyDay ? renderEmptyDay(d) : <p className="pt-1 text-sm text-zinc-600">Trống</p>)}
              {list.map((e) => {
                const Tag = e.onClick ? "button" : "div";
                return (
                  <Tag key={e.id} type={e.onClick ? "button" : undefined} onClick={e.onClick}
                    className={`block w-full rounded-xl border px-3 py-2 text-left text-sm ${e.className || "shift-chip shift-chip--amber"}`}>
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span className="font-semibold tabular-nums">{e.start}–{e.overnight ? `${e.end} (+1)` : e.end}</span>
                      {e.badge}
                    </div>
                    <p className="font-medium">{e.title}</p>
                    {e.subtitle && <p className="text-xs opacity-80">{e.subtitle}</p>}
                    {e.meta && <p className="text-xs opacity-70">{e.meta}</p>}
                  </Tag>
                );
              })}
            </div>
          </li>
        );
      })}
    </ol>
  );
}

/** Chia làn cho sự kiện trùng giờ: mỗi cụm sự kiện chồng nhau dùng chung số làn. */
function layoutLanes(list) {
  const result = [];
  let cluster = [];
  let clusterEnd = -1;
  const flush = () => {
    const laneEnds = [];
    cluster.forEach((e) => {
      let lane = laneEnds.findIndex((end) => end <= e.startMin);
      if (lane === -1) { lane = laneEnds.length; laneEnds.push(e.endMin); } else laneEnds[lane] = e.endMin;
      e.lane = lane;
    });
    cluster.forEach((e) => { e.lanes = laneEnds.length; result.push(e); });
    cluster = [];
    clusterEnd = -1;
  };
  list.forEach((src) => {
    const e = { ...src };
    if (cluster.length && e.startMin >= clusterEnd) flush();
    cluster.push(e);
    clusterEnd = Math.max(clusterEnd, e.endMin);
  });
  if (cluster.length) flush();
  return result;
}

function useNow() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 60_000);
    return () => clearInterval(timer);
  }, []);
  return now;
}

const WEEKDAYS = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];
const pad = (n) => String(n).padStart(2, "0");
const isoOf = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const toMinutes = (hhmm) => {
  const [h, m] = String(hhmm || "0:0").split(":").map(Number);
  return h * 60 + (m || 0);
};
function label(iso) {
  const d = new Date(`${iso}T00:00:00`);
  return { weekday: WEEKDAYS[d.getDay()], dm: `${pad(d.getDate())}/${pad(d.getMonth() + 1)}` };
}
