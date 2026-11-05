import { useState } from "react";
import { Pencil } from "lucide-react";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { useSystemNotification } from "../../../components/common/SystemNotification";
import { Badge, ErrorText, Field, Loading, Section, btnGhost, btnPrimary, control, textarea, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { EMAIL_TEMPLATE_HINTS, formatDateTime } from "./recruitmentLogic";

/** Mẫu email gửi ứng viên (dùng chung toàn chuỗi; HR Admin cấp chuỗi được sửa, bật / tắt gửi tự động). */
export default function EmailTemplateList() {
  const notification = useSystemNotification();
  const { data, error, loading, reload } = useLoader((signal) => recruitmentApi.emailTemplates({ signal }), []);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);

  const save = async () => {
    setSaving(true);
    try {
      await recruitmentApi.updateEmailTemplate(editing.code, { subject: editing.subject, body: editing.body, enabled: editing.enabled });
      notification.success("Đã lưu mẫu email.");
      setEditing(null);
      reload();
    } catch (e) {
      notification.error(e.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading && !data) return <Loading />;
  if (!data) return <ErrorText>{error}</ErrorText>;
  const placeholders = data[0]?.placeholders || [];

  return (
    <div className="space-y-4">
      <p className="text-sm text-zinc-400">
        Biến dùng được: {placeholders.map((p) => <code key={p} className="mr-1.5 rounded bg-zinc-800 px-1.5 py-0.5 text-xs text-amber-200">{`{{${p}}}`}</code>)}
      </p>
      {data.map((t) => (
        <Section key={t.code} title={t.name} action={
          <div className="flex items-center gap-2">
            <Badge tone={t.enabled ? "emerald" : "zinc"}>{t.enabled ? "Tự động gửi" : "Đã tắt"}</Badge>
            {t.canEdit && editing?.code !== t.code && <button type="button" className={btnGhost} onClick={() => setEditing({ ...t })}><Pencil size={14} />Sửa</button>}
          </div>}>
          <p className="mb-3 text-xs text-zinc-500">{EMAIL_TEMPLATE_HINTS[t.code]}{t.updatedAt ? ` · Sửa lần cuối ${formatDateTime(t.updatedAt)} bởi ${t.updatedByName}` : ""}</p>
          {editing?.code === t.code ? (
            <div className="space-y-3">
              <Field label="Tiêu đề"><input className={control} value={editing.subject} maxLength={300} onChange={(e) => setEditing({ ...editing, subject: e.target.value })} /></Field>
              <Field label="Nội dung"><textarea className={`${textarea} font-mono text-[13px]`} rows={12} value={editing.body} onChange={(e) => setEditing({ ...editing, body: e.target.value })} /></Field>
              <label className="flex items-center gap-2 text-sm text-zinc-300">
                <input type="checkbox" checked={editing.enabled} onChange={(e) => setEditing({ ...editing, enabled: e.target.checked })} />Tự động gửi email này
              </label>
              <div className="flex justify-end gap-2">
                <button type="button" className={btnGhost} onClick={() => setEditing(null)}>Hủy</button>
                <button type="button" className={btnPrimary} disabled={saving || !editing.subject.trim() || !editing.body.trim()} onClick={save}>{saving ? "Đang lưu…" : "Lưu mẫu"}</button>
              </div>
            </div>
          ) : (
            <div className="rounded-2xl border border-zinc-800 bg-zinc-950/50 p-3">
              <p className="text-sm font-semibold text-zinc-100">{t.subject}</p>
              <pre className="mt-2 whitespace-pre-wrap font-sans text-sm text-zinc-400">{t.body}</pre>
            </div>
          )}
        </Section>
      ))}
    </div>
  );
}
