import { useState } from "react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { ErrorText, Field, btnGhost, btnPrimary, control, textarea } from "../../../components/admin/recruitment/RecruitmentUi";
import { marketingApi } from "../../../api/admin/marketingApi";
import { OBJECTIVES, campaignPayload, campaignToForm, emptyCampaignForm, validateCampaignForm } from "./marketingLogic";

/** Tạo chiến dịch mới, hoặc sửa khi truyền {@code campaign} (chỉ khi Nháp / Bị từ chối). */
/** {@code stores}: danh sách cửa hàng cho nhân viên cấp chuỗi; {@code ownStoreName}: cửa hàng cố định của quản lý cửa hàng. */
export default function CampaignFormDialog({ owners = [], stores = [], ownStoreName, campaign, onClose, onSaved }) {
  const creating = !campaign;
  const [form, setForm] = useState(() => (creating ? emptyCampaignForm() : campaignToForm(campaign)));
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value });

  const submit = async (event) => {
    event.preventDefault();
    const problem = validateCampaignForm(form);
    if (problem) return setError(problem);
    setSaving(true);
    setError("");
    try {
      const body = campaignPayload(form);
      onSaved(creating ? await marketingApi.create(body) : await marketingApi.update(campaign.id, body));
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={creating ? "Tạo chiến dịch" : `Sửa chiến dịch ${campaign.code}`}
          description="Chiến dịch được lưu ở trạng thái Nháp. Gắn ưu đãi và gửi duyệt ở trang chi tiết." onClose={onClose} />
        <AdminDialogBody className="space-y-4">
          <Field label="Tên chiến dịch" required>
            <input className={control} value={form.name} maxLength={150} onChange={set("name")} placeholder="Ví dụ: Sale cuối năm 12.12" />
          </Field>
          <Field label="Mục tiêu" required hint={OBJECTIVES[form.objective]?.hint}>
            <select className={control} value={form.objective} onChange={set("objective")}>
              {Object.entries(OBJECTIVES).map(([value, o]) => <option key={value} value={value}>{o.label}</option>)}
            </select>
          </Field>
          <Field label="Mô tả">
            <textarea className={textarea} rows={3} value={form.description} maxLength={5000} onChange={set("description")}
              placeholder="Thông điệp chính, đối tượng khách, kênh dự kiến…" />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Bắt đầu" required>
              <input type="datetime-local" className={control} value={form.startsAt} onChange={set("startsAt")} />
            </Field>
            <Field label="Kết thúc" required>
              <input type="datetime-local" className={control} value={form.endsAt} min={form.startsAt || undefined} onChange={set("endsAt")} />
            </Field>
          </div>
          {ownStoreName ? (
            <Field label="Phạm vi" hint="Quản lý cửa hàng chỉ lập chiến dịch cho cửa hàng của mình">
              <input className={control} value={`Cửa hàng ${ownStoreName}`} disabled />
            </Field>
          ) : (
            <Field label="Phạm vi" hint="Chiến dịch cửa hàng chỉ gửi tin cho khách của cửa hàng và chỉ tính đơn tại cửa hàng đó">
              <select className={control} value={form.storeId} onChange={set("storeId")}>
                <option value="">Toàn chuỗi</option>
                {stores.map((s) => <option key={s.id} value={s.id}>Cửa hàng {s.name}</option>)}
              </select>
            </Field>
          )}
          <Field label="Người phụ trách" hint="Bỏ trống thì bạn là người phụ trách">
            <select className={control} value={form.ownerId} onChange={set("ownerId")}>
              <option value="">Tôi</option>
              {owners.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
            </select>
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Ngân sách dự kiến (đ)">
              <input type="number" min="0" step="1000" className={control} value={form.plannedBudget} onChange={set("plannedBudget")} />
            </Field>
            <Field label="Chỉ tiêu doanh thu (đ)">
              <input type="number" min="0" step="1000" className={control} value={form.targetRevenue} onChange={set("targetRevenue")} />
            </Field>
            <Field label="Chỉ tiêu số đơn">
              <input type="number" min="0" step="1" className={control} value={form.targetOrders} onChange={set("targetOrders")} />
            </Field>
            <Field label="Chỉ tiêu số khách mua">
              <input type="number" min="0" step="1" className={control} value={form.targetBuyers} onChange={set("targetBuyers")} />
            </Field>
          </div>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : creating ? "Tạo chiến dịch" : "Lưu thay đổi"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
