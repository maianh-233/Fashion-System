import { useMemo, useRef, useState } from "react";
import PageGuide from "../../../components/admin/common/PageGuide";
import { Link } from "react-router-dom";
import { AlertTriangle, CalendarRange, ChevronLeft, ChevronRight, Copy, GripVertical, Plus, Send, Settings2, Trash2, UserPlus, Repeat } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { attendanceApi } from "../../../api/admin/attendanceApi";
import { shiftApi } from "../../../api/admin/shiftApi";
import { useAdminPermissions } from "../../../contexts/AdminPermissionsContext";
import {
  Badge, Empty, ErrorText, Field, Loading, Section, Stat, btnDanger, btnGhost, btnPrimary, control, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { useAttendance } from "./AttendanceLayout";
import { isoDate } from "./attendanceLogic";
import WeekTimeGrid from "../../../components/admin/common/WeekTimeGrid";
import {
  ACTUAL_STATUS, COLOR_OPTIONS, SHIFT_COLORS, SHIFT_DOT, SHIFT_TYPES, addDays, dayLabel, hhmm, hoursText, mondayOf, timeRange,
  toApiTime,
} from "./shiftLogic";

/** Lịch xếp ca theo tuần: kéo ca mẫu vào ô để xếp, kéo thẻ ca sang ô khác để chuyển, bấm để sửa. */
export default function ShiftSchedule() {
  const { stores, scopeStoreId } = useAttendance();
  const { hasPermission } = useAdminPermissions();
  const canManage = hasPermission("SHIFT_MANAGE") || hasPermission("ATTENDANCE_ADMIN");
  const [storeId, setStoreId] = useState(scopeStoreId || "");
  const effectiveStore = scopeStoreId || storeId || stores[0]?.id || "";
  const [week, setWeek] = useState(mondayOf());
  const { data, error, loading, reload } = useLoader(
    (signal) => (effectiveStore ? shiftApi.schedule({ storeId: effectiveStore, week }, { signal }) : Promise.resolve(null)),
    [effectiveStore, week],
  );
  const [dialog, setDialog] = useState(null);
  const [actionError, setActionError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [extraRows, setExtraRows] = useState([]);

  const run = async (action, message) => {
    setBusy(true);
    setActionError("");
    setNotice("");
    try {
      const result = await action();
      if (message) setNotice(typeof message === "function" ? message(result) : message);
      reload();
      return result;
    } catch (e) {
      setActionError(e.message);
      return null;
    } finally {
      setBusy(false);
    }
  };

  const publish = () => run(() => shiftApi.publish({ storeId: effectiveStore, from: data.from, to: data.to }),
    (r) => `Đã công bố ${r.affected} ca. Nhân viên đã nhận thông báo.`);
  const copyPrevious = () => run(() => shiftApi.copyWeek({ storeId: effectiveStore, sourceWeek: addDays(week, -7), targetWeek: week }),
    (r) => `Đã sao chép ${r.affected} ca thành nháp${r.skipped ? `, bỏ qua ${r.skipped}: ${r.messages.slice(0, 3).join("; ")}${r.skipped > 3 ? "…" : ""}` : ""}.`);

  return (
    <div className="space-y-5">
      <PageGuide id="shiftSchedule" />
      <Section>
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Cửa hàng" className="min-w-[12rem] flex-1 sm:flex-none">
            <select className={control} value={effectiveStore} onChange={(e) => { setStoreId(e.target.value); setExtraRows([]); }} disabled={Boolean(scopeStoreId)}>
              {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <div className="flex items-center gap-1">
            <button type="button" className={btnGhost} onClick={() => setWeek(addDays(week, -7))} aria-label="Tuần trước"><ChevronLeft size={16} /></button>
            <div className="min-w-[9.5rem] rounded-xl border border-zinc-800 px-3 py-2 text-center text-sm tabular-nums text-zinc-200">
              {dayLabel(week).date} – {dayLabel(addDays(week, 6)).date}/{addDays(week, 6).slice(0, 4)}
            </div>
            <button type="button" className={btnGhost} onClick={() => setWeek(addDays(week, 7))} aria-label="Tuần sau"><ChevronRight size={16} /></button>
            {week !== mondayOf() && <button type="button" className={btnGhost} onClick={() => setWeek(mondayOf())}>Tuần này</button>}
          </div>
          {canManage && data && (
            <div className="ml-auto flex flex-wrap gap-2">
              <button type="button" className={btnGhost} onClick={() => setDialog({ type: "templates" })}><Settings2 size={15} />Ca mẫu</button>
              <button type="button" className={btnGhost} onClick={copyPrevious} disabled={busy}><Copy size={15} />Sao chép tuần trước</button>
              <button type="button" className={btnPrimary} onClick={publish} disabled={busy || !data.stats.drafts}>
                <Send size={15} />Công bố{data.stats.drafts ? ` ${data.stats.drafts} ca nháp` : ""}
              </button>
            </div>
          )}
        </div>
      </Section>

      <ErrorText>{error || actionError}</ErrorText>
      {notice && <p className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-200">{notice}</p>}
      {!effectiveStore && <Empty>Chưa có cửa hàng để xếp ca.</Empty>}
      {loading && !data ? <Loading /> : data && (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
            <Stat label="Ca trong tuần" value={data.stats.shifts} />
            <Stat label="Tổng giờ công" value={hoursText(data.stats.paidMinutes)} tone="sky" />
            <Stat label="Ca nháp" value={data.stats.drafts} tone={data.stats.drafts ? "amber" : "zinc"} hint="Nhân viên chưa thấy" />
            <Stat label="Vắng ca" value={data.stats.absent} tone={data.stats.absent ? "rose" : "zinc"} />
            <Stat label="Đi trễ / về sớm" value={`${data.stats.late} / ${data.stats.early}`} tone="amber" />
            <Link to="/admin/attendance/swaps" className="block rounded-2xl transition hover:ring-1 hover:ring-amber-400/50">
              <Stat label="Đổi ca chờ duyệt" value={data.stats.pendingSwaps} tone={data.stats.pendingSwaps ? "amber" : "zinc"} hint="Bấm để xử lý" />
            </Link>
          </div>
          <ScheduleGrid data={data} canManage={canManage} busy={busy} extraRows={extraRows} storeId={effectiveStore}
            onOpen={setDialog} run={run} onAddRow={(emp) => setExtraRows((rows) => rows.some((r) => r.id === emp.id) ? rows : [...rows, emp])} />
        </>
      )}

      {dialog?.type === "entry" && (
        <EntryDialog grid={data} storeId={effectiveStore} entry={dialog.entry} userId={dialog.userId} date={dialog.date}
          canManage={canManage} onClose={() => setDialog(null)} onSaved={() => { setDialog(null); reload(); }} />
      )}
      {dialog?.type === "templates" && (
        <TemplatesDialog storeId={effectiveStore} onClose={() => { setDialog(null); reload(); }} />
      )}
    </div>
  );
}

// ------------------------------------------------------------------ lưới tuần

function ScheduleGrid({ data, canManage, busy, extraRows, storeId, onOpen, run, onAddRow }) {
  const today = isoDate();
  const drag = useRef(null);
  const [over, setOver] = useState("");
  const [view, setView] = useState("roster");
  // Nền cột: hôm nay nổi bật, ngày lễ / nghỉ tuần nhạt hơn để dễ dò theo cột.
  const columnTone = (d) => (d.date === today ? "bg-amber-400/[0.07]" : d.holiday ? "bg-rose-500/[0.06]" : d.weeklyOff ? "bg-zinc-500/[0.08]" : "");

  const rows = useMemo(() => {
    const list = [...data.employees];
    extraRows.forEach((e) => {
      if (!list.some((x) => x.id === e.id)) list.push({ id: e.id, employeeCode: e.employeeCode, fullName: e.fullName, employeeType: "GLOBAL", storeMember: false, scheduledMinutes: 0, shiftCount: 0 });
    });
    return list;
  }, [data.employees, extraRows]);

  const index = useMemo(() => {
    const key = (u, d) => `${u}|${d}`;
    const entries = new Map();
    data.entries.forEach((e) => entries.set(key(e.userId, e.date), e));
    const availability = new Map(data.availability.map((a) => [key(a.userId, a.date), a]));
    const leaves = new Map(data.leaves.map((l) => [key(l.userId, l.date), l]));
    return { entries, availability, leaves, key };
  }, [data]);

  const droppable = (userId, date) => {
    if (!canManage || busy || date < today) return false;
    const existing = index.entries.get(index.key(userId, date));
    const leave = index.leaves.get(index.key(userId, date));
    if (leave && !leave.halfDay) return false;
    return !existing || existing.id === drag.current?.id;
  };

  const drop = async (userId, date) => {
    const item = drag.current;
    drag.current = null;
    setOver("");
    if (!item || !droppable(userId, date)) return;
    if (item.kind === "template") {
      await run(() => shiftApi.createEntry({ userId, storeId, date, shiftId: item.id }));
    } else if (item.kind === "entry") {
      const e = item.entry;
      if (e.userId === userId && e.date === date) return;
      await run(() => shiftApi.updateEntry(e.id, {
        userId, storeId, date, shiftId: e.shiftId, startTime: e.startTime, endTime: e.endTime, breakMinutes: e.breakMinutes, note: e.note,
      }));
    }
  };

  const limit = data.weeklyHourLimit * 60;

  return (
    <Section title="Lịch tuần" action={(
      <div className="flex flex-wrap items-center gap-2">
        <div className="inline-flex rounded-xl border border-zinc-700 p-0.5" role="group" aria-label="Kiểu xem lịch xếp ca">
          {[["roster", "Theo nhân viên"], ["hours", "Theo giờ"]].map(([key, label]) => (
            <button key={key} type="button" aria-pressed={view === key} onClick={() => setView(key)}
              className={`rounded-lg px-2.5 py-1 text-xs ${view === key ? "bg-amber-400 font-semibold text-zinc-950" : "text-zinc-400 hover:text-zinc-200"}`}>{label}</button>
          ))}
        </div>
        {canManage && view === "roster" && <AddEmployee existing={rows} onAdd={onAddRow} />}
      </div>
    )}>
      {view === "hours" && (
        <>
          <p className="mb-3 text-xs text-zinc-400">Mỗi khối là một ca, xếp theo giờ để thấy khung giờ nào đông / thiếu người. Bấm vào ca để sửa; muốn kéo thả hãy chuyển về “Theo nhân viên”.</p>
          <WeekTimeGrid days={data.days.map((d) => d.date)} minHour={7} maxHour={22}
            events={data.entries.filter((e) => e.storeId === storeId).map((e) => ({
              id: e.id, date: e.date, start: hhmm(e.startTime), end: hhmm(e.endTime), title: e.employeeName,
              subtitle: `${e.shiftName}${e.status === "DRAFT" ? " · nháp" : ""}`,
              meta: ACTUAL_STATUS[e.actualStatus] && e.actualStatus !== "UPCOMING" ? ACTUAL_STATUS[e.actualStatus].label : "",
              className: `${SHIFT_COLORS[e.color] || SHIFT_COLORS.zinc} ${e.status === "DRAFT" ? "shift-chip--draft" : ""}`,
              onClick: () => onOpen({ type: "entry", entry: e, userId: e.userId, date: e.date }),
            }))} />
        </>
      )}
      {view === "roster" && canManage && (
        <div className="mb-4 flex flex-wrap items-center gap-2">
          <span className="text-xs text-zinc-400">Kéo ca vào ô để xếp:</span>
          {data.templates.map((t) => (
            <span key={t.id} draggable onDragStart={(ev) => { drag.current = { kind: "template", id: t.id }; ev.dataTransfer.effectAllowed = "copy"; ev.dataTransfer.setData("text/plain", t.name); }}
              onDragEnd={() => { drag.current = null; setOver(""); }}
              className={`inline-flex cursor-grab select-none items-center gap-1.5 rounded-lg border px-2.5 py-1 text-xs active:cursor-grabbing ${SHIFT_COLORS[t.color] || SHIFT_COLORS.zinc}`}
              title={`${t.name} ${timeRange(t.startTime, t.endTime)}${t.breakMinutes ? `, nghỉ ${t.breakMinutes}p` : ""}`}>
              <GripVertical size={12} className="opacity-60" />{t.name}<span className="opacity-70">{timeRange(t.startTime, t.endTime)}</span>
            </span>
          ))}
          {!data.templates.length && <span className="text-xs text-zinc-500">Chưa có ca mẫu — bấm “Ca mẫu” để tạo.</span>}
        </div>
      )}
      {view === "roster" && (!rows.length ? <Empty>Cửa hàng chưa có nhân viên.</Empty> : (
        <div className="-mx-5 overflow-x-auto px-5">
          <table data-table-mode="scroll" className="w-full min-w-[960px] table-fixed border-separate border-spacing-0 text-sm">
            <thead>
              <tr>
                <th className="sticky left-0 z-10 w-48 border-b border-zinc-800 bg-zinc-900 py-2 pr-3 text-left text-xs font-medium text-zinc-400">Nhân viên</th>
                {data.days.map((d) => {
                  const { weekday, date } = dayLabel(d.date);
                  return (
                    <th key={d.date} className={`border-b border-zinc-800 px-1.5 py-2 text-left text-xs font-medium ${columnTone(d)} ${d.date === today ? "text-amber-300" : "text-zinc-400"}`}>
                      <div className="flex items-baseline gap-1.5"><span className="font-semibold">{weekday}</span><span className="tabular-nums">{date}</span>
                        {d.date === today && <span className="rounded bg-amber-400 px-1 text-[10px] font-semibold uppercase text-zinc-950">Hôm nay</span>}</div>
                      <div className="mt-0.5 truncate font-normal text-zinc-500">
                        {d.holiday ? <span className="text-rose-300">{d.holiday}</span> : d.weeklyOff ? "Nghỉ tuần" : `${d.shiftCount} ca · ${hoursText(d.paidMinutes)}`}
                      </div>
                    </th>
                  );
                })}
              </tr>
            </thead>
            <tbody>
              {rows.map((emp) => {
                const overLimit = emp.scheduledMinutes > limit;
                return (
                  <tr key={emp.id}>
                    <td className="sticky left-0 z-10 border-b border-zinc-800 bg-zinc-900 py-2 pr-3 align-top">
                      <p className="truncate font-medium text-zinc-100">{emp.fullName}</p>
                      <p className="truncate text-xs text-zinc-500">{emp.employeeCode}{!emp.storeMember && " · Global"}</p>
                      <p className={`mt-0.5 flex items-center gap-1 text-xs tabular-nums ${overLimit ? "text-rose-300" : "text-zinc-400"}`}>
                        {overLimit && <AlertTriangle size={12} />}{hoursText(emp.scheduledMinutes)} / tuần
                      </p>
                    </td>
                    {data.days.map((d) => {
                      const cellKey = index.key(emp.id, d.date);
                      const entry = index.entries.get(cellKey);
                      const availability = index.availability.get(cellKey);
                      const leave = index.leaves.get(cellKey);
                      const past = d.date < today;
                      const canAdd = canManage && !past && !entry && !(leave && !leave.halfDay);
                      return (
                        <td key={d.date}
                          onDragOver={(ev) => { if (droppable(emp.id, d.date)) { ev.preventDefault(); setOver(cellKey); } }}
                          onDragLeave={() => setOver((k) => (k === cellKey ? "" : k))}
                          onDrop={(ev) => { ev.preventDefault(); drop(emp.id, d.date); }}
                          className={`group/cell h-[4.5rem] border-b border-l border-zinc-800 p-1 align-top ${past ? "bg-zinc-950/40" : columnTone(d)} ${over === cellKey ? "bg-amber-400/10 ring-1 ring-inset ring-amber-400/60" : ""}`}>
                          <div className="flex h-full flex-col gap-1">
                            {entry && (
                              <ShiftChip entry={entry} foreign={entry.storeId !== storeId} draggable={canManage && entry.editable}
                                onDragStart={() => { drag.current = { kind: "entry", id: entry.id, entry }; }}
                                onDragEnd={() => { drag.current = null; setOver(""); }}
                                onClick={() => entry.storeId === storeId && onOpen({ type: "entry", entry, userId: emp.id, date: d.date })} />
                            )}
                            {leave && <span className="rounded-md bg-violet-500/15 px-1.5 py-0.5 text-[11px] text-violet-200">{leave.halfDay ? "½ " : ""}{leave.leaveType}</span>}
                            {!entry && availability && (
                              <span className={`rounded-md px-1.5 py-0.5 text-[11px] ${availability.kind === "AVAILABLE" ? "bg-emerald-500/10 text-emerald-300" : "bg-rose-500/10 text-rose-300"}`}
                                title={availability.note || ""}>
                                {availability.kind === "AVAILABLE" ? `Rảnh${availability.preferredShiftName ? ` · ${availability.preferredShiftName}` : ""}` : "Bận"}
                              </span>
                            )}
                            {entry && availability?.kind === "UNAVAILABLE" && (
                              <span className="flex items-center gap-1 text-[11px] text-rose-300" title="Nhân viên đã đăng ký bận ngày này"><AlertTriangle size={11} />Đã báo bận</span>
                            )}
                            {canAdd && (
                              <button type="button" onClick={() => onOpen({ type: "entry", userId: emp.id, date: d.date })}
                                className="flex min-h-[1.75rem] flex-1 items-center justify-center gap-1 rounded-md border border-dashed border-zinc-700 text-xs text-zinc-500 opacity-0 transition-opacity hover:border-amber-400/60 hover:text-amber-300 focus-visible:opacity-100 group-hover/cell:opacity-100 [@media(hover:none)]:opacity-60"
                                aria-label={`Xếp ca cho ${emp.fullName} ngày ${dayLabel(d.date).date}`}>
                                <Plus size={13} />Xếp ca
                              </button>
                            )}
                          </div>
                        </td>
                      );
                    })}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ))}
      {view === "roster" && <Legend />}
    </Section>
  );
}

function ShiftChip({ entry, foreign, draggable, onDragStart, onDragEnd, onClick }) {
  const draft = entry.status === "DRAFT";
  const actual = ACTUAL_STATUS[entry.actualStatus];
  const showActual = entry.actualStatus && !["UPCOMING"].includes(entry.actualStatus);
  return (
    <button type="button" draggable={draggable} onDragStart={(ev) => { ev.dataTransfer.effectAllowed = "move"; ev.dataTransfer.setData("text/plain", entry.shiftName); onDragStart(); }}
      onDragEnd={onDragEnd} onClick={onClick} disabled={foreign}
      title={`${entry.shiftName} ${timeRange(entry.startTime, entry.endTime)}${entry.note ? ` — ${entry.note}` : ""}${foreign ? ` (tại ${entry.storeName})` : ""}`}
      className={`w-full rounded-lg border px-1.5 py-1 text-left text-xs ${SHIFT_COLORS[entry.color] || SHIFT_COLORS.zinc} ${draft ? "shift-chip--draft" : ""} ${foreign ? "cursor-default opacity-50" : draggable ? "cursor-grab active:cursor-grabbing" : ""}`}>
      <span className="flex items-center justify-between gap-1">
        <span className="truncate font-semibold">{foreign ? entry.storeName : entry.shiftName}</span>
        {entry.swapPending && <Repeat size={11} className="shrink-0" aria-label="Đang có yêu cầu đổi ca" />}
      </span>
      <span className="block tabular-nums opacity-80">{timeRange(entry.startTime, entry.endTime)}</span>
      <span className="mt-0.5 flex flex-wrap gap-1">
        {draft && <span className="shift-tag uppercase tracking-wide">Nháp</span>}
        {showActual && <span className={`shift-tag ${toneTag(actual?.tone)}`}>{actual?.label}{entry.lateMinutes > 0 ? ` ${entry.lateMinutes}p` : ""}</span>}
      </span>
    </button>
  );
}

const toneTag = (tone) => ({ emerald: "shift-tag--ok", amber: "shift-tag--warn", rose: "shift-tag--bad", sky: "shift-tag--info", violet: "shift-tag--info" }[tone] || "");

function Legend() {
  return (
    <div className="mt-4 flex flex-wrap gap-x-4 gap-y-1 text-xs text-zinc-500">
      <span><span className="shift-chip shift-chip--zinc shift-chip--draft mr-1 inline-block h-2.5 w-4 rounded align-middle" />Ca nháp (chưa công bố)</span>
      <span><span className="shift-chip shift-chip--zinc mr-1 inline-block h-2.5 w-4 rounded align-middle" />Đã công bố</span>
      <span className="text-emerald-300">Rảnh / Bận: nhân viên tự đăng ký</span>
      <span>Giới hạn cảnh báo: 48 giờ / tuần</span>
    </div>
  );
}

/** Thêm nhân viên Global (không thuộc cửa hàng) vào lịch tuần. */
function AddEmployee({ existing, onAdd }) {
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState("");
  const { data } = useLoader((signal) => (open ? attendanceApi.employees({ keyword }, { signal }) : Promise.resolve([])), [open, keyword]);
  const options = (data || []).filter((e) => !e.storeId && !existing.some((x) => x.id === e.id));
  if (!open) return <button type="button" className={btnGhost} onClick={() => setOpen(true)}><UserPlus size={15} />Thêm nhân viên Global</button>;
  return (
    <div className="flex flex-wrap items-center gap-2">
      <input className={`${control} w-48`} placeholder="Tìm tên / mã…" value={keyword} onChange={(e) => setKeyword(e.target.value)} autoFocus />
      <select className={`${control} w-56`} value="" onChange={(e) => { const emp = options.find((o) => o.id === e.target.value); if (emp) { onAdd(emp); setOpen(false); } }}>
        <option value="">{options.length ? "Chọn nhân viên Global…" : "Không có nhân viên Global phù hợp"}</option>
        {options.map((o) => <option key={o.id} value={o.id}>{o.fullName} ({o.employeeCode})</option>)}
      </select>
      <button type="button" className={btnGhost} onClick={() => setOpen(false)}>Đóng</button>
    </div>
  );
}

// ------------------------------------------------------------------ hộp thoại xếp / sửa ca

function EntryDialog({ grid, storeId, entry, userId, date, canManage, onClose, onSaved }) {
  const employee = grid.employees.find((e) => e.id === (entry?.userId || userId));
  const editable = canManage && (!entry || entry.editable);
  const [form, setForm] = useState(() => ({
    mode: entry && !entry.shiftId ? "custom" : "template",
    shiftId: entry?.shiftId || grid.templates[0]?.id || "",
    startTime: hhmm(entry?.startTime) || "08:00",
    endTime: hhmm(entry?.endTime) || "17:00",
    breakMinutes: entry?.breakMinutes ?? 0,
    date: entry?.date || date,
    note: entry?.note || "",
  }));
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const availability = grid.availability.find((a) => a.userId === (entry?.userId || userId) && a.date === form.date);

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    const body = {
      userId: entry?.userId || userId, storeId, date: form.date, note: form.note.trim() || null,
      ...(form.mode === "template"
        ? { shiftId: form.shiftId }
        : { startTime: toApiTime(form.startTime), endTime: toApiTime(form.endTime), breakMinutes: Number(form.breakMinutes) || 0 }),
    };
    try {
      if (entry) await shiftApi.updateEntry(entry.id, body);
      else await shiftApi.createEntry(body);
      onSaved();
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  const remove = async () => {
    if (!window.confirm(entry.status === "PUBLISHED" ? "Xóa ca đã công bố? Nhân viên sẽ nhận thông báo hủy ca." : "Xóa ca nháp này?")) return;
    setSaving(true);
    try {
      await shiftApi.deleteEntry(entry.id);
      onSaved();
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={entry ? "Ca làm" : "Xếp ca"} description={`${employee?.fullName || ""} · ${dayLabel(form.date).weekday} ${dayLabel(form.date).date}`} onClose={onClose} />
        <AdminDialogBody className="space-y-4">
          {entry && (
            <div className="flex flex-wrap gap-2">
              <Badge tone={entry.status === "DRAFT" ? "amber" : "emerald"}>{entry.status === "DRAFT" ? "Nháp" : "Đã công bố"}</Badge>
              {entry.actualStatus && <Badge map={ACTUAL_STATUS} value={entry.actualStatus} />}
              {entry.swapPending && <Badge tone="sky">Đang có yêu cầu đổi ca</Badge>}
            </div>
          )}
          {entry && !entry.editable && (
            <p className="text-sm text-zinc-400">Ca đã qua hoặc nhân viên đã chấm công nên không sửa được.
              {entry.checkIn && ` Vào ${entry.checkIn.slice(11, 16)}`}{entry.checkOut && `, ra ${entry.checkOut.slice(11, 16)}`}.</p>
          )}
          {availability && (
            <p className={`text-sm ${availability.kind === "AVAILABLE" ? "text-emerald-300" : "text-rose-300"}`}>
              Nhân viên đăng ký {availability.kind === "AVAILABLE" ? `rảnh${availability.preferredShiftName ? ` (muốn ${availability.preferredShiftName})` : ""}` : "bận"} ngày này{availability.note ? `: ${availability.note}` : ""}.
            </p>
          )}
          <fieldset disabled={!editable} className="space-y-4">
            <div className="flex gap-2">
              {[["template", "Ca mẫu"], ["custom", "Giờ tùy chỉnh"]].map(([value, label]) => (
                <button key={value} type="button" onClick={() => setForm({ ...form, mode: value })}
                  className={`rounded-xl border px-3 py-1.5 text-sm ${form.mode === value ? "border-amber-400 bg-amber-400/10 text-amber-300" : "border-zinc-700 text-zinc-300"}`}>{label}</button>
              ))}
            </div>
            {form.mode === "template" ? (
              <div className="grid gap-2 sm:grid-cols-2">
                {grid.templates.map((t) => (
                  <label key={t.id} className={`flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-2 text-sm ${form.shiftId === t.id ? "border-amber-400 bg-amber-400/5" : "border-zinc-700"}`}>
                    <input type="radio" name="shift" checked={form.shiftId === t.id} onChange={() => setForm({ ...form, shiftId: t.id })} />
                    <span className={`h-2.5 w-2.5 rounded-full ${SHIFT_DOT[t.color] || SHIFT_DOT.zinc}`} />
                    <span className="flex-1"><span className="font-medium text-zinc-100">{t.name}</span>
                      <span className="block text-xs text-zinc-400">{timeRange(t.startTime, t.endTime)} · {hoursText(t.paidMinutes)}</span></span>
                  </label>
                ))}
                {!grid.templates.length && <p className="text-sm text-zinc-500">Chưa có ca mẫu, dùng giờ tùy chỉnh.</p>}
              </div>
            ) : (
              <div className="grid gap-3 sm:grid-cols-3">
                <Field label="Bắt đầu"><input type="time" className={control} value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} required /></Field>
                <Field label="Kết thúc" hint="Nhỏ hơn giờ bắt đầu = ca qua đêm"><input type="time" className={control} value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} required /></Field>
                <Field label="Nghỉ (phút)"><input type="number" min="0" max="240" className={control} value={form.breakMinutes} onChange={(e) => setForm({ ...form, breakMinutes: e.target.value })} /></Field>
              </div>
            )}
            {entry && (
              <Field label="Ngày làm" hint="Đổi ngày để chuyển ca (hoặc kéo thả trên lịch)">
                <input type="date" className={control} value={form.date} min={isoDate()} onChange={(e) => setForm({ ...form, date: e.target.value })} required />
              </Field>
            )}
            <Field label="Ghi chú"><input className={control} maxLength={500} value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} placeholder="Ví dụ: kiểm kho, trực quầy thu ngân" /></Field>
          </fieldset>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          {entry && editable && <button type="button" className={`${btnDanger} mr-auto`} onClick={remove} disabled={saving}><Trash2 size={15} />Xóa ca</button>}
          <button type="button" className={btnGhost} onClick={onClose}>Đóng</button>
          {editable && (
            <button type="submit" className={btnPrimary} disabled={saving || (form.mode === "template" && !form.shiftId)}>
              {saving ? "Đang lưu…" : entry ? "Lưu thay đổi" : "Xếp ca (nháp)"}
            </button>
          )}
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}

// ------------------------------------------------------------------ ca mẫu

const EMPTY_TEMPLATE = { code: "", name: "", shiftType: "MORNING", startTime: "08:00", endTime: "14:00", breakMinutes: 0, color: "sky", sortOrder: 0, active: true };

function TemplatesDialog({ storeId, onClose }) {
  const { scopeStoreId } = useAttendance();
  const { data, error, loading, reload } = useLoader((signal) => shiftApi.templates({ storeId, includeInactive: true }, { signal }), [storeId]);
  const [editing, setEditing] = useState(null);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);
  const canEditGlobal = !scopeStoreId;

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    setFormError("");
    const body = {
      ...editing, storeId: editing.scope === "GLOBAL" ? null : storeId, startTime: toApiTime(editing.startTime), endTime: toApiTime(editing.endTime),
      breakMinutes: Number(editing.breakMinutes) || 0, sortOrder: Number(editing.sortOrder) || 0,
    };
    try {
      if (editing.id) await shiftApi.updateTemplate(editing.id, body);
      else await shiftApi.createTemplate(body);
      setEditing(null);
      reload();
    } catch (e) {
      setFormError(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <AdminDialog onClose={onClose} size="lg">
      <AdminDialogHeader title="Ca mẫu" description="Ca dùng chung toàn chuỗi và ca riêng của cửa hàng. Sửa ca mẫu không làm đổi các ca đã xếp." onClose={onClose} />
      <AdminDialogBody className="space-y-4">
        <ErrorText>{error}</ErrorText>
        {loading && !data ? <Loading /> : (
          <ul className="divide-y divide-zinc-800 rounded-2xl border border-zinc-800">
            {(data || []).map((t) => {
              const editableRow = t.storeId ? true : canEditGlobal;
              return (
                <li key={t.id} className={`flex flex-wrap items-center gap-3 px-3 py-2.5 text-sm ${t.active ? "" : "opacity-50"}`}>
                  <span className={`h-3 w-3 rounded-full ${SHIFT_DOT[t.color] || SHIFT_DOT.zinc}`} />
                  <span className="min-w-[8rem] flex-1"><span className="font-medium text-zinc-100">{t.name}</span> <span className="text-xs text-zinc-500">{t.code}</span>
                    <span className="block text-xs text-zinc-400">{timeRange(t.startTime, t.endTime)} · nghỉ {t.breakMinutes}p · {hoursText(t.paidMinutes)} công</span></span>
                  <Badge tone={t.storeId ? "sky" : "zinc"}>{t.storeId ? "Cửa hàng" : "Toàn chuỗi"}</Badge>
                  {!t.active && <Badge tone="zinc">Ngừng dùng</Badge>}
                  {editableRow && (
                    <button type="button" className={btnGhost} onClick={() => { setFormError(""); setEditing({ ...t, scope: t.storeId ? "STORE" : "GLOBAL", startTime: hhmm(t.startTime), endTime: hhmm(t.endTime) }); }}>Sửa</button>
                  )}
                </li>
              );
            })}
            {!data?.length && <li className="px-3 py-6 text-center text-sm text-zinc-500">Chưa có ca mẫu.</li>}
          </ul>
        )}
        {editing ? (
          <form onSubmit={save} className="space-y-3 rounded-2xl border border-zinc-800 p-4">
            <p className="text-sm font-semibold text-zinc-200">{editing.id ? `Sửa ${editing.name}` : "Ca mẫu mới"}</p>
            <div className="grid gap-3 sm:grid-cols-3">
              <Field label="Mã ca" required><input className={control} value={editing.code} maxLength={30} onChange={(e) => setEditing({ ...editing, code: e.target.value })} required /></Field>
              <Field label="Tên ca" required><input className={control} value={editing.name} maxLength={100} onChange={(e) => setEditing({ ...editing, name: e.target.value })} required /></Field>
              <Field label="Loại ca">
                <select className={control} value={editing.shiftType} onChange={(e) => setEditing({ ...editing, shiftType: e.target.value })}>
                  {SHIFT_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                </select>
              </Field>
              <Field label="Bắt đầu"><input type="time" className={control} value={editing.startTime} onChange={(e) => setEditing({ ...editing, startTime: e.target.value })} required /></Field>
              <Field label="Kết thúc"><input type="time" className={control} value={editing.endTime} onChange={(e) => setEditing({ ...editing, endTime: e.target.value })} required /></Field>
              <Field label="Nghỉ giữa ca (phút)"><input type="number" min="0" max="240" className={control} value={editing.breakMinutes} onChange={(e) => setEditing({ ...editing, breakMinutes: e.target.value })} /></Field>
              <Field label="Thứ tự"><input type="number" min="0" max="999" className={control} value={editing.sortOrder} onChange={(e) => setEditing({ ...editing, sortOrder: e.target.value })} /></Field>
              {!editing.id && canEditGlobal && (
                <Field label="Phạm vi">
                  <select className={control} value={editing.scope} onChange={(e) => setEditing({ ...editing, scope: e.target.value })}>
                    <option value="STORE">Riêng cửa hàng này</option>
                    <option value="GLOBAL">Toàn chuỗi</option>
                  </select>
                </Field>
              )}
              <label className="flex items-center gap-2 self-end pb-2 text-sm text-zinc-300">
                <input type="checkbox" checked={editing.active} onChange={(e) => setEditing({ ...editing, active: e.target.checked })} />Đang sử dụng
              </label>
            </div>
            <div>
              <p className="mb-1.5 text-xs font-medium text-zinc-400">Màu</p>
              <div className="flex flex-wrap gap-2">
                {COLOR_OPTIONS.map((c) => (
                  <button key={c} type="button" onClick={() => setEditing({ ...editing, color: c })} aria-label={c}
                    className={`h-7 w-7 rounded-full ${SHIFT_DOT[c]} ${editing.color === c ? "ring-2 ring-white ring-offset-2 ring-offset-zinc-900" : "opacity-70"}`} />
                ))}
              </div>
            </div>
            <ErrorText>{formError}</ErrorText>
            <div className="flex justify-end gap-2">
              <button type="button" className={btnGhost} onClick={() => setEditing(null)}>Hủy</button>
              <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Lưu ca mẫu"}</button>
            </div>
          </form>
        ) : (
          <button type="button" className={btnGhost} onClick={() => { setFormError(""); setEditing({ ...EMPTY_TEMPLATE, scope: "STORE" }); }}><Plus size={15} />Thêm ca mẫu</button>
        )}
      </AdminDialogBody>
      <AdminDialogFooter><button type="button" className={btnPrimary} onClick={onClose}><CalendarRange size={15} />Xong</button></AdminDialogFooter>
    </AdminDialog>
  );
}
