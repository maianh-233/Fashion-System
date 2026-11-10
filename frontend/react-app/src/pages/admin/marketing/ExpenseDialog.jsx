import { useState } from "react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { ErrorText, Field, btnGhost, btnPrimary, control } from "../../../components/admin/recruitment/RecruitmentUi";
import { marketingApi } from "../../../api/admin/marketingApi";
import { EXPENSE_CATEGORIES, emptyExpenseForm, expensePayload, expenseToForm, validateExpenseForm } from "./marketingLogic";

/** Thêm hoặc sửa (khi truyền {@code expense}) một khoản chi thực tế của chiến dịch. */
export default function ExpenseDialog({ campaignId, expense, onClose, onSaved }) {
  const creating = !expense;
  const [form, setForm] = useState(() => (creating ? emptyExpenseForm() : expenseToForm(expense)));
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value });

  const submit = async (event) => {
    event.preventDefault();
    const problem = validateExpenseForm(form);
    if (problem) return setError(problem);
    setSaving(true);
    setError("");
    try {
      const body = expensePayload(form);
      onSaved(creating ? await marketingApi.addExpense(campaignId, body) : await marketingApi.updateExpense(campaignId, expense.id, body));
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={creating ? "Thêm chi phí" : "Sửa chi phí"} onClose={onClose}
          description="Chi phí thực tế ngoài hệ thống. Chi phí giảm giá của ưu đãi sẽ được tính tự động ở báo cáo." />
        <AdminDialogBody className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Loại chi phí" required>
              <select className={control} value={form.category} onChange={set("category")}>
                {Object.entries(EXPENSE_CATEGORIES).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </Field>
            <Field label="Số tiền (đ)" required>
              <input type="number" min="1" step="1" className={control} value={form.amount} onChange={set("amount")} />
            </Field>
            <Field label="Ngày chi" required>
              <input type="date" className={control} value={form.spentOn} onChange={set("spentOn")} />
            </Field>
            <Field label="Đơn vị / đối tác">
              <input className={control} value={form.vendor} maxLength={150} onChange={set("vendor")} placeholder="Ví dụ: Facebook Ads" />
            </Field>
          </div>
          <Field label="Ghi chú">
            <input className={control} value={form.note} maxLength={500} onChange={set("note")} />
          </Field>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : creating ? "Thêm chi phí" : "Lưu thay đổi"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
