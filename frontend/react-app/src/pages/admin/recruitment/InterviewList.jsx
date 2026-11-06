import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { Badge, ErrorText, Loading, Section, btnGhost, control, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import WeekTimeGrid from "../../../components/admin/common/WeekTimeGrid";
import { SHIFT_COLORS } from "../attendance/shiftLogic";
import { INTERVIEW_MODES, INTERVIEW_STATUS, labelOf } from "./recruitmentLogic";

const pad = (n) => String(n).padStart(2, "0");
const isoDay = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const iso = (d) => `${isoDay(d)}T00:00:00`;
const hhmm = (value) => new Date(value).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit", hour12: false });
const STATUS_COLOR = { SCHEDULED: "sky", COMPLETED: "emerald", CANCELLED: "zinc", NO_SHOW: "rose" };

function startOfWeek(date) {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  return d;
}

/** Lịch phỏng vấn theo tuần (lưới giờ hoặc danh sách), lọc trạng thái / chỉ lịch của tôi. */
export default function InterviewList() {
  const navigate = useNavigate();
  const [week, setWeek] = useState(() => startOfWeek(new Date()));
  const [status, setStatus] = useState("");
  const [mine, setMine] = useState(false);
  const end = new Date(week); end.setDate(end.getDate() + 7);
  const params = { from: iso(week), to: iso(end), status, mine: mine || undefined, size: 200 };
  const { data, error, loading } = useLoader((signal) => recruitmentApi.interviews(params, { signal }), [JSON.stringify(params)]);
  const shift = (days) => setWeek((w) => { const d = new Date(w); d.setDate(d.getDate() + days); return d; });
  const days = Array.from({ length: 7 }, (_, i) => { const d = new Date(week); d.setDate(d.getDate() + i); return isoDay(d); });
  const items = data?.content || [];
  const thisWeek = isoDay(startOfWeek(new Date())) === days[0];

  const events = items.map((i) => ({
    id: i.id,
    date: isoDay(new Date(i.startsAt)),
    start: hhmm(i.startsAt),
    end: hhmm(i.endsAt),
    title: i.candidateName,
    subtitle: `${i.positionTitle} · ${i.stageName}`,
    meta: [labelOf(INTERVIEW_MODES, i.mode), i.location, i.participants.map((p) => p.fullName).join(", ")].filter(Boolean).join(" · "),
    badge: <Badge map={INTERVIEW_STATUS} value={i.status} />,
    className: `${SHIFT_COLORS[STATUS_COLOR[i.status] || "amber"]} ${i.status === "CANCELLED" ? "line-through opacity-60" : ""}`,
    onClick: () => navigate(`/admin/recruitment/applications/${i.applicationId}`),
  }));
  const counts = Object.keys(INTERVIEW_STATUS).map((k) => [k, items.filter((i) => i.status === k).length]).filter(([, n]) => n);

  return (
    <div className="space-y-4">
      <Section>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-1">
            <button type="button" className={btnGhost} onClick={() => shift(-7)} aria-label="Tuần trước"><ChevronLeft size={15} /></button>
            <p className="min-w-52 text-center text-sm font-semibold tabular-nums">
              {new Date(`${days[0]}T00:00:00`).toLocaleDateString("vi-VN")} – {new Date(`${days[6]}T00:00:00`).toLocaleDateString("vi-VN")}
            </p>
            <button type="button" className={btnGhost} onClick={() => shift(7)} aria-label="Tuần sau"><ChevronRight size={15} /></button>
            {!thisWeek && <button type="button" className={btnGhost} onClick={() => setWeek(startOfWeek(new Date()))}>Tuần này</button>}
          </div>
          <select className={`${control} w-44`} value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Lọc trạng thái">
            <option value="">Mọi trạng thái</option>
            {Object.entries(INTERVIEW_STATUS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
          </select>
          <label className="flex items-center gap-2 text-sm text-zinc-300"><input type="checkbox" checked={mine} onChange={(e) => setMine(e.target.checked)} />Chỉ lịch của tôi</label>
          <div className="ml-auto flex flex-wrap gap-2 text-xs text-zinc-400">
            {items.length ? <span>{items.length} buổi:</span> : null}
            {counts.map(([k, n]) => <Badge key={k} map={INTERVIEW_STATUS} value={k}>{INTERVIEW_STATUS[k].label} · {n}</Badge>)}
          </div>
        </div>
      </Section>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : (
        <WeekTimeGrid days={days} events={events} minHour={8} maxHour={18} emptyText="Không có buổi phỏng vấn nào trong tuần này." />
      )}
      <p className="text-xs text-zinc-500">Bấm vào một buổi phỏng vấn để mở hồ sơ ứng viên. Màu theo trạng thái: xanh dương = đã lên lịch, xanh lá = đã phỏng vấn, đỏ = không đến, xám = đã hủy.</p>
    </div>
  );
}
