import { useMemo, useState } from "react";
import { CalendarDays, ChevronLeft, ChevronRight, Repeat } from "lucide-react";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { myShiftApi } from "../../../api/admin/shiftApi";
import {
  Badge, Empty, ErrorText, Field, Loading, ReasonDialog, Section, btnDanger, btnGhost, btnPrimary, control, textarea, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { formatDate, formatDateTime, isoDate } from "../attendance/attendanceLogic";
import {
  ACTUAL_STATUS, SHIFT_COLORS, SWAP_STATUS, SWAP_TYPE, addDays, dayLabel, hoursText, mondayOf, timeRange,
} from "../attendance/shiftLogic";

const AVAILABILITY_DAYS = 14;

export default function MyShifts() {
  const [version, setVersion] = useState(0);
  const refresh = () => setVersion((v) => v + 1);
  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
      <AdminCatalogPageHeader icon={CalendarDays} eyebrow="Cá nhân" title="Lịch làm của tôi"
        description="Ca đã được công bố, đăng ký ngày rảnh / bận để quản lý xếp ca, và đổi ca hoặc nhờ đồng nghiệp làm thay." />
      <div className="grid gap-5 xl:grid-cols-[3fr_2fr] [&>*]:min-w-0">
        <WeekSchedule version={version} onChanged={refresh} />
        <Availability />
      </div>
      <Swaps version={version} onChanged={refresh} />
    </div>
  );
}

// ------------------------------------------------------------------ lịch tuần

function WeekSchedule({ version, onChanged }) {
  const [week, setWeek] = useState(mondayOf());
  const to = addDays(week, 6);
  const { data, error, loading } = useLoader((signal) => myShiftApi.mine({ from: week, to }, { signal }), [week, version]);
  const [swapFor, setSwapFor] = useState(null);
  const today = isoDate();
  const byDate = useMemo(() => new Map((data || []).map((e) => [e.date, e])), [data]);
  const total = (data || []).reduce((sum, e) => sum + e.paidMinutes, 0);

  return (
    <Section title="Lịch làm trong tuần" action={(
      <div className="flex items-center gap-1">
        <button type="button" className={btnGhost} onClick={() => setWeek(addDays(week, -7))} aria-label="Tuần trước"><ChevronLeft size={16} /></button>
        <span className="min-w-[8.5rem] text-center text-sm tabular-nums text-zinc-300">{dayLabel(week).date} – {dayLabel(to).date}</span>
        <button type="button" className={btnGhost} onClick={() => setWeek(addDays(week, 7))} aria-label="Tuần sau"><ChevronRight size={16} /></button>
      </div>
    )}>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : (
        <>
          <ul className="divide-y divide-zinc-800">
            {Array.from({ length: 7 }, (_, i) => addDays(week, i)).map((date) => {
              const entry = byDate.get(date);
              const { weekday, date: dm } = dayLabel(date);
              return (
                <li key={date} className={`flex flex-wrap items-center gap-3 py-3 ${date === today ? "rounded-xl bg-amber-400/5 px-2" : ""}`}>
                  <div className="w-14 shrink-0">
                    <p className={`text-sm font-semibold ${date === today ? "text-amber-300" : "text-zinc-200"}`}>{weekday}</p>
                    <p className="text-xs tabular-nums text-zinc-500">{dm}</p>
                  </div>
                  {entry ? (
                    <>
                      <div className={`min-w-[10rem] flex-1 rounded-xl border px-3 py-2 ${SHIFT_COLORS[entry.color] || SHIFT_COLORS.zinc}`}>
                        <p className="text-sm font-semibold">{entry.shiftName} <span className="font-normal opacity-80">{timeRange(entry.startTime, entry.endTime)}</span></p>
                        <p className="text-xs opacity-80">{entry.storeName}{entry.breakMinutes ? ` · nghỉ ${entry.breakMinutes}p` : ""}{entry.note ? ` · ${entry.note}` : ""}</p>
                      </div>
                      <Badge map={ACTUAL_STATUS} value={entry.actualStatus} />
                      {entry.swapPending ? <Badge tone="sky">Đang đổi ca</Badge> : entry.editable && (
                        <button type="button" className={btnGhost} onClick={() => setSwapFor(entry)}><Repeat size={14} />Đổi / nhờ làm thay</button>
                      )}
                    </>
                  ) : <p className="text-sm text-zinc-500">Không có ca</p>}
                </li>
              );
            })}
          </ul>
          <p className="mt-3 text-right text-xs text-zinc-400">Tổng: {(data || []).length} ca · {hoursText(total)} công</p>
        </>
      )}
      {swapFor && <SwapDialog entry={swapFor} onClose={() => setSwapFor(null)} onCreated={() => { setSwapFor(null); onChanged(); }} />}
    </Section>
  );
}

// ------------------------------------------------------------------ đăng ký rảnh / bận

function Availability() {
  const from = isoDate();
  const to = addDays(from, AVAILABILITY_DAYS - 1);
  const { data, error, loading, setData } = useLoader((signal) => myShiftApi.availability({ from, to }, { signal }), [from]);
  const [actionError, setActionError] = useState("");
  const [busy, setBusy] = useState("");
  const byDate = useMemo(() => new Map((data || []).map((a) => [a.date, a])), [data]);

  const choose = async (date, kind) => {
    setBusy(date);
    setActionError("");
    try {
      const current = byDate.get(date);
      if (current?.kind === kind) {
        await myShiftApi.clearAvailability(date);
        setData((data || []).filter((a) => a.date !== date));
      } else {
        const saved = await myShiftApi.setAvailability({ date, kind, note: current?.note || null });
        setData([...(data || []).filter((a) => a.date !== date), saved]);
      }
    } catch (e) {
      setActionError(e.message);
    } finally {
      setBusy("");
    }
  };
  const saveNote = async (date, note) => {
    const current = byDate.get(date);
    if (!current || (current.note || "") === note) return;
    try {
      const saved = await myShiftApi.setAvailability({ date, kind: current.kind, note: note || null });
      setData([...(data || []).filter((a) => a.date !== date), saved]);
    } catch (e) {
      setActionError(e.message);
    }
  };

  return (
    <Section title={`Đăng ký rảnh / bận (${AVAILABILITY_DAYS} ngày tới)`}>
      <p className="mb-3 text-xs text-zinc-400">Quản lý sẽ thấy đăng ký của bạn khi xếp ca. Bấm lại lựa chọn đang chọn để bỏ đăng ký.</p>
      <ErrorText>{error || actionError}</ErrorText>
      {loading && !data ? <Loading /> : (
        <ul className="space-y-1.5">
          {Array.from({ length: AVAILABILITY_DAYS }, (_, i) => addDays(from, i)).map((date) => {
            const current = byDate.get(date);
            const { weekday, date: dm } = dayLabel(date);
            return (
              <li key={date} className="flex flex-wrap items-center gap-2">
                <span className="w-16 text-sm text-zinc-300"><span className="font-semibold">{weekday}</span> <span className="text-xs tabular-nums text-zinc-500">{dm}</span></span>
                {[["AVAILABLE", "Rảnh", "border-emerald-400 bg-emerald-400/10 text-emerald-300"], ["UNAVAILABLE", "Bận", "border-rose-400 bg-rose-400/10 text-rose-300"]].map(([kind, label, active]) => (
                  <button key={kind} type="button" disabled={busy === date} onClick={() => choose(date, kind)}
                    className={`rounded-lg border px-2.5 py-1 text-xs ${current?.kind === kind ? active : "border-zinc-700 text-zinc-400 hover:text-zinc-200"}`}>{label}</button>
                ))}
                {current && (
                  <input key={`${date}-${current.kind}`} className={`${control} h-8 min-w-0 flex-1 text-xs`} maxLength={300} defaultValue={current.note || ""}
                    placeholder={current.kind === "AVAILABLE" ? "Ví dụ: chỉ rảnh buổi chiều" : "Lý do (không bắt buộc)"}
                    onBlur={(e) => saveNote(date, e.target.value.trim())} />
                )}
              </li>
            );
          })}
        </ul>
      )}
    </Section>
  );
}

// ------------------------------------------------------------------ đổi ca

function SwapDialog({ entry, onClose, onCreated }) {
  const { data, error, loading } = useLoader((signal) => myShiftApi.colleagues(entry.id, { signal }), [entry.id]);
  const [targetUserId, setTargetUserId] = useState("");
  const [targetScheduleId, setTargetScheduleId] = useState("");
  const [reason, setReason] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [saving, setSaving] = useState(false);
  const colleague = (data || []).find((c) => c.id === targetUserId);

  const submit = async (event) => {
    event.preventDefault();
    if (!targetScheduleId && !colleague?.freeOnDate) return setSubmitError("Đồng nghiệp này đã có ca hoặc nghỉ phép ngày đó — hãy chọn một ca của họ để đổi.");
    setSaving(true);
    setSubmitError("");
    try {
      await myShiftApi.createSwap({ scheduleId: entry.id, targetUserId, targetScheduleId: targetScheduleId || null, reason: reason.trim() });
      onCreated();
    } catch (e) {
      setSubmitError(e.message);
      setSaving(false);
    }
  };

  return (
    <AdminDialog onClose={onClose} size="lg">
      <form onSubmit={submit}>
        <AdminDialogHeader title="Đổi ca / nhờ làm thay"
          description={`Ca của bạn: ${entry.shiftName} ${timeRange(entry.startTime, entry.endTime)} ngày ${formatDate(entry.date)}. Đồng nghiệp đồng ý xong, quản lý cửa hàng sẽ duyệt.`} onClose={onClose} />
        <AdminDialogBody className="space-y-4">
          <ErrorText>{error}</ErrorText>
          {loading && !data ? <Loading /> : !data?.length ? <Empty>Cửa hàng chưa có đồng nghiệp nào khác.</Empty> : (
            <>
              <Field label="Đồng nghiệp" required>
                <select className={control} value={targetUserId} onChange={(e) => { setTargetUserId(e.target.value); setTargetScheduleId(""); }} required>
                  <option value="">Chọn đồng nghiệp…</option>
                  {data.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.fullName} — {c.freeOnDate ? "trống ngày này" : "đã có ca / nghỉ"}{c.availability === "AVAILABLE" ? " · đã báo rảnh" : c.availability === "UNAVAILABLE" ? " · đã báo bận" : ""}
                    </option>
                  ))}
                </select>
              </Field>
              {colleague && (
                <div className="space-y-2">
                  <p className="text-xs font-medium text-zinc-400">Hình thức</p>
                  <label className={`flex items-start gap-2 rounded-xl border px-3 py-2 text-sm ${!targetScheduleId ? "border-amber-400 bg-amber-400/5" : "border-zinc-700"} ${colleague.freeOnDate ? "" : "opacity-50"}`}>
                    <input type="radio" className="mt-1" checked={!targetScheduleId} disabled={!colleague.freeOnDate} onChange={() => setTargetScheduleId("")} />
                    <span><span className="font-medium text-zinc-100">Nhờ làm thay</span>
                      <span className="block text-xs text-zinc-400">{colleague.fullName} nhận ca của bạn, bạn không nhận lại ca nào.</span></span>
                  </label>
                  {colleague.upcoming.map((u) => (
                    <label key={u.id} className={`flex items-start gap-2 rounded-xl border px-3 py-2 text-sm ${targetScheduleId === u.id ? "border-amber-400 bg-amber-400/5" : "border-zinc-700"}`}>
                      <input type="radio" className="mt-1" checked={targetScheduleId === u.id} onChange={() => setTargetScheduleId(u.id)} />
                      <span><span className="font-medium text-zinc-100">Đổi lấy ca {u.shiftName} {timeRange(u.startTime, u.endTime)}</span>
                        <span className="block text-xs text-zinc-400">{dayLabel(u.date).weekday} {formatDate(u.date)}</span></span>
                    </label>
                  ))}
                  {!colleague.upcoming.length && <p className="text-xs text-zinc-500">{colleague.fullName} chưa có ca nào trong 30 ngày tới để đổi.</p>}
                </div>
              )}
              <Field label="Lý do" required>
                <textarea className={textarea} rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} required />
              </Field>
            </>
          )}
          <ErrorText>{submitError}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving || !targetUserId || !reason.trim()}>{saving ? "Đang gửi…" : "Gửi yêu cầu"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}

function Swaps({ version, onChanged }) {
  const { data, error, loading, reload } = useLoader((signal) => myShiftApi.swaps({ signal }), [version]);
  const [declining, setDeclining] = useState(null);
  const [actionError, setActionError] = useState("");
  const [busy, setBusy] = useState("");
  const me = data?.userId;
  const swaps = data?.swaps || [];
  const incoming = swaps.filter((s) => s.targetUserId === me && s.status === "PENDING_PEER");
  const others = swaps.filter((s) => !incoming.includes(s));

  const act = async (id, action) => {
    setBusy(id);
    setActionError("");
    try {
      await action();
      reload();
      onChanged();
    } catch (e) {
      setActionError(e.message);
    } finally {
      setBusy("");
    }
  };

  return (
    <Section title="Yêu cầu đổi ca">
      <ErrorText>{error || actionError}</ErrorText>
      {loading && !data ? <Loading /> : !swaps.length ? <Empty>Chưa có yêu cầu đổi ca nào.</Empty> : (
        <div className="space-y-4">
          {incoming.length > 0 && (
            <div className="space-y-2">
              <p className="text-sm font-semibold text-amber-300">Đồng nghiệp nhờ bạn ({incoming.length})</p>
              {incoming.map((s) => (
                <SwapRow key={s.id} swap={s} me={me}>
                  <button type="button" className={btnDanger} disabled={busy === s.id} onClick={() => setDeclining(s)}>Từ chối</button>
                  <button type="button" className={btnPrimary} disabled={busy === s.id} onClick={() => act(s.id, () => myShiftApi.accept(s.id))}>Đồng ý</button>
                </SwapRow>
              ))}
            </div>
          )}
          {others.length > 0 && (
            <div className="space-y-2">
              {incoming.length > 0 && <p className="text-sm font-semibold text-zinc-300">Lịch sử</p>}
              {others.map((s) => (
                <SwapRow key={s.id} swap={s} me={me}>
                  {s.requesterId === me && ["PENDING_PEER", "PENDING_APPROVAL"].includes(s.status) && (
                    <button type="button" className={btnGhost} disabled={busy === s.id} onClick={() => act(s.id, () => myShiftApi.cancel(s.id))}>Hủy yêu cầu</button>
                  )}
                </SwapRow>
              ))}
            </div>
          )}
        </div>
      )}
      {declining && (
        <ReasonDialog title="Từ chối đổi ca" label="Ghi chú cho đồng nghiệp" required={false} confirmText="Từ chối" danger
          onClose={() => setDeclining(null)}
          onConfirm={async (note) => { await myShiftApi.decline(declining.id, note || null); setDeclining(null); reload(); onChanged(); }} />
      )}
    </Section>
  );
}

function SwapRow({ swap: s, me, children }) {
  const outgoing = s.requesterId === me;
  return (
    <div className="flex flex-wrap items-start justify-between gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/40 p-3">
      <div className="min-w-0 space-y-1 text-sm">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-semibold text-zinc-100">{s.code}</span>
          <Badge tone={s.swapType === "SWAP" ? "violet" : "sky"}>{SWAP_TYPE[s.swapType]}</Badge>
          <Badge map={SWAP_STATUS} value={s.status} />
        </div>
        <p className="text-zinc-300">
          {outgoing ? <>Bạn → <b>{s.targetName}</b>: </> : <><b>{s.requesterName}</b> → bạn: </>}
          ca {s.requesterShift} ngày {formatDate(s.requesterDate)}
          {s.swapType === "SWAP" && <> ⇄ ca {s.targetShift} ngày {formatDate(s.targetDate)}</>}
        </p>
        <p className="text-xs text-zinc-500">Lý do: {s.reason} · {formatDateTime(s.createdAt)}</p>
        {s.rejectionReason && <p className="text-xs text-rose-300">{s.rejectionReason}</p>}
        {s.peerNote && <p className="text-xs text-zinc-400">Ghi chú đồng nghiệp: {s.peerNote}</p>}
      </div>
      {children && <div className="flex shrink-0 gap-2">{children}</div>}
    </div>
  );
}
