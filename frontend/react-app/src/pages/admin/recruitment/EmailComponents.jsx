import { useState } from "react";
import { ChevronDown, ChevronRight, Mail, RotateCw } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { Badge, ErrorText, Field, btnGhost, btnPrimary, control, textarea, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { EMAIL_STATUS, formatDateTime } from "./recruitmentLogic";

/** Soạn email cho ứng viên: chọn mẫu để điền sẵn (đã thay biến của hồ sơ) rồi chỉnh sửa trước khi gửi. */
export function ComposeEmailDialog({ applicationId, toEmail, onClose, onSent }) {
  const templates = useLoader((signal) => recruitmentApi.emailTemplates({ signal }), []);
  const [form, setForm] = useState({ templateCode: "", subject: "", body: "" });
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);

  const pickTemplate = async (templateCode) => {
    setForm((f) => ({ ...f, templateCode }));
    if (!templateCode) return;
    try {
      const rendered = await recruitmentApi.previewEmail(applicationId, templateCode);
      setForm({ templateCode, subject: rendered.subject, body: rendered.body });
    } catch (e) {
      setError(e.message);
    }
  };

  const send = async (event) => {
    event.preventDefault();
    if (!form.subject.trim() || !form.body.trim()) return setError("Vui lòng nhập tiêu đề và nội dung");
    setSending(true);
    try {
      onSent(await recruitmentApi.sendEmail(applicationId, { ...form, templateCode: form.templateCode || null }));
    } catch (e) {
      setError(e.message);
      setSending(false);
    }
  };

  return (
    <AdminDialog size="lg" onClose={onClose}>
      <form onSubmit={send}>
        <AdminDialogHeader title="Gửi email cho ứng viên" description={`Tới: ${toEmail || "(ứng viên chưa có email)"}`} onClose={onClose} />
        <AdminDialogBody className="space-y-4">
          <Field label="Dùng mẫu" hint="Biến {{...}} còn lại trong nội dung sẽ được thay khi gửi">
            <select className={control} value={form.templateCode} onChange={(e) => pickTemplate(e.target.value)}>
              <option value="">Soạn tự do</option>
              {templates.data?.map((t) => <option key={t.code} value={t.code}>{t.name}</option>)}
            </select>
          </Field>
          <Field label="Tiêu đề" required><input className={control} value={form.subject} maxLength={300} onChange={(e) => setForm({ ...form, subject: e.target.value })} /></Field>
          <Field label="Nội dung" required>
            <textarea className={`${textarea} font-mono text-[13px]`} rows={12} value={form.body} onChange={(e) => setForm({ ...form, body: e.target.value })} />
          </Field>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={sending || !toEmail}><Mail size={15} />{sending ? "Đang gửi…" : "Gửi email"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}

/** Nhật ký email đã gửi cho ứng viên (trạng thái gửi, lỗi, gửi lại). */
export function EmailLogList({ applicationId, emails, canManage, onChanged }) {
  const [open, setOpen] = useState(null);
  const [error, setError] = useState("");
  const retry = async (email) => {
    try {
      await recruitmentApi.retryEmail(applicationId, email.id);
      onChanged?.();
    } catch (e) {
      setError(e.message);
    }
  };
  if (!emails.length) return <p className="text-sm text-zinc-500">Chưa gửi email nào cho ứng viên.</p>;
  return (
    <div className="space-y-2">
      <ErrorText>{error}</ErrorText>
      {emails.map((e) => (
        <div key={e.id} className="rounded-2xl border border-zinc-800 bg-zinc-950/50">
          <button type="button" className="flex w-full items-start gap-2 p-3 text-left" onClick={() => setOpen(open === e.id ? null : e.id)} aria-expanded={open === e.id}>
            {open === e.id ? <ChevronDown size={15} className="mt-0.5 shrink-0" /> : <ChevronRight size={15} className="mt-0.5 shrink-0" />}
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm text-zinc-100">{e.subject}</span>
              <span className="block text-xs text-zinc-500">{e.toEmail} · {formatDateTime(e.sentAt || e.createdAt)} · {e.sentByName || "Tự động"}</span>
            </span>
            <Badge map={EMAIL_STATUS} value={e.status} />
          </button>
          {open === e.id && (
            <div className="border-t border-zinc-800 p-3">
              <pre className="whitespace-pre-wrap font-sans text-sm text-zinc-300">{e.body}</pre>
              {e.error && <p className="mt-2 text-xs text-rose-300">Lỗi: {e.error}</p>}
            </div>
          )}
          {e.status === "FAILED" && canManage && (
            <div className="px-3 pb-3"><button type="button" className={btnGhost} onClick={() => retry(e)}><RotateCw size={14} />Gửi lại</button></div>
          )}
        </div>
      ))}
    </div>
  );
}
