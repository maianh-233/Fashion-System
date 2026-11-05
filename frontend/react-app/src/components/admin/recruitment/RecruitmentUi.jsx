/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { Check, Download, ExternalLink, FileText, Loader2, X } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../common/AdminDialog";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { TONES, canPreview, formatBytes, formatDateTime, labelOf, DOC_TYPES, validateFile } from "../../../pages/admin/recruitment/recruitmentLogic";

export const control =
  "h-10 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-amber-400 disabled:opacity-60";
export const textarea =
  "w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-zinc-100 outline-none focus:border-amber-400";
export const card = "rounded-3xl border border-zinc-800 bg-zinc-900";
export const btnPrimary =
  "inline-flex items-center justify-center gap-2 rounded-xl bg-amber-400 px-4 py-2.5 text-sm font-semibold text-zinc-950 hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60";
export const btnGhost =
  "inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-sm text-zinc-200 hover:border-zinc-500 hover:text-white disabled:cursor-not-allowed disabled:opacity-50";
export const btnDanger =
  "inline-flex items-center justify-center gap-2 rounded-xl border border-rose-500/40 px-3 py-2 text-sm text-rose-300 hover:bg-rose-500/10 disabled:opacity-50";

// ------------------------------------------------------------------ dữ liệu dùng chung (danh mục + quyền + phạm vi)

const LookupsContext = createContext(null);

export function RecruitmentLookupsProvider({ children }) {
  const [state, setState] = useState({ data: null, error: "" });
  useEffect(() => {
    const controller = new AbortController();
    recruitmentApi.lookups({ signal: controller.signal })
      .then((data) => setState({ data, error: "" }))
      .catch((e) => { if (!controller.signal.aborted) setState({ data: null, error: e.message }); });
    return () => controller.abort();
  }, []);
  return <LookupsContext.Provider value={state}>{children}</LookupsContext.Provider>;
}

/** Danh mục, quyền ({admin, manage, evaluate, approve}) và phạm vi cửa hàng của người đang đăng nhập. */
export function useRecruitment() {
  const state = useContext(LookupsContext);
  const data = state?.data;
  return useMemo(() => ({
    loading: !data && !state?.error,
    error: state?.error || "",
    lookups: data,
    can: data?.permissions || { admin: false, manage: false, evaluate: false, approve: false },
    scope: data?.scope || { storeId: null, storeName: null },
    users: data?.users || [],
    sourceLabel: (value) => data?.sources?.find((s) => s.value === value)?.label || value,
  }), [data, state?.error]);
}

/** Tải dữ liệu theo key; đổi key → tải lại, hủy request cũ. */
export function useLoader(load, deps) {
  const [version, setVersion] = useState(0);
  const key = JSON.stringify([...deps, version]);
  // Dữ liệu cũ vẫn hiện trong lúc tải lại; "loading" = kết quả hiện có không thuộc key hiện tại.
  const [state, setState] = useState({ key: null, data: null, error: "" });
  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal)
      .then((data) => setState({ key, data, error: "" }))
      .catch((e) => { if (!controller.signal.aborted) setState({ key, data: null, error: e.message }); });
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);
  return {
    data: state.data, error: state.error, loading: state.key !== key,
    reload: () => setVersion((v) => v + 1),
    setData: (data) => setState({ key, data, error: "" }),
  };
}

// ------------------------------------------------------------------ hiển thị

export function Badge({ map, value, children, tone }) {
  const cls = tone ? TONES[tone] : TONES[map?.[value]?.tone || "zinc"];
  return (
    <span className={`inline-flex items-center whitespace-nowrap rounded-full border px-2.5 py-0.5 text-xs font-semibold ${cls}`}>
      {children || map?.[value]?.label || value}
    </span>
  );
}

export function Section({ title, action, children, className = "" }) {
  return (
    <section className={`${card} p-5 ${className}`}>
      {(title || action) && (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          {title && <h2 className="text-base font-semibold text-zinc-100">{title}</h2>}
          {action}
        </div>
      )}
      {children}
    </section>
  );
}

export function Field({ label, hint, required, children, className = "" }) {
  return (
    <label className={`block space-y-1.5 ${className}`}>
      <span className="text-xs font-medium text-zinc-400">{label}{required && <span className="text-rose-400"> *</span>}</span>
      {children}
      {hint && <span className="block text-xs text-zinc-500">{hint}</span>}
    </label>
  );
}

export function Loading({ text = "Đang tải…" }) {
  return <div className="flex items-center justify-center gap-2 py-12 text-sm text-zinc-400"><Loader2 size={16} className="animate-spin" />{text}</div>;
}

export function ErrorText({ children }) {
  if (!children) return null;
  return <p role="alert" className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">{children}</p>;
}

export function Empty({ children }) {
  return <p className="py-10 text-center text-sm text-zinc-500">{children}</p>;
}

export function Stat({ label, value, hint, tone = "zinc" }) {
  const accent = { emerald: "text-emerald-300", amber: "text-amber-300", rose: "text-rose-300", sky: "text-sky-300", violet: "text-violet-300", zinc: "text-zinc-100" }[tone];
  return (
    <div className="rounded-2xl border border-zinc-800 bg-zinc-900 p-4">
      <p className="text-xs text-zinc-400">{label}</p>
      <p className={`mt-1 text-2xl font-semibold tabular-nums ${accent}`}>{value ?? 0}</p>
      {hint && <p className="mt-0.5 text-xs text-zinc-500">{hint}</p>}
    </div>
  );
}

/** Chọn nhiều nhân viên (lọc theo tên). */
export function UserPicker({ users, value, onChange, placeholder = "Tìm nhân viên…", single = false }) {
  const [keyword, setKeyword] = useState("");
  const selected = new Set(single ? (value ? [value] : []) : value);
  const filtered = users.filter((u) => !keyword.trim()
    || `${u.fullName} ${u.username}`.toLowerCase().includes(keyword.trim().toLowerCase())).slice(0, 50);
  const toggle = (id) => {
    if (single) return onChange(selected.has(id) ? "" : id);
    onChange(selected.has(id) ? value.filter((v) => v !== id) : [...value, id]);
  };
  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-1.5">
        {[...selected].map((id) => {
          const user = users.find((u) => u.id === id);
          return (
            <span key={id} className="inline-flex items-center gap-1 rounded-full bg-amber-400/15 px-2.5 py-1 text-xs text-amber-200">
              {user?.fullName || "Nhân viên"}
              <button type="button" aria-label="Bỏ chọn" onClick={() => toggle(id)}><X size={12} /></button>
            </span>
          );
        })}
      </div>
      <input className={control} value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder={placeholder} />
      <div className="max-h-44 overflow-y-auto rounded-xl border border-zinc-800">
        {filtered.length === 0 && <p className="p-3 text-xs text-zinc-500">Không có nhân viên phù hợp</p>}
        {filtered.map((u) => (
          <button key={u.id} type="button" onClick={() => toggle(u.id)}
            className={`flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-zinc-800 ${selected.has(u.id) ? "text-amber-200" : "text-zinc-200"}`}>
            <span>{u.fullName} <span className="text-xs text-zinc-500">@{u.username}</span></span>
            {selected.has(u.id) && <Check size={14} />}
          </button>
        ))}
      </div>
    </div>
  );
}

/** Thanh tiến độ các vòng (stepper). */
export function StageStepper({ stages, currentStageId, onSelect, selectedId }) {
  return (
    <ol className="flex gap-2 overflow-x-auto pb-1">
      {stages.map((s, index) => {
        const status = s.status;
        const current = s.stage.id === currentStageId;
        const color = status === "PASSED" ? "border-emerald-500/50 bg-emerald-500/10"
          : status === "FAILED" ? "border-rose-500/50 bg-rose-500/10"
            : status === "SKIPPED" ? "border-zinc-700 bg-zinc-900 opacity-60"
              : current ? "border-amber-400 bg-amber-400/10" : "border-zinc-800 bg-zinc-900";
        const dot = status === "PASSED" ? "bg-emerald-400 text-zinc-950" : status === "FAILED" ? "bg-rose-400 text-zinc-950"
          : current ? "bg-amber-400 text-zinc-950" : "bg-zinc-700 text-zinc-200";
        return (
          <li key={s.stage.id} className="min-w-[150px] flex-1">
            <button type="button" onClick={() => onSelect?.(s.stage.id)}
              className={`w-full rounded-2xl border p-3 text-left transition ${color} ${selectedId === s.stage.id ? "ring-2 ring-amber-300/60" : ""}`}>
              <div className="flex items-center gap-2">
                <span className={`grid h-6 w-6 shrink-0 place-items-center rounded-full text-xs font-bold ${dot}`}>
                  {status === "PASSED" ? <Check size={13} /> : status === "FAILED" ? <X size={13} /> : index + 1}
                </span>
                <span className="truncate text-sm font-semibold text-zinc-100">{s.stage.name}</span>
              </div>
              <p className="mt-1.5 text-xs text-zinc-400">
                {s.statusLabel}{s.score != null ? ` · ${Number(s.score)}/${Number(s.stage.scaleMax)}` : ""}
              </p>
            </button>
          </li>
        );
      })}
    </ol>
  );
}

const KIND_DOT = { STATUS: "bg-amber-400", STAGE: "bg-sky-400", EVALUATION: "bg-violet-400", INTERVIEW: "bg-emerald-400", OFFER: "bg-pink-400", DOCUMENT: "bg-zinc-400", NOTE: "bg-zinc-200" };

/** Dòng thời gian lịch sử hồ sơ. */
export function Timeline({ entries, statusLabel }) {
  if (!entries?.length) return <Empty>Chưa có lịch sử</Empty>;
  return (
    <ol className="relative space-y-4 border-l border-zinc-800 pl-5">
      {entries.map((h) => (
        <li key={h.id} className="relative">
          <span className={`absolute -left-[26px] top-1.5 h-2.5 w-2.5 rounded-full ${KIND_DOT[h.kind] || "bg-zinc-400"}`} />
          <p className="text-sm text-zinc-100">
            {h.message || (h.kind === "STATUS" ? `${h.fromStatus ? `${statusLabel(h.fromStatus)} → ` : ""}${statusLabel(h.toStatus)}` : h.toStatus)}
          </p>
          {h.reason && <p className="mt-0.5 text-xs text-zinc-400">Lý do: {h.reason}</p>}
          <p className="mt-0.5 text-xs text-zinc-500">{formatDateTime(h.createdAt)} · {h.actorName || "Hệ thống"}</p>
        </li>
      ))}
    </ol>
  );
}

/** Danh sách tài liệu + xem trước PDF / ảnh qua link đã ký (hết hạn sau vài phút). */
export function DocumentList({ candidateId, documents, canManage, onChanged, onUpload }) {
  const [viewer, setViewer] = useState(null);
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");
  const [showOld, setShowOld] = useState(false);

  const open = async (doc, disposition) => {
    setBusy(doc.id + disposition);
    setError("");
    try {
      const link = await recruitmentApi.documentUrl(candidateId, doc.id, disposition);
      if (disposition === "inline" && canPreview(doc)) setViewer({ doc, url: link.url, expires: link.expiresInSeconds });
      else window.open(link.url, "_blank", "noopener");
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy("");
    }
  };

  const remove = async (doc) => {
    if (!window.confirm(`Xóa ${doc.fileName} (phiên bản ${doc.version})?`)) return;
    try {
      await recruitmentApi.deleteDocument(candidateId, doc.id);
      onChanged?.();
    } catch (e) {
      setError(e.message);
    }
  };

  const visible = showOld ? documents : documents.filter((d) => d.current);
  const hiddenCount = documents.length - documents.filter((d) => d.current).length;
  return (
    <div className="space-y-2">
      <ErrorText>{error}</ErrorText>
      {visible.length === 0 && <Empty>Chưa có tài liệu</Empty>}
      {visible.map((doc) => (
        <div key={doc.id} className={`flex flex-wrap items-center gap-3 rounded-2xl border px-3 py-2.5 ${doc.current ? "border-zinc-800 bg-zinc-950/60" : "border-dashed border-zinc-800 opacity-70"}`}>
          <FileText size={18} className="shrink-0 text-amber-300" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm text-zinc-100">{doc.fileName}</p>
            <p className="text-xs text-zinc-500">
              {labelOf(DOC_TYPES, doc.docType)} · v{doc.version}{doc.current ? " (hiện hành)" : ""} · {formatBytes(doc.size)} · {formatDateTime(doc.uploadedAt)}
              {doc.uploadedByName ? ` · ${doc.uploadedByName}` : " · ứng viên tự nộp"}
            </p>
          </div>
          <div className="flex gap-1.5">
            {canPreview(doc) && (
              <button type="button" className={btnGhost} disabled={busy} onClick={() => open(doc, "inline")}>
                {busy === doc.id + "inline" ? <Loader2 size={14} className="animate-spin" /> : <ExternalLink size={14} />}Xem
              </button>
            )}
            <button type="button" className={btnGhost} disabled={busy} onClick={() => open(doc, "attachment")}><Download size={14} />Tải</button>
            {canManage && doc.current && onUpload && (
              <button type="button" className={btnGhost} onClick={() => onUpload(doc.docType)}>Thay thế</button>
            )}
            {canManage && <button type="button" className={btnDanger} onClick={() => remove(doc)} aria-label="Xóa"><X size={14} /></button>}
          </div>
        </div>
      ))}
      {hiddenCount > 0 && (
        <button type="button" className="text-xs text-amber-300 hover:underline" onClick={() => setShowOld((v) => !v)}>
          {showOld ? "Ẩn phiên bản cũ" : `Xem ${hiddenCount} phiên bản cũ`}
        </button>
      )}
      {viewer && (
        <AdminDialog size="xl" onClose={() => setViewer(null)}>
          <AdminDialogHeader title={viewer.doc.fileName} description={`Link xem có hiệu lực ${Math.round(viewer.expires / 60)} phút`} onClose={() => setViewer(null)} />
          <AdminDialogBody>
            {viewer.doc.contentType === "application/pdf"
              ? <iframe title={viewer.doc.fileName} src={viewer.url} className="h-[70vh] w-full rounded-xl bg-white" />
              : <img src={viewer.url} alt={viewer.doc.fileName} className="mx-auto max-h-[70vh] rounded-xl" />}
          </AdminDialogBody>
        </AdminDialog>
      )}
    </div>
  );
}

/** Hộp thoại tải tài liệu lên. */
export function UploadDialog({ candidateId, applicationId, defaultType = "CV", onClose, onUploaded }) {
  const [file, setFile] = useState(null);
  const [docType, setDocType] = useState(defaultType);
  const [note, setNote] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    const problem = validateFile(file);
    if (problem) return setError(problem);
    setSaving(true);
    try {
      const doc = await recruitmentApi.uploadDocument(candidateId, file, { docType, note, applicationId });
      onUploaded?.(doc);
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title="Tải tài liệu" description="PDF, DOC, DOCX, JPG, PNG · tối đa 10MB. Tải cùng loại sẽ tạo phiên bản mới." onClose={onClose} />
        <AdminDialogBody className="space-y-4">
          <Field label="Loại tài liệu">
            <select className={control} value={docType} onChange={(e) => setDocType(e.target.value)}>
              {DOC_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
          </Field>
          <Field label="File" required>
            <input type="file" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" className="block w-full text-sm text-zinc-300 file:mr-3 file:rounded-lg file:border-0 file:bg-zinc-800 file:px-3 file:py-2 file:text-zinc-100"
              onChange={(e) => { setFile(e.target.files?.[0] || null); setError(""); }} />
          </Field>
          <Field label="Ghi chú"><input className={control} value={note} maxLength={500} onChange={(e) => setNote(e.target.value)} /></Field>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving || !file}>{saving ? "Đang tải…" : "Tải lên"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}

/** Hộp thoại nhập lý do / ghi chú. */
export function ReasonDialog({ title, description, label = "Lý do", required = true, confirmText = "Xác nhận", danger, checkbox, onClose, onConfirm }) {
  const [value, setValue] = useState("");
  const [checked, setChecked] = useState(Boolean(checkbox?.defaultChecked));
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    if (required && !value.trim()) return setError(`Vui lòng nhập ${label.toLowerCase()}`);
    setSaving(true);
    try {
      await onConfirm(value.trim(), checked);
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={title} description={description} onClose={onClose} />
        <AdminDialogBody className="space-y-3">
          <Field label={label} required={required}>
            <textarea className={textarea} rows={4} value={value} maxLength={1000} onChange={(e) => setValue(e.target.value)} autoFocus />
          </Field>
          {checkbox && (
            <label className="flex items-start gap-2 text-sm text-zinc-300">
              <input type="checkbox" className="mt-0.5" checked={checked} onChange={(e) => setChecked(e.target.checked)} />
              <span>{checkbox.label}{checkbox.hint && <span className="block text-xs text-zinc-500">{checkbox.hint}</span>}</span>
            </label>
          )}
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={danger ? `${btnDanger} px-4 py-2.5` : btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : confirmText}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
