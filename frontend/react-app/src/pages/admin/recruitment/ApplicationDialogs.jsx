import { useState } from "react";
import { Link } from "react-router-dom";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { useDebouncedValue } from "../../../hooks/useDebouncedValue";
import {
  ErrorText, Field, UserPicker, btnGhost, btnPrimary, control, textarea, useLoader, useRecruitment,
} from "../../../components/admin/recruitment/RecruitmentUi";
import {
  INTERVIEW_MODES, candidatePayload, emptyCandidateForm, hireDefaults, money, validateCandidate, validateFile,
} from "./recruitmentLogic";

/** Thêm / sửa ứng viên; cảnh báo trùng email / số điện thoại trước khi lưu. Tạo mới có thể kèm CV và vị trí ứng tuyển. */
export function CandidateDialog({ candidateId, initial, defaultPositionId, onClose, onSaved }) {
  const { lookups } = useRecruitment();
  const [form, setForm] = useState(initial || { ...emptyCandidateForm(), positionIds: defaultPositionId ? [defaultPositionId] : [] });
  const [cv, setCv] = useState(null);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [confirmDuplicate, setConfirmDuplicate] = useState(false);
  const email = useDebouncedValue(form.email, 400);
  const phone = useDebouncedValue(form.phone, 400);
  const duplicates = useLoader((signal) => ((email.trim() || phone.trim().length >= 9)
    ? recruitmentApi.duplicates({ email: email.trim(), phone: phone.trim(), excludeId: candidateId }, { signal })
    : Promise.resolve([])), [email, phone]);
  const positions = useLoader((signal) => (candidateId ? Promise.resolve([]) : recruitmentApi.positions({ openOnly: true }, { signal })), [candidateId]);
  const set = (patch) => setForm((f) => ({ ...f, ...patch }));
  const matches = duplicates.data || [];

  const submit = async (event) => {
    event.preventDefault();
    const problem = validateCandidate(form) || (cv ? validateFile(cv) : "");
    if (problem) return setError(problem);
    if (!candidateId && matches.length && !confirmDuplicate) return setError("Có ứng viên trùng thông tin. Mở hồ sơ cũ hoặc tick \"Vẫn tạo mới\".");
    setSaving(true);
    try {
      let saved = candidateId
        ? await recruitmentApi.updateCandidate(candidateId, candidatePayload(form))
        : await recruitmentApi.createCandidate(candidatePayload(form, { ignoreDuplicates: confirmDuplicate }));
      if (cv) {
        await recruitmentApi.uploadDocument(saved.id, cv, { docType: "CV" });
        saved = await recruitmentApi.candidate(saved.id);
      }
      onSaved(saved);
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <AdminDialog size="xl" onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={candidateId ? "Sửa hồ sơ ứng viên" : "Thêm ứng viên"} onClose={onClose} />
        <AdminDialogBody className="grid gap-4 md:grid-cols-3">
          <Field label="Họ tên" required><input className={control} value={form.fullName} maxLength={200} onChange={(e) => set({ fullName: e.target.value })} autoFocus /></Field>
          <Field label="Email"><input type="email" className={control} value={form.email} onChange={(e) => { set({ email: e.target.value }); setConfirmDuplicate(false); }} /></Field>
          <Field label="Số điện thoại"><input className={control} value={form.phone} onChange={(e) => { set({ phone: e.target.value }); setConfirmDuplicate(false); }} /></Field>
          {matches.length > 0 && (
            <div className="rounded-2xl border border-amber-500/40 bg-amber-500/10 p-3 text-sm text-amber-100 md:col-span-3">
              <p className="font-semibold">Có thể trùng với ứng viên đã có:</p>
              <ul className="mt-1 space-y-1">
                {matches.map((m) => (
                  <li key={m.id}>
                    <Link to={`/admin/recruitment/candidates/${m.id}`} className="underline" onClick={onClose}>{m.code} – {m.fullName}</Link>
                    <span className="text-xs text-amber-200/80"> · trùng {m.matchedOn.map((x) => (x === "EMAIL" ? "email" : "SĐT")).join(", ")} · {m.applicationCount} hồ sơ</span>
                  </li>
                ))}
              </ul>
              {!candidateId && (
                <label className="mt-2 flex items-center gap-2 text-xs"><input type="checkbox" checked={confirmDuplicate} onChange={(e) => setConfirmDuplicate(e.target.checked)} />Vẫn tạo ứng viên mới</label>
              )}
            </div>
          )}
          <Field label="Ngày sinh"><input type="date" className={control} value={form.dateOfBirth} onChange={(e) => set({ dateOfBirth: e.target.value })} /></Field>
          <Field label="Giới tính">
            <select className={control} value={form.gender} onChange={(e) => set({ gender: e.target.value })}>
              <option value="">—</option><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option>
            </select>
          </Field>
          <Field label="Địa chỉ"><input className={control} value={form.address} onChange={(e) => set({ address: e.target.value })} /></Field>
          <Field label="Chức danh hiện tại"><input className={control} value={form.currentTitle} onChange={(e) => set({ currentTitle: e.target.value })} /></Field>
          <Field label="Công ty hiện tại"><input className={control} value={form.currentCompany} onChange={(e) => set({ currentCompany: e.target.value })} /></Field>
          <Field label="Số năm kinh nghiệm"><input type="number" min="0" step="0.5" className={control} value={form.yearsExperience} onChange={(e) => set({ yearsExperience: e.target.value })} /></Field>
          <Field label="Kỹ năng" hint="Ngăn cách bằng dấu phẩy" className="md:col-span-2"><input className={control} value={form.skills} onChange={(e) => set({ skills: e.target.value })} /></Field>
          <Field label="Học vấn"><input className={control} value={form.education} onChange={(e) => set({ education: e.target.value })} /></Field>
          <Field label="Nguồn ứng tuyển">
            <select className={control} value={form.source} onChange={(e) => set({ source: e.target.value })}>
              {lookups?.sources?.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          </Field>
          <Field label="Chi tiết nguồn" hint="Ví dụ: người giới thiệu"><input className={control} value={form.sourceDetail} onChange={(e) => set({ sourceDetail: e.target.value })} /></Field>
          <Field label="Thẻ phân loại" hint="Ngăn cách bằng dấu phẩy"><input className={control} value={form.tags} onChange={(e) => set({ tags: e.target.value })} /></Field>
          <Field label="LinkedIn"><input className={control} value={form.linkedinUrl} onChange={(e) => set({ linkedinUrl: e.target.value })} /></Field>
          <Field label="Portfolio"><input className={control} value={form.portfolioUrl} onChange={(e) => set({ portfolioUrl: e.target.value })} /></Field>
          {!candidateId && (
            <Field label="CV" hint="PDF, DOC, DOCX, JPG, PNG · tối đa 10MB">
              <input type="file" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" className="block w-full text-sm text-zinc-300 file:mr-2 file:rounded-lg file:border-0 file:bg-zinc-800 file:px-3 file:py-2 file:text-zinc-100"
                onChange={(e) => setCv(e.target.files?.[0] || null)} />
            </Field>
          )}
          <Field label="Tóm tắt" className="md:col-span-3"><textarea className={textarea} rows={2} value={form.summary} onChange={(e) => set({ summary: e.target.value })} /></Field>
          <Field label="Ghi chú nội bộ" className="md:col-span-3"><textarea className={textarea} rows={2} value={form.notes} onChange={(e) => set({ notes: e.target.value })} /></Field>
          {!candidateId && (
            <div className="space-y-2 md:col-span-3">
              <p className="text-xs font-medium text-zinc-400">Ứng tuyển vào vị trí (đang mở)</p>
              <div className="flex flex-wrap gap-2">
                {(positions.data || []).length === 0 && <span className="text-xs text-zinc-500">Không có vị trí đang mở bạn quản lý</span>}
                {(positions.data || []).map((p) => {
                  const checked = form.positionIds.includes(p.id);
                  return (
                    <label key={p.id} className={`flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-1.5 text-xs ${checked ? "border-amber-400 text-amber-200" : "border-zinc-700 text-zinc-300"}`}>
                      <input type="checkbox" checked={checked} onChange={() => set({ positionIds: checked ? form.positionIds.filter((x) => x !== p.id) : [...form.positionIds, p.id] })} />
                      {p.title} <span className="text-zinc-500">· {p.campaignName}</span>
                    </label>
                  );
                })}
              </div>
            </div>
          )}
          <div className="md:col-span-3"><ErrorText>{error}</ErrorText></div>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Lưu"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}

/** Thêm hồ sơ vào vị trí: chọn ứng viên có sẵn hoặc tạo mới. */
export function AddApplicationDialog({ positionId, candidateId, onClose, onSaved }) {
  const { lookups } = useRecruitment();
  const [mode, setMode] = useState(candidateId ? "existing" : "pick");
  const [keyword, setKeyword] = useState("");
  const [selected, setSelected] = useState(candidateId || "");
  const [chosenPosition, setChosenPosition] = useState(positionId || "");
  const [source, setSource] = useState("OTHER");
  const [notify, setNotify] = useState(false);
  const [error, setError] = useState("");
  const kw = useDebouncedValue(keyword, 300);
  const candidates = useLoader((signal) => (mode === "pick" ? recruitmentApi.candidates({ keyword: kw, size: 10 }, { signal }) : Promise.resolve(null)), [kw, mode]);
  const positions = useLoader((signal) => (positionId ? Promise.resolve([]) : recruitmentApi.positions({ openOnly: true }, { signal })), [positionId]);

  if (mode === "new") {
    return <CandidateDialog defaultPositionId={positionId} onClose={onClose} onSaved={onSaved} />;
  }
  const submit = async () => {
    try {
      const saved = await recruitmentApi.createApplication({ candidateId: selected, positionId: chosenPosition, source, notifyCandidate: notify });
      onSaved(saved);
    } catch (e) {
      setError(e.message);
    }
  };
  return (
    <AdminDialog size="lg" onClose={onClose}>
      <AdminDialogHeader title="Thêm hồ sơ ứng tuyển" onClose={onClose} />
      <AdminDialogBody className="space-y-4">
        {!positionId && (
          <Field label="Vị trí" required>
            <select className={control} value={chosenPosition} onChange={(e) => setChosenPosition(e.target.value)}>
              <option value="">Chọn vị trí đang mở…</option>
              {positions.data?.map((p) => <option key={p.id} value={p.id}>{p.title} · {p.campaignName}</option>)}
            </select>
          </Field>
        )}
        {!candidateId && (
          <>
            <div className="flex items-center gap-2">
              <input className={control} placeholder="Tìm ứng viên có sẵn (tên, email, SĐT)" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
              <button type="button" className={btnGhost} onClick={() => setMode("new")}>Ứng viên mới</button>
            </div>
            <div className="max-h-60 divide-y divide-zinc-800 overflow-y-auto rounded-xl border border-zinc-800">
              {candidates.data?.content?.length === 0 && <p className="p-3 text-sm text-zinc-500">Không tìm thấy – chọn "Ứng viên mới".</p>}
              {candidates.data?.content?.map((c) => (
                <label key={c.id} className={`flex cursor-pointer items-center gap-3 px-3 py-2 text-sm ${selected === c.id ? "bg-amber-400/10" : ""}`}>
                  <input type="radio" name="candidate" checked={selected === c.id} onChange={() => setSelected(c.id)} />
                  <span className="text-zinc-100">{c.fullName}</span>
                  <span className="text-xs text-zinc-500">{c.code} · {c.email || c.phone}</span>
                </label>
              ))}
            </div>
          </>
        )}
        <Field label="Nguồn">
          <select className={control} value={source} onChange={(e) => setSource(e.target.value)}>
            {lookups?.sources?.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
        </Field>
        <label className="flex items-center gap-2 text-sm text-zinc-300">
          <input type="checkbox" checked={notify} onChange={(e) => setNotify(e.target.checked)} />Gửi email xác nhận đã nhận hồ sơ cho ứng viên
        </label>
        <ErrorText>{error}</ErrorText>
      </AdminDialogBody>
      <AdminDialogFooter>
        <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
        <button type="button" className={btnPrimary} disabled={!selected || !chosenPosition} onClick={submit}>Thêm hồ sơ</button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}

const pad = (n) => String(n).padStart(2, "0");
const localInput = (date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;

/** Đặt / dời lịch phỏng vấn và phân công người phỏng vấn (tự thành người đánh giá của vòng). */
export function InterviewDialog({ application, stages, interview, storeId, onClose, onSaved }) {
  const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate() + 1); tomorrow.setHours(9, 0, 0, 0);
  const [form, setForm] = useState(() => interview ? {
    stageId: interview.stageId || "", title: interview.title, startsAt: interview.startsAt.slice(0, 16), endsAt: interview.endsAt.slice(0, 16),
    mode: interview.mode, location: interview.location || "", meetingUrl: interview.meetingUrl || "", note: interview.note || "",
    participantIds: interview.participants.map((u) => u.id), notifyCandidate: true,
  } : {
    stageId: application.currentStageId || "", title: `Phỏng vấn ${stages.find((s) => s.stage.id === application.currentStageId)?.stage.name || ""}`.trim(),
    startsAt: localInput(tomorrow), endsAt: localInput(new Date(tomorrow.getTime() + 3600_000)), mode: "ONSITE", location: "", meetingUrl: "", note: "", participantIds: [], notifyCandidate: true,
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const users = useLoader((signal) => recruitmentApi.users({ storeId: storeId || undefined }, { signal }), [storeId]);
  const set = (patch) => setForm((f) => ({ ...f, ...patch }));
  const submit = async (event) => {
    event.preventDefault();
    if (!form.participantIds.length) return setError("Chọn ít nhất một người phỏng vấn");
    if (form.endsAt <= form.startsAt) return setError("Giờ kết thúc phải sau giờ bắt đầu");
    setSaving(true);
    const body = { ...form, stageId: form.stageId || null, startsAt: `${form.startsAt}:00`, endsAt: `${form.endsAt}:00` };
    try {
      const saved = interview ? await recruitmentApi.updateInterview(interview.id, body) : await recruitmentApi.createInterview(application.id, body);
      onSaved(saved);
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  return (
    <AdminDialog size="lg" onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={interview ? "Sửa / dời lịch phỏng vấn" : "Đặt lịch phỏng vấn"} description={`${application.candidateName} · ${application.positionTitle}`} onClose={onClose} />
        <AdminDialogBody className="grid gap-4 md:grid-cols-2">
          <Field label="Tiêu đề" required className="md:col-span-2"><input className={control} value={form.title} onChange={(e) => set({ title: e.target.value })} /></Field>
          {!interview && (
            <Field label="Vòng">
              <select className={control} value={form.stageId} onChange={(e) => set({ stageId: e.target.value })}>
                {stages.filter((s) => s.status !== "NOT_REACHED").map((s) => <option key={s.stage.id} value={s.stage.id}>{s.stage.name}</option>)}
              </select>
            </Field>
          )}
          <Field label="Hình thức">
            <select className={control} value={form.mode} onChange={(e) => set({ mode: e.target.value })}>
              {INTERVIEW_MODES.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
            </select>
          </Field>
          <Field label="Bắt đầu" required><input type="datetime-local" className={control} value={form.startsAt} onChange={(e) => set({ startsAt: e.target.value })} /></Field>
          <Field label="Kết thúc" required><input type="datetime-local" className={control} value={form.endsAt} onChange={(e) => set({ endsAt: e.target.value })} /></Field>
          {form.mode === "ONSITE" && <Field label="Địa điểm" required className="md:col-span-2"><input className={control} value={form.location} onChange={(e) => set({ location: e.target.value })} /></Field>}
          {form.mode === "ONLINE" && <Field label="Link họp" required className="md:col-span-2"><input className={control} value={form.meetingUrl} onChange={(e) => set({ meetingUrl: e.target.value })} /></Field>}
          <Field label="Người phỏng vấn" required className="md:col-span-2" hint="Cần vai trò Người phỏng vấn / Hiring Manager; trùng lịch sẽ bị chặn">
            <UserPicker users={users.data || []} value={form.participantIds} onChange={(participantIds) => set({ participantIds })} />
          </Field>
          <Field label="Ghi chú nội bộ" className="md:col-span-2"><textarea className={textarea} rows={2} value={form.note} onChange={(e) => set({ note: e.target.value })} /></Field>
          <label className="flex items-center gap-2 text-sm text-zinc-300 md:col-span-2">
            <input type="checkbox" checked={form.notifyCandidate} onChange={(e) => set({ notifyCandidate: e.target.checked })} />
            {interview ? "Gửi email báo đổi lịch cho ứng viên (nếu đổi giờ)" : "Gửi thư mời phỏng vấn qua email cho ứng viên"}
          </label>
          <div className="md:col-span-2"><ErrorText>{error}</ErrorText></div>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Lưu lịch"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}

/** Tạo / sửa offer. */
export function OfferDialog({ application, offer, onClose, onSaved }) {
  const today = new Date();
  const plus = (days) => { const d = new Date(today); d.setDate(d.getDate() + days); return d.toISOString().slice(0, 10); };
  const [form, setForm] = useState(offer ? {
    jobTitle: offer.jobTitle, salary: String(Number(offer.salary)), startDate: offer.startDate, responseDeadline: offer.responseDeadline, note: offer.note || "",
  } : { jobTitle: application.positionTitle, salary: "", startDate: plus(14), responseDeadline: plus(3), note: "" });
  const [submit, setSubmit] = useState(true);
  const [error, setError] = useState("");
  const set = (patch) => setForm((f) => ({ ...f, ...patch }));
  const save = async (event) => {
    event.preventDefault();
    if (!(Number(form.salary) > 0)) return setError("Vui lòng nhập mức lương");
    const body = { ...form, salary: Number(form.salary), note: form.note.trim() || null, submitForApproval: submit };
    try {
      onSaved(offer ? await recruitmentApi.updateOffer(offer.id, body) : await recruitmentApi.createOffer(application.id, body));
    } catch (e) {
      setError(e.message);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={save}>
        <AdminDialogHeader title={offer ? "Sửa offer" : "Tạo offer"} description={application.candidateName} onClose={onClose} />
        <AdminDialogBody className="grid gap-4 sm:grid-cols-2">
          <Field label="Chức danh" required className="sm:col-span-2"><input className={control} value={form.jobTitle} onChange={(e) => set({ jobTitle: e.target.value })} /></Field>
          <Field label="Lương (₫/tháng)" required hint={money(form.salary)}><input type="number" min="0" step="100000" className={control} value={form.salary} onChange={(e) => set({ salary: e.target.value })} /></Field>
          <Field label="Ngày bắt đầu làm" required><input type="date" className={control} value={form.startDate} onChange={(e) => set({ startDate: e.target.value })} /></Field>
          <Field label="Hạn phản hồi" required><input type="date" className={control} value={form.responseDeadline} onChange={(e) => set({ responseDeadline: e.target.value })} /></Field>
          <Field label="Ghi chú" className="sm:col-span-2"><textarea className={textarea} rows={3} value={form.note} onChange={(e) => set({ note: e.target.value })} /></Field>
          {!offer && (
            <label className="flex items-center gap-2 text-sm text-zinc-300 sm:col-span-2">
              <input type="checkbox" checked={submit} onChange={(e) => setSubmit(e.target.checked)} />Gửi duyệt ngay (hiring manager / HR Admin)
            </label>
          )}
          <div className="sm:col-span-2"><ErrorText>{error}</ErrorText></div>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary}>Lưu offer</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}

/** Chuyển ứng viên đã nhận offer thành nhân viên (module Nhân sự). */
export function HireDialog({ application, onClose, onHired }) {
  const { lookups } = useRecruitment();
  const position = useLoader((signal) => recruitmentApi.position(application.positionId, { signal }), [application.positionId]);
  if (position.loading && !position.data) return null;
  return <HireForm key={position.data ? "p" : "x"} application={application} defaults={hireDefaults(application, position.data)}
    lookups={lookups} onClose={onClose} onHired={onHired} />;
}

function HireForm({ application, defaults, lookups, onClose, onHired }) {
  const [form, setForm] = useState(defaults);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);
  const set = (patch) => setForm((f) => ({ ...f, ...patch }));
  const submit = async (event) => {
    event.preventDefault();
    try {
      const hired = await recruitmentApi.hire(application.id, {
        ...form, username: form.username.trim() || null, departmentId: form.departmentId || null,
        orgPositionId: form.orgPositionId || null, storeId: form.storeId || null, sendWelcomeEmail: form.sendWelcomeEmail,
      });
      setResult(hired);
    } catch (e) {
      setError(e.message);
    }
  };
  if (result) {
    return (
      <AdminDialog onClose={() => onHired(result)}>
        <AdminDialogHeader title="Đã tạo tài khoản nhân viên" onClose={() => onHired(result)} />
        <AdminDialogBody className="space-y-2 text-sm text-zinc-200">
          <p>Mã nhân viên: <strong>{result.employeeCode}</strong></p>
          <p>Tên đăng nhập: <strong>{result.username}</strong></p>
          <p>Mật khẩu tạm (chỉ hiện một lần): <code className="rounded bg-zinc-800 px-2 py-0.5">{result.temporaryPassword}</code></p>
          <p className="text-zinc-400">Đã chuyển sang hồ sơ nhân viên: {result.transferred.join(", ")}.</p>
          <p className="text-zinc-400">{result.welcomeEmailQueued ? "Đã gửi email chào mừng (không kèm mật khẩu)." : "Không gửi email chào mừng."}</p>
        </AdminDialogBody>
        <AdminDialogFooter><button type="button" className={btnPrimary} onClick={() => onHired(result)}>Xong</button></AdminDialogFooter>
      </AdminDialog>
    );
  }
  const orgPositions = (lookups?.orgPositions || []).filter((p) => !form.departmentId || p.departmentId === form.departmentId);
  return (
    <AdminDialog size="lg" onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title="Chuyển thành nhân viên" description={`${application.candidateName} – ngày vào làm theo offer`} onClose={onClose} />
        <AdminDialogBody className="grid gap-4 sm:grid-cols-2">
          <Field label="Tên đăng nhập" hint="Để trống = mã nhân viên"><input className={control} value={form.username} onChange={(e) => set({ username: e.target.value })} /></Field>
          <Field label="Email" required><input type="email" className={control} value={form.email} onChange={(e) => set({ email: e.target.value })} /></Field>
          <Field label="Số điện thoại" required><input className={control} value={form.phone} onChange={(e) => set({ phone: e.target.value })} /></Field>
          <Field label="Loại hợp đồng">
            <select className={control} value={form.employmentType} onChange={(e) => set({ employmentType: e.target.value })}>
              {lookups?.employmentTypes?.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
          </Field>
          <Field label="Phòng ban" required>
            <select className={control} value={form.departmentId} onChange={(e) => set({ departmentId: e.target.value, orgPositionId: "" })}>
              <option value="">—</option>
              {lookups?.departments?.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
            </select>
          </Field>
          <Field label="Chức danh" required>
            <select className={control} value={form.orgPositionId} onChange={(e) => set({ orgPositionId: e.target.value })}>
              <option value="">—</option>
              {orgPositions.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </Field>
          <Field label="Vai trò">
            <select className={control} value={form.roleCodes[0]} onChange={(e) => set({ roleCodes: [e.target.value] })}>
              {lookups?.roles?.map((r) => <option key={r.code} value={r.code}>{r.name}</option>)}
            </select>
          </Field>
          <Field label="Cửa hàng">
            <select className={control} value={form.storeId} onChange={(e) => set({ storeId: e.target.value })}>
              <option value="">Văn phòng</option>
              {lookups?.stores?.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          </Field>
          <p className="text-xs text-zinc-500 sm:col-span-2">
            Ngày sinh, giới tính, nơi làm việc và quản lý trực tiếp (hiring manager) được lấy từ hồ sơ ứng viên / vị trí. CV vẫn lưu trong hồ sơ ứng viên.
          </p>
          <label className="flex items-center gap-2 text-sm text-zinc-300 sm:col-span-2">
            <input type="checkbox" checked={form.sendWelcomeEmail} onChange={(e) => set({ sendWelcomeEmail: e.target.checked })} />Gửi email chào mừng (mã nhân viên, tên đăng nhập, ngày bắt đầu)
          </label>
          <div className="sm:col-span-2"><ErrorText>{error}</ErrorText></div>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary}>Tạo nhân viên</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}

/** Phân công người đánh giá cho một vòng. */
export function EvaluatorsDialog({ applicationId, stage, current, storeId, onClose, onSaved }) {
  const users = useLoader((signal) => recruitmentApi.users({ storeId: storeId || undefined }, { signal }), [storeId]);
  const [value, setValue] = useState(current);
  const [error, setError] = useState("");
  const save = async () => {
    try {
      onSaved(await recruitmentApi.assignEvaluators(applicationId, stage.id, value));
    } catch (e) {
      setError(e.message);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <AdminDialogHeader title={`Người đánh giá – ${stage.name}`} description="Người đã nộp phiếu luôn được giữ lại. Người được chọn sẽ nhận thông báo." onClose={onClose} />
      <AdminDialogBody className="space-y-3">
        <UserPicker users={users.data || []} value={value} onChange={setValue} />
        <ErrorText>{error}</ErrorText>
      </AdminDialogBody>
      <AdminDialogFooter>
        <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
        <button type="button" className={btnPrimary} onClick={save}>Lưu</button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}
