import { useEffect, useState } from "react";
import Button from "../../common/Button";
import PromotionBasicInfo from "./PromotionBasicInfo";
import PromotionDateSection from "./PromotionDateSection";
import PromotionLimitSection from "./PromotionLimitSection";
import PromotionCustomerTier from "./PromotionCustomerTier";
import PromotionProductPicker from "./PromotionProductPicker";
import PromotionMetaInfo from "./PromotionMetaInfo";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";
import { customerTierOptionsApi, promotionApi } from "../../../api/promotionApi";
import { useSystemNotification } from "../../common/SystemNotification";
import {
  emptyPromotionForm,
  formToRequest,
  promotionToForm,
  validatePromotionForm,
} from "./promotionOptions";

export default function PromotionDialog({ mode = "view", promotionId, onClose, onSaved }) {
  const notification = useSystemNotification();
  const isView = mode === "view";
  const isCreate = mode === "create";

  const [promotion, setPromotion] = useState(null);
  const [form, setForm] = useState(emptyPromotionForm);
  const [loading, setLoading] = useState(!isCreate);
  const [loadError, setLoadError] = useState("");
  const [tiers, setTiers] = useState([]);
  const [tiersLoading, setTiersLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    customerTierOptionsApi.list({ signal: controller.signal })
      .then((page) => setTiers(page?.content || []))
      .catch(() => setTiers([]))
      .finally(() => setTiersLoading(false));
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (isCreate || !promotionId) return undefined;
    const controller = new AbortController();
    promotionApi.detail(promotionId, { signal: controller.signal })
      .then((data) => { setPromotion(data); setForm(promotionToForm(data)); })
      .catch((error) => { if (!controller.signal.aborted) setLoadError(error.message || "Không tải được khuyến mãi"); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [isCreate, promotionId]);

  const onChange = (field, value) => {
    setFormError("");
    setForm((current) => ({ ...current, [field]: value }));
  };

  // Đã phát sinh lượt dùng thì không đổi được kiểu giảm, giá trị giảm và ngày bắt đầu.
  const locked = mode === "edit" && Boolean(promotion?.inUse);

  const submit = async (event) => {
    event.preventDefault();
    const message = validatePromotionForm(form);
    if (message) { setFormError(message); return; }
    setSaving(true);
    try {
      const body = formToRequest(form);
      const saved = isCreate ? await promotionApi.create(body) : await promotionApi.update(promotionId, body);
      notification.success(isCreate ? `Đã tạo khuyến mãi ${saved.code}.` : "Đã cập nhật khuyến mãi.");
      onSaved(saved);
    } catch (error) {
      setFormError(error.message || "Không lưu được khuyến mãi");
    } finally {
      setSaving(false);
    }
  };

  const title = (isView && "Chi tiết khuyến mãi") || (mode === "edit" && "Chỉnh sửa khuyến mãi") || "Thêm khuyến mãi";

  return (
    <AdminDialog open onClose={onClose} size="lg">
      <form onSubmit={submit}>
        <AdminDialogHeader title={title} onClose={onClose} />

        <AdminDialogBody className="space-y-8">
          {loading && <p className="text-sm text-zinc-400">Đang tải khuyến mãi...</p>}
          {loadError && <p className="text-sm text-red-400">{loadError}</p>}

          {!loading && !loadError && (
            <>
              {locked && (
                <p className="rounded-xl border border-amber-500/40 bg-amber-500/10 px-4 py-3 text-sm text-amber-300">
                  Khuyến mãi đã được sử dụng nên không thể đổi kiểu giảm, giá trị giảm và ngày bắt đầu.
                </p>
              )}

              <PromotionBasicInfo mode={mode} form={form} code={promotion?.code} locked={locked} onChange={onChange} />
              <PromotionDateSection mode={mode} form={form} locked={locked} onChange={onChange} />
              <PromotionLimitSection mode={mode} form={form} onChange={onChange} />
              {form.promotionType === "PRODUCT" && (
                <PromotionProductPicker
                  mode={mode}
                  selected={form.products}
                  onChange={(products) => onChange("products", products)}
                />
              )}
              <PromotionCustomerTier
                mode={mode}
                tiers={tiers}
                loading={tiersLoading}
                selectedIds={form.tierIds}
                onChange={(tierIds) => onChange("tierIds", tierIds)}
              />
              {!isCreate && promotion && <PromotionMetaInfo promotion={promotion} />}

              {formError && (
                <p role="alert" className="rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
                  {formError}
                </p>
              )}
            </>
          )}
        </AdminDialogBody>

        {!isView && (
          <AdminDialogFooter>
            <Button type="button" onClick={onClose} className="px-4 py-2 rounded bg-gray-700 hover:bg-gray-600">
              Hủy
            </Button>
            <Button
              type="submit"
              disabled={saving || loading || Boolean(loadError)}
              className="px-6 py-2 rounded bg-orange-500 hover:bg-orange-600 text-black font-semibold disabled:opacity-60"
            >
              {saving ? "Đang lưu..." : isCreate ? "Thêm mới" : "Lưu thay đổi"}
            </Button>
          </AdminDialogFooter>
        )}
      </form>
    </AdminDialog>
  );
}
