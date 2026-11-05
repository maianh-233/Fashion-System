import { useState } from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { useSystemNotification } from "../../../components/common/SystemNotification";
import {
  ErrorText, Field, Loading, Section, btnDanger, btnGhost, btnPrimary, control, textarea, useLoader, useRecruitment,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { emptyStage, formatDate, passRuleText, stagesToForm, stagesToPayload } from "./recruitmentLogic";
import PipelineEditor from "./PipelineEditor";

/** Mẫu quy trình tuyển dụng (vòng + tiêu chí) để áp dụng nhanh cho vị trí mới. */
export default function TemplateList() {
  const notification = useSystemNotification();
  const { can } = useRecruitment();
  const { data, error, loading, reload } = useLoader((signal) => recruitmentApi.templates({ signal }), []);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);

  const save = async () => {
    if (!editing.name.trim()) return notification.error("Vui lòng nhập tên mẫu");
    setSaving(true);
    const body = { name: editing.name.trim(), description: editing.description.trim() || null, stages: stagesToPayload(editing.stages, { keepIds: false }) };
    try {
      if (editing.id) await recruitmentApi.updateTemplate(editing.id, body); else await recruitmentApi.createTemplate(body);
      notification.success("Đã lưu mẫu quy trình.");
      setEditing(null);
      reload();
    } catch (e) {
      notification.error(e.message);
    } finally {
      setSaving(false);
    }
  };

  const remove = async (t) => {
    if (!await notification.confirm(`Xóa mẫu "${t.name}"? Các vị trí đã áp dụng không bị ảnh hưởng.`, { confirmText: "Xóa" })) return;
    try {
      await recruitmentApi.deleteTemplate(t.id);
      reload();
    } catch (e) {
      notification.error(e.message);
    }
  };

  if (editing) {
    return (
      <Section title={editing.id ? "Sửa mẫu quy trình" : "Tạo mẫu quy trình"}>
        <div className="mb-4 grid gap-3 md:grid-cols-3">
          <Field label="Tên mẫu" required><input className={control} value={editing.name} maxLength={150} onChange={(e) => setEditing({ ...editing, name: e.target.value })} /></Field>
          <Field label="Mô tả" className="md:col-span-2"><textarea className={textarea} rows={1} value={editing.description} onChange={(e) => setEditing({ ...editing, description: e.target.value })} /></Field>
        </div>
        <PipelineEditor value={editing.stages} onChange={(stages) => setEditing({ ...editing, stages })} owners={null}
          onSave={save} saving={saving} saveText="Lưu mẫu" extraActions={<button type="button" className={btnGhost} onClick={() => setEditing(null)}>Hủy</button>} />
      </Section>
    );
  }

  return (
    <div className="space-y-4">
      {can.manage && (
        <div className="flex justify-end">
          <button type="button" className={btnPrimary} onClick={() => setEditing({ id: null, name: "", description: "", stages: [emptyStage("SCREENING")] })}><Plus size={15} />Tạo mẫu</button>
        </div>
      )}
      <ErrorText>{error}</ErrorText>
      {loading && !data && <Loading />}
      <div className="grid gap-4 lg:grid-cols-2">
        {data?.map((t) => (
          <Section key={t.id} title={t.name} action={t.canEdit && (
            <div className="flex gap-1.5">
              <button type="button" className={btnGhost} onClick={() => setEditing({ id: t.id, name: t.name, description: t.description || "", stages: stagesToForm(t.stages) })}><Pencil size={14} />Sửa</button>
              <button type="button" className={btnDanger} onClick={() => remove(t)} aria-label="Xóa"><Trash2 size={14} /></button>
            </div>)}>
            {t.description && <p className="mb-3 text-sm text-zinc-400">{t.description}</p>}
            <ol className="space-y-1.5">
              {t.stages.map((s, i) => (
                <li key={i} className="flex gap-2 text-sm">
                  <span className="grid h-5 w-5 shrink-0 place-items-center rounded-full bg-zinc-800 text-xs">{i + 1}</span>
                  <span className="text-zinc-100">{s.name}</span>
                  <span className="text-xs text-zinc-500">· {s.criteria.length} tiêu chí · {passRuleText(s)}</span>
                </li>
              ))}
            </ol>
            <p className="mt-3 text-xs text-zinc-500">{t.createdByName ? `Tạo bởi ${t.createdByName} · ` : ""}{formatDate(t.updatedAt || t.createdAt)}</p>
          </Section>
        ))}
      </div>
    </div>
  );
}
