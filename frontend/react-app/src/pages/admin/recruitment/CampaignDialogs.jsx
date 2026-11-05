import { useState } from "react";
import { Plus, X } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import {
  ErrorText, Field, btnGhost, btnPrimary, control, textarea, useLoader, useRecruitment,
} from "../../../components/admin/recruitment/RecruitmentUi";
import {
  CAMPAIGN_STATUS, POSITION_STATUS, campaignPayload, emptyCampaignForm, emptyPositionForm, positionPayload,
  validateCampaign, validatePosition,
} from "./recruitmentLogic";

/** Tạo / sửa chiến dịch. Nhân viên cửa hàng luôn tuyển cho cửa hàng của mình. */
export function CampaignDialog({ initial, campaignId, onClose, onSaved }) {
  const { lookups, scope, users } = useRecruitment();
  const [form, setForm] = useState(initial || emptyCampaignForm(scope));
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const set = (patch) => setForm((f) => ({ ...f, ...patch }));
  // Thành viên phải là nhân viên cửa hàng của chiến dịch hoặc nhân viên cấp chuỗi.
  const assignable = useLoader((signal) => recruitmentApi.users({ storeId: form.storeId || undefined }, { signal }), [form.storeId]);
  const people = assignable.data || users;

  const submit = async (event) => {
    event.preventDefault();
    const problem = validateCampaign(form);
    if (problem) return setError(problem);
    setSaving(true);
    try {
      const saved = campaignId
        ? await recruitmentApi.updateCampaign(campaignId, campaignPayload(form, { includeStatus: false }))
        : await recruitmentApi.createCampaign(campaignPayload(form));
      onSaved(saved);
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <AdminDialog size="lg" onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={campaignId ? "Sửa chiến dịch" : "Tạo chiến dịch tuyển dụng"}
          description="Người phụ trách và thành viên RECRUITER quản lý chiến dịch; HIRING_MANAGER theo dõi và duyệt offer." onClose={onClose} />
        <AdminDialogBody className="grid gap-4 sm:grid-cols-2">
          <Field label="Tên chiến dịch" required className="sm:col-span-2">
            <input className={control} value={form.name} maxLength={200} onChange={(e) => set({ name: e.target.value })} autoFocus />
          </Field>
          <Field label="Cửa hàng tuyển" hint={scope.storeId ? "Bạn chỉ tuyển cho cửa hàng của mình" : "Để trống = văn phòng / toàn chuỗi"}>
            <select className={control} value={form.storeId} disabled={Boolean(scope.storeId)} onChange={(e) => set({ storeId: e.target.value, members: [] })}>
              {!scope.storeId && <option value="">Văn phòng / toàn chuỗi</option>}
              {lookups?.stores?.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          </Field>
          <Field label="Phòng ban">
            <select className={control} value={form.departmentId} onChange={(e) => set({ departmentId: e.target.value })}>
              <option value="">—</option>
              {lookups?.departments?.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
            </select>
          </Field>
          <Field label="Người phụ trách" hint="Để trống = bạn">
            <select className={control} value={form.ownerId} onChange={(e) => set({ ownerId: e.target.value })}>
              <option value="">—</option>
              {people.map((u) => <option key={u.id} value={u.id}>{u.fullName} (@{u.username})</option>)}
            </select>
          </Field>
          {!campaignId && (
            <Field label="Trạng thái">
              <select className={control} value={form.status} onChange={(e) => set({ status: e.target.value })}>
                {["DRAFT", "OPEN"].map((s) => <option key={s} value={s}>{CAMPAIGN_STATUS[s].label}</option>)}
              </select>
            </Field>
          )}
          <Field label="Bắt đầu" required><input type="date" className={control} value={form.startDate} onChange={(e) => set({ startDate: e.target.value })} /></Field>
          <Field label="Kết thúc"><input type="date" className={control} value={form.endDate} onChange={(e) => set({ endDate: e.target.value })} /></Field>
          <Field label="Mô tả" className="sm:col-span-2">
            <textarea className={textarea} rows={3} value={form.description} onChange={(e) => set({ description: e.target.value })} />
          </Field>
          <div className="space-y-2 sm:col-span-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-zinc-400">Thành viên được phân công</span>
              <button type="button" className={btnGhost} onClick={() => set({ members: [...form.members, { userId: "", role: "RECRUITER" }] })}>
                <Plus size={14} />Thêm
              </button>
            </div>
            {form.members.length === 0 && <p className="text-xs text-zinc-500">Chưa có thành viên.</p>}
            {form.members.map((m, i) => (
              <div key={i} className="flex gap-2">
                <select className={control} value={m.userId} onChange={(e) => set({ members: form.members.map((x, j) => (j === i ? { ...x, userId: e.target.value } : x)) })}>
                  <option value="">Chọn nhân viên…</option>
                  {people.map((u) => <option key={u.id} value={u.id}>{u.fullName} (@{u.username})</option>)}
                </select>
                <select className={`${control} max-w-44`} value={m.role} onChange={(e) => set({ members: form.members.map((x, j) => (j === i ? { ...x, role: e.target.value } : x)) })}>
                  <option value="RECRUITER">Recruiter</option>
                  <option value="HIRING_MANAGER">Hiring Manager</option>
                </select>
                <button type="button" className={btnGhost} aria-label="Bỏ" onClick={() => set({ members: form.members.filter((_, j) => j !== i) })}><X size={14} /></button>
              </div>
            ))}
          </div>
          <div className="sm:col-span-2"><ErrorText>{error}</ErrorText></div>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Lưu"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}

/** Tạo / sửa vị trí tuyển dụng (JD, kỹ năng, kinh nghiệm, lương, hạn nhận hồ sơ). */
export function PositionDialog({ campaignId, storeId, positionId, initial, onClose, onSaved }) {
  const { lookups, users } = useRecruitment();
  const [form, setForm] = useState(initial || emptyPositionForm());
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const templates = useLoader((signal) => (positionId ? Promise.resolve([]) : recruitmentApi.templates({ signal })), [positionId]);
  const assignable = useLoader((signal) => recruitmentApi.users({ storeId: storeId || undefined }, { signal }), [storeId]);
  const set = (patch) => setForm((f) => ({ ...f, ...patch }));

  const submit = async (event) => {
    event.preventDefault();
    const problem = validatePosition(form);
    if (problem) return setError(problem);
    setSaving(true);
    try {
      const saved = positionId
        ? await recruitmentApi.updatePosition(positionId, positionPayload(form))
        : await recruitmentApi.createPosition(campaignId, positionPayload(form));
      onSaved(saved);
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  const orgPositions = (lookups?.orgPositions || []).filter((p) => !form.departmentId || p.departmentId === form.departmentId);
  return (
    <AdminDialog size="xl" onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={positionId ? "Sửa vị trí tuyển dụng" : "Thêm vị trí tuyển dụng"} onClose={onClose} />
        <AdminDialogBody className="grid gap-4 md:grid-cols-3">
          <Field label="Tên vị trí" required className="md:col-span-2">
            <input className={control} value={form.title} maxLength={200} onChange={(e) => set({ title: e.target.value })} autoFocus />
          </Field>
          <Field label="Số lượng cần tuyển" required>
            <input type="number" min="1" className={control} value={form.headcount} onChange={(e) => set({ headcount: e.target.value })} />
          </Field>
          <Field label="Phòng ban">
            <select className={control} value={form.departmentId} onChange={(e) => set({ departmentId: e.target.value, orgPositionId: "" })}>
              <option value="">—</option>
              {lookups?.departments?.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
            </select>
          </Field>
          <Field label="Chức danh trong sơ đồ tổ chức">
            <select className={control} value={form.orgPositionId} onChange={(e) => set({ orgPositionId: e.target.value })}>
              <option value="">—</option>
              {orgPositions.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </Field>
          <Field label="Hiring manager">
            <select className={control} value={form.hiringManagerId} onChange={(e) => set({ hiringManagerId: e.target.value })}>
              <option value="">—</option>
              {(assignable.data || users).map((u) => <option key={u.id} value={u.id}>{u.fullName} (@{u.username})</option>)}
            </select>
          </Field>
          <Field label="Hình thức">
            <select className={control} value={form.employmentType} onChange={(e) => set({ employmentType: e.target.value })}>
              <option value="">—</option>
              {lookups?.employmentTypes?.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
          </Field>
          <Field label="Nơi làm việc"><input className={control} value={form.workLocation} onChange={(e) => set({ workLocation: e.target.value })} /></Field>
          <Field label="Hạn nhận hồ sơ"><input type="date" className={control} value={form.deadline} onChange={(e) => set({ deadline: e.target.value })} /></Field>
          <Field label="Lương từ (₫)"><input type="number" min="0" step="100000" className={control} value={form.salaryMin} disabled={form.salaryNegotiable} onChange={(e) => set({ salaryMin: e.target.value })} /></Field>
          <Field label="Lương đến (₫)"><input type="number" min="0" step="100000" className={control} value={form.salaryMax} disabled={form.salaryNegotiable} onChange={(e) => set({ salaryMax: e.target.value })} /></Field>
          <label className="flex items-end gap-2 pb-2 text-sm text-zinc-300">
            <input type="checkbox" checked={form.salaryNegotiable} onChange={(e) => set({ salaryNegotiable: e.target.checked })} />Lương thỏa thuận
          </label>
          <Field label="Kinh nghiệm từ (năm)"><input type="number" min="0" step="0.5" className={control} value={form.experienceMin} onChange={(e) => set({ experienceMin: e.target.value })} /></Field>
          <Field label="Kinh nghiệm đến (năm)"><input type="number" min="0" step="0.5" className={control} value={form.experienceMax} onChange={(e) => set({ experienceMax: e.target.value })} /></Field>
          <Field label="Kỹ năng yêu cầu" hint="Ngăn cách bằng dấu phẩy"><input className={control} value={form.skills} onChange={(e) => set({ skills: e.target.value })} /></Field>
          <Field label="Mô tả công việc (JD)" className="md:col-span-3">
            <textarea className={textarea} rows={5} value={form.jobDescription} onChange={(e) => set({ jobDescription: e.target.value })} />
          </Field>
          <Field label="Yêu cầu" className="md:col-span-3 lg:col-span-2">
            <textarea className={textarea} rows={4} value={form.requirements} onChange={(e) => set({ requirements: e.target.value })} />
          </Field>
          <Field label="Quyền lợi" className="md:col-span-3 lg:col-span-1">
            <textarea className={textarea} rows={4} value={form.benefits} onChange={(e) => set({ benefits: e.target.value })} />
          </Field>
          <Field label="Trạng thái">
            <select className={control} value={form.status} onChange={(e) => set({ status: e.target.value })}>
              {Object.entries(POSITION_STATUS).filter(([k]) => positionId || ["DRAFT", "OPEN"].includes(k))
                .map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
            </select>
          </Field>
          {!positionId && (
            <Field label="Quy trình từ mẫu" hint="Có thể chỉnh từng vòng sau khi tạo">
              <select className={control} value={form.templateId} onChange={(e) => set({ templateId: e.target.value })}>
                <option value="">Tự cấu hình sau</option>
                {templates.data?.map((t) => <option key={t.id} value={t.id}>{t.name} ({t.stages.length} vòng)</option>)}
              </select>
            </Field>
          )}
          <label className="flex items-end gap-2 pb-2 text-sm text-zinc-300">
            <input type="checkbox" checked={form.isPublic} onChange={(e) => set({ isPublic: e.target.checked })} />Đăng lên trang tuyển dụng
          </label>
          <div className="md:col-span-3"><ErrorText>{error}</ErrorText></div>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Lưu vị trí"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
