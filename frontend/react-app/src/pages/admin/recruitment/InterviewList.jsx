import { useState } from "react";
import { Link } from "react-router-dom";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { Badge, ErrorText, Loading, Section, btnGhost, control, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { INTERVIEW_MODES, INTERVIEW_STATUS, labelOf } from "./recruitmentLogic";

const pad = (n) => String(n).padStart(2, "0");
const iso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T00:00:00`;
function startOfWeek(date) {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  return d;
}

/** Lịch phỏng vấn theo tuần, lọc trạng thái / chỉ lịch của tôi. */
export default function InterviewList() {
  const [week, setWeek] = useState(() => startOfWeek(new Date()));
  const [status, setStatus] = useState("");
  const [mine, setMine] = useState(false);
  const end = new Date(week); end.setDate(end.getDate() + 7);
  const params = { from: iso(week), to: iso(end), status, mine: mine || undefined, size: 200 };
  const { data, error, loading } = useLoader((signal) => recruitmentApi.interviews(params, { signal }), [JSON.stringify(params)]);
  const shift = (days) => setWeek((w) => { const d = new Date(w); d.setDate(d.getDate() + days); return d; });
  const days = Array.from({ length: 7 }, (_, i) => { const d = new Date(week); d.setDate(d.getDate() + i); return d; });
  const items = data?.content || [];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <button type="button" className={btnGhost} onClick={() => shift(-7)} aria-label="Tuần trước"><ChevronLeft size={15} /></button>
        <p className="min-w-52 text-center text-sm font-semibold">{days[0].toLocaleDateString("vi-VN")} – {days[6].toLocaleDateString("vi-VN")}</p>
        <button type="button" className={btnGhost} onClick={() => shift(7)} aria-label="Tuần sau"><ChevronRight size={15} /></button>
        <button type="button" className={btnGhost} onClick={() => setWeek(startOfWeek(new Date()))}>Tuần này</button>
        <select className={`${control} w-44`} value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">Mọi trạng thái</option>
          {Object.entries(INTERVIEW_STATUS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
        </select>
        <label className="flex items-center gap-2 text-sm text-zinc-300"><input type="checkbox" checked={mine} onChange={(e) => setMine(e.target.checked)} />Chỉ lịch của tôi</label>
      </div>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-7">
          {days.map((day) => {
            const list = items.filter((i) => new Date(i.startsAt).toDateString() === day.toDateString());
            const today = day.toDateString() === new Date().toDateString();
            return (
              <Section key={day.toISOString()} className={`!p-3 ${today ? "!border-amber-400/50" : ""}`}>
                <p className={`mb-2 text-xs font-semibold uppercase ${today ? "text-amber-300" : "text-zinc-400"}`}>
                  {day.toLocaleDateString("vi-VN", { weekday: "short", day: "2-digit", month: "2-digit" })}
                </p>
                {list.length === 0 && <p className="text-xs text-zinc-600">—</p>}
                <ul className="space-y-2">
                  {list.map((i) => (
                    <li key={i.id}>
                      <Link to={`/admin/recruitment/applications/${i.applicationId}`} className="block rounded-xl border border-zinc-800 bg-zinc-950 p-2.5 hover:border-amber-400/50">
                        <p className="text-xs font-semibold tabular-nums text-amber-200">
                          {new Date(i.startsAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}–{new Date(i.endsAt).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })}
                        </p>
                        <p className="truncate text-sm text-zinc-100">{i.candidateName}</p>
                        <p className="truncate text-xs text-zinc-500">{i.positionTitle} · {i.stageName}</p>
                        <p className="truncate text-xs text-zinc-500">{labelOf(INTERVIEW_MODES, i.mode)}{i.location ? ` · ${i.location}` : ""}</p>
                        <p className="truncate text-xs text-zinc-500">{i.participants.map((p) => p.fullName).join(", ")}</p>
                        <div className="mt-1.5"><Badge map={INTERVIEW_STATUS} value={i.status} /></div>
                      </Link>
                    </li>
                  ))}
                </ul>
              </Section>
            );
          })}
        </div>
      )}
    </div>
  );
}
