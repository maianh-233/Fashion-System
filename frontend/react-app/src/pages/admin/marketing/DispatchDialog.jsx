import { useState } from "react";
import { Users } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { ErrorText, Field, btnGhost, btnPrimary, control, textarea } from "../../../components/admin/recruitment/RecruitmentUi";
import { marketingApi } from "../../../api/admin/marketingApi";
import { CHANNELS, dispatchCriteria, dispatchPayload, dispatchToForm, emptyDispatchForm, validateDispatchForm } from "./marketingLogic";

/**
 * Thêm hoặc sửa (khi truyền {@code dispatch}) một đợt gửi thông báo trong app. Danh sách người nhận được chốt đúng lúc
 * bắt đầu gửi; "Xem trước" chỉ là ước lượng tại thời điểm bấm.
 */
export default function DispatchDialog({ campaign, dispatch, tiers = [], categories = [], approved, onClose, onSaved }) {
  const creating = !dispatch;
  const [form, setForm] = useState(() => (creating ? emptyDispatchForm() : dispatchToForm(dispatch)));
  const [preview, setPreview] = useState(null);
  const [previewing, setPreviewing] = useState(false);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const set = (key) => (event) => { setForm({ ...form, [key]: event.target.value }); if (!["title", "message", "holdoutPercent"].includes(key)) setPreview(null); };
  const toggleTier = (id) => {
    setForm({ ...form, tierIds: form.tierIds.includes(id) ? form.tierIds.filter((t) => t !== id) : [...form.tierIds, id] });
    setPreview(null);
  };

  const runPreview = async () => {
    setPreviewing(true);
    setError("");
    try {
      setPreview(await marketingApi.previewAudience({ criteria: dispatchCriteria(form), channel: form.channel, campaignId: campaign.id }));
    } catch (e) {
      setError(e.message);
    } finally {
      setPreviewing(false);
    }
  };

  const submit = async (event) => {
    event.preventDefault();
    const problem = validateDispatchForm(form);
    if (problem) return setError(problem);
    setSaving(true);
    setError("");
    try {
      const body = dispatchPayload(form);
      onSaved(creating ? await marketingApi.createDispatch(campaign.id, body) : await marketingApi.updateDispatch(campaign.id, dispatch.id, body));
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={creating ? "Thêm đợt gửi" : `Sửa đợt gửi ${dispatch.code}`} onClose={onClose}
          description={approved
            ? "Chiến dịch đã duyệt nên đợt này cần người có quyền duyệt phê duyệt riêng trước khi gửi."
            : "Đợt gửi được duyệt cùng chiến dịch."} />
        <AdminDialogBody className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Kênh gửi" required hint={CHANNELS[form.channel]?.hint}>
              <select className={control} value={form.channel} onChange={set("channel")}>
                {Object.entries(CHANNELS).map(([value, c]) => <option key={value} value={value}>{c.label}</option>)}
              </select>
            </Field>
            <Field label="Nhóm đối chứng (%)" hint="Giữ lại ngẫu nhiên % khách đủ điều kiện, không gửi, để đo mức tăng thêm (0 = không dùng)">
              <input type="number" min="0" max="50" step="5" className={control} value={form.holdoutPercent} onChange={set("holdoutPercent")} />
            </Field>
          </div>
          <fieldset className="space-y-3 rounded-2xl border border-zinc-800 p-3">
            <legend className="px-1 text-xs font-medium text-zinc-400">Nhóm khách (thỏa tất cả tiêu chí đã chọn; bỏ trống là mọi khách)</legend>
            {tiers.length > 0 && (
              <div className="flex flex-wrap gap-2" aria-label="Hạng khách hàng">
                {tiers.map((t) => (
                  <label key={t.id} className={`flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-1.5 text-sm ${form.tierIds.includes(t.id) ? "border-amber-400 bg-amber-500/10 text-amber-200" : "border-zinc-700 text-zinc-300"}`}>
                    <input type="checkbox" className="accent-amber-400" checked={form.tierIds.includes(t.id)} onChange={() => toggleTier(t.id)} />{t.name}
                  </label>
                ))}
              </div>
            )}
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label="Lâu chưa mua (ngày)" hint="Đã từng mua nhưng không đặt đơn trong chừng này ngày">
                <input type="number" min="1" max="1095" className={control} value={form.inactiveDays} onChange={set("inactiveDays")} />
              </Field>
              <Field label="Từng mua danh mục">
                <select className={control} value={form.categoryId} onChange={set("categoryId")}>
                  <option value="">Không lọc</option>
                  {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </Field>
            </div>
            <div className="flex flex-wrap items-center gap-3">
              <button type="button" className={btnGhost} onClick={runPreview} disabled={previewing}><Users size={15} />{previewing ? "Đang đếm…" : "Xem trước nhóm khách"}</button>
              {preview && (
                <p className="text-sm text-zinc-300">
                  Khớp <b>{preview.matched}</b> khách · {preview.channel === "EMAIL" ? "có email và đã đồng ý" : "có tài khoản, không tắt tin"} <b>{preview.reachable}</b> · dự kiến nhận <b className="text-amber-300">{preview.afterFrequencyCap}</b>
                  <span className="block text-xs text-zinc-500">Bỏ qua khách đã nhận tin cùng kênh trong {preview.frequencyCapHours} giờ{Number(form.holdoutPercent) > 0 ? `; khoảng ${form.holdoutPercent}% còn lại được giữ làm nhóm đối chứng` : ""}. Danh sách thật được chốt lúc gửi.</span>
                </p>
              )}
            </div>
          </fieldset>
          <Field label="Tiêu đề" required>
            <input className={control} value={form.title} maxLength={120} onChange={set("title")} placeholder="Ví dụ: Sale cuối năm đã bắt đầu" />
          </Field>
          <Field label="Nội dung" required hint={`${form.message.length}/500`}>
            <textarea className={textarea} rows={3} value={form.message} maxLength={500} onChange={set("message")} />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Liên kết khi bấm" hint="Trang của khách, ví dụ /promotions">
              <input className={control} value={form.actionUrl} maxLength={500} onChange={set("actionUrl")} placeholder="/promotions" />
            </Field>
            <Field label="Giờ gửi" required hint="Từ 7 ngày trước khi bắt đầu tới lúc kết thúc chiến dịch">
              <input type="datetime-local" className={control} value={form.scheduledAt} max={String(campaign.endsAt).slice(0, 16)} onChange={set("scheduledAt")} />
            </Field>
          </div>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : creating ? "Thêm đợt gửi" : "Lưu thay đổi"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
