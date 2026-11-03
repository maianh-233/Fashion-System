import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Megaphone, Plus, Users } from "lucide-react";
import Button from "../../components/common/Button";
import Pagination from "../../components/common/Pagination";
import { useSystemNotification } from "../../components/common/SystemNotification";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../components/admin/common/AdminDialog";
import { voucherCampaignApi } from "../../api/admin/voucherCampaignApi";
import { customerTierOptionsApi } from "../../api/admin/promotionApi";
import { categoryApi } from "../../api/admin/catalogApi";
import {
  CAMPAIGN_STATUS, audienceText, benefitText, campaignPayload, criteriaPayload, emptyCampaignForm, usageText,
  validateCampaignForm,
} from "./voucherCampaignLogic";

const control = "h-10 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-amber-400";
const formatTime = (value) => (value ? new Date(value).toLocaleString("vi-VN") : "");
const money = (value) => `${Number(value || 0).toLocaleString("vi-VN")} ₫`;

/** Chiến dịch voucher theo nhóm khách: chọn nhóm, mức giảm, gửi ngay hoặc hẹn giờ; theo dõi số voucher đã dùng. */
export default function VoucherCampaignManagement() {
  const notification = useSystemNotification();
  const [page, setPage] = useState(1);
  const [version, setVersion] = useState(0);
  const [state, setState] = useState({ key: null, rows: [], totalPages: 1, error: "" });
  const [creating, setCreating] = useState(false);
  const key = `${page}|${version}`;

  useEffect(() => {
    const controller = new AbortController();
    voucherCampaignApi.list({ page: page - 1 }, { signal: controller.signal })
      .then((res) => setState({ key, rows: res.content || [], totalPages: Math.max(1, res.page?.totalPages || res.totalPages || 1), error: "" }))
      .catch((failure) => { if (!controller.signal.aborted) setState({ key, rows: [], totalPages: 1, error: failure.message }); });
    return () => controller.abort();
  }, [key, page]);

  const cancel = async (campaign) => {
    if (!await notification.confirm(`Hủy chiến dịch "${campaign.name}"? Voucher sẽ không được gửi.`, { confirmText: "Hủy chiến dịch" })) return;
    try {
      await voucherCampaignApi.cancel(campaign.id);
      notification.success("Đã hủy chiến dịch.");
      setVersion((v) => v + 1);
    } catch (failure) {
      notification.error(failure.message);
    }
  };

  const loading = state.key !== key;
  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100">
      <AdminCatalogPageHeader icon={Megaphone} eyebrow="Chiến dịch bán hàng" title="Chiến dịch voucher"
        description="Tặng voucher riêng có hạn cho một nhóm khách (theo hạng, lâu chưa mua, từng mua danh mục) và báo qua thông báo + email. Voucher giảm trên toàn đơn, dùng một lần, online và tại quầy."
        action={<div className="flex gap-2">
          <Link to="/admin/promotions" className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-sm text-zinc-300 hover:text-white"><ArrowLeft size={15} />Khuyến mãi</Link>
          <Button type="button" variant="primary" permission="PROMOTION_CREATE" globalOnly className="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold"
            onClick={() => setCreating(true)}><Plus size={15} />Tạo chiến dịch</Button>
        </div>} />

      <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900">
        {state.error && <p role="alert" className="m-4 text-sm text-rose-300">{state.error}</p>}
        <div className="overflow-x-auto">
          <table className="w-full min-w-[1000px] text-sm">
            <thead className="bg-zinc-800/70 text-zinc-300">
              <tr>{["Chiến dịch", "Nhóm khách", "Ưu đãi", "Gửi", "Đã dùng", "Trạng thái", ""].map((h) => <th key={h} className="px-4 py-3 text-left font-semibold">{h}</th>)}</tr>
            </thead>
            <tbody>
              {loading && <tr><td colSpan="7" className="px-4 py-12 text-center text-zinc-400">Đang tải…</td></tr>}
              {!loading && state.rows.length === 0 && <tr><td colSpan="7" className="px-4 py-12 text-center text-zinc-400">Chưa có chiến dịch nào.</td></tr>}
              {!loading && state.rows.map((campaign) => (
                <tr key={campaign.id} className="border-t border-zinc-800 align-top hover:bg-zinc-800/30">
                  <td className="px-4 py-3">
                    <p className="font-semibold">{campaign.name}</p>
                    <p className="text-xs text-zinc-500">{campaign.code} · mã {campaign.promotionCode}{campaign.createdByName ? ` · ${campaign.createdByName}` : ""}</p>
                  </td>
                  <td className="max-w-[260px] px-4 py-3 text-xs text-zinc-300">{audienceText(campaign)}</td>
                  <td className="max-w-[220px] px-4 py-3 text-xs text-zinc-300">{benefitText(campaign)}</td>
                  <td className="whitespace-nowrap px-4 py-3 text-xs text-zinc-300">
                    {campaign.sentAt ? formatTime(campaign.sentAt) : formatTime(campaign.scheduledAt)}
                    {campaign.recipientCount != null && <p className="text-zinc-500">{campaign.recipientCount.toLocaleString("vi-VN")} khách</p>}
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-xs text-zinc-300">
                    {usageText(campaign)}
                    {Number(campaign.usedRevenue) > 0 && <p className="text-zinc-500">Doanh thu {money(campaign.usedRevenue)}</p>}
                  </td>
                  <td className="px-4 py-3"><span className={`inline-flex whitespace-nowrap rounded-full border px-2.5 py-1 text-xs font-semibold ${CAMPAIGN_STATUS[campaign.status]?.style || ""}`}>{campaign.statusLabel}</span></td>
                  <td className="px-4 py-3">
                    {campaign.status === "SCHEDULED" && (
                      <Button type="button" variant="ghost" permission="PROMOTION_UPDATE" globalOnly className="text-sm text-rose-300" onClick={() => cancel(campaign)}>Hủy</Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pagination currentPage={page} totalPages={state.totalPages} onPageChange={setPage} />
      </section>

      {creating && <CampaignDialog onClose={() => setCreating(false)}
        onSaved={(campaign) => {
          setCreating(false);
          notification.success(campaign.status === "SENT"
            ? `Đã gửi voucher cho ${campaign.recipientCount.toLocaleString("vi-VN")} khách.`
            : `Đã hẹn gửi lúc ${formatTime(campaign.scheduledAt)}.`);
          setPage(1);
          setVersion((v) => v + 1);
        }} />}
    </div>
  );
}

function CampaignDialog({ onClose, onSaved }) {
  const notification = useSystemNotification();
  const [form, setForm] = useState(emptyCampaignForm);
  const [tiers, setTiers] = useState([]);
  const [categories, setCategories] = useState([]);
  // Kết quả xem trước gắn với tiêu chí đã dùng; tiêu chí đổi thì kết quả cũ không còn hiện.
  const [previewResult, setPreviewResult] = useState({ key: null, data: null });
  const [previewing, setPreviewing] = useState(false);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const set = (field) => (event) => {
    const value = event.target.type === "checkbox" ? event.target.checked : event.target.value;
    setForm((f) => ({ ...f, [field]: value }));
  };
  const criteriaKey = JSON.stringify(criteriaPayload(form));
  const preview = previewResult.key === criteriaKey ? previewResult.data : null;

  useEffect(() => {
    const controller = new AbortController();
    customerTierOptionsApi.list({ signal: controller.signal })
      .then((res) => setTiers(Array.isArray(res) ? res : res.content || []))
      .catch(() => {});
    categoryApi.list({ size: 500 }, { signal: controller.signal })
      .then((res) => setCategories((Array.isArray(res) ? res : res.content || []).slice().sort((a, b) => a.name.localeCompare(b.name, "vi"))))
      .catch(() => {});
    return () => controller.abort();
  }, []);

  const toggleTier = (id) => setForm((f) => ({ ...f, tierIds: f.tierIds.includes(id) ? f.tierIds.filter((t) => t !== id) : [...f.tierIds, id] }));

  const runPreview = async () => {
    setPreviewing(true);
    setError("");
    try {
      setPreviewResult({ key: criteriaKey, data: await voucherCampaignApi.preview(criteriaPayload(form)) });
    } catch (failure) {
      setError(failure.message);
    } finally {
      setPreviewing(false);
    }
  };

  const save = async () => {
    const problem = validateCampaignForm(form);
    setError(problem);
    if (problem) return;
    let count = preview?.count;
    if (count == null) {
      try {
        const result = await voucherCampaignApi.preview(criteriaPayload(form));
        setPreviewResult({ key: criteriaKey, data: result });
        count = result.count;
      } catch (failure) {
        setError(failure.message);
        return;
      }
    }
    if (form.sendMode === "now") {
      if (count === 0) { setError("Không có khách nào khớp nhóm này."); return; }
      if (!await notification.confirm(`Gửi voucher cho ${count.toLocaleString("vi-VN")} khách ngay bây giờ?`, { confirmText: "Gửi ngay" })) return;
    }
    setSaving(true);
    try {
      onSaved(await voucherCampaignApi.create(campaignPayload(form)));
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <AdminDialog open onClose={onClose} size="lg">
      <AdminDialogHeader title="Tạo chiến dịch voucher" description="Khách phải thỏa tất cả tiêu chí được chọn; không chọn tiêu chí nào là mọi khách đang hoạt động." onClose={onClose} />
      <AdminDialogBody className="space-y-5 text-sm">
        <label className="block space-y-1 text-zinc-400"><span>Tên chiến dịch</span>
          <input className={control} value={form.name} maxLength={150} onChange={set("name")} placeholder="Mời khách quay lại tháng 11" /></label>

        <fieldset className="space-y-3 rounded-2xl border border-zinc-800 p-4">
          <legend className="px-1 font-semibold text-zinc-200">Nhóm khách</legend>
          <div>
            <p className="mb-1.5 text-zinc-400">Hạng thành viên <span className="text-zinc-500">(bỏ trống = mọi hạng)</span></p>
            <div className="flex flex-wrap gap-2">
              {tiers.map((tier) => (
                <label key={tier.id} className={`inline-flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-1.5 ${form.tierIds.includes(tier.id) ? "border-amber-400 bg-amber-500/10 text-amber-200" : "border-zinc-700 text-zinc-300"}`}>
                  <input type="checkbox" className="accent-amber-400" checked={form.tierIds.includes(tier.id)} onChange={() => toggleTier(tier.id)} />{tier.name}
                </label>
              ))}
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <label className="inline-flex items-center gap-2 text-zinc-300">
              <input type="checkbox" className="accent-amber-400" checked={form.useInactive} onChange={set("useInactive")} />Lâu chưa mua: không có đơn trong
            </label>
            <input type="number" min="1" max="1095" className={`${control} w-24`} value={form.inactiveDays} disabled={!form.useInactive} onChange={set("inactiveDays")} />
            <span className="text-zinc-400">ngày (đã từng mua)</span>
          </div>
          <label className="block space-y-1 text-zinc-400"><span>Từng mua danh mục <span className="text-zinc-500">(gồm danh mục con)</span></span>
            <select className={control} value={form.categoryId} onChange={set("categoryId")}>
              <option value="">Không lọc theo danh mục</option>
              {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select>
          </label>
          <label className="inline-flex items-center gap-2 text-zinc-300">
            <input type="checkbox" className="accent-amber-400" checked={form.skipRecent} onChange={set("skipRecent")} />Bỏ qua khách đã nhận voucher chiến dịch trong 14 ngày
          </label>
          <div className="flex flex-wrap items-center gap-3 border-t border-zinc-800 pt-3">
            <Button type="button" variant="secondary" className="inline-flex items-center gap-2 rounded-xl px-3 py-2 text-sm" loading={previewing} onClick={runPreview}>
              <Users size={15} />Xem trước nhóm khách
            </Button>
            {preview && <span className="font-semibold text-amber-200">{preview.count.toLocaleString("vi-VN")} khách khớp</span>}
          </div>
          {preview?.sample?.length > 0 && (
            <ul className="max-h-40 overflow-y-auto rounded-xl border border-zinc-800 text-xs">
              {preview.sample.map((customer) => (
                <li key={customer.id} className="flex justify-between gap-2 border-b border-zinc-800 px-3 py-1.5 last:border-0">
                  <span>{customer.fullName || "Khách hàng"} <span className="text-zinc-500">· {customer.customerCode}</span></span>
                  <span className="text-zinc-500">{[customer.tierName, customer.phone].filter(Boolean).join(" · ")}</span>
                </li>
              ))}
              {preview.count > preview.sample.length && <li className="px-3 py-1.5 text-zinc-500">… và {(preview.count - preview.sample.length).toLocaleString("vi-VN")} khách khác</li>}
            </ul>
          )}
        </fieldset>

        <fieldset className="grid gap-3 rounded-2xl border border-zinc-800 p-4 sm:grid-cols-2">
          <legend className="px-1 font-semibold text-zinc-200">Voucher</legend>
          <label className="space-y-1 text-zinc-400"><span>Kiểu giảm</span>
            <select className={control} value={form.discountType} onChange={set("discountType")}>
              <option value="PERCENT">Phần trăm</option><option value="FIXED">Số tiền</option>
            </select></label>
          <label className="space-y-1 text-zinc-400"><span>{form.discountType === "PERCENT" ? "Giảm (%)" : "Giảm (đ)"}</span>
            <input type="number" min="1" className={control} value={form.discountValue} onChange={set("discountValue")} /></label>
          {form.discountType === "PERCENT" && (
            <label className="space-y-1 text-zinc-400"><span>Giảm tối đa (đ, không bắt buộc)</span>
              <input type="number" min="1" className={control} value={form.maxDiscount} onChange={set("maxDiscount")} /></label>
          )}
          <label className="space-y-1 text-zinc-400"><span>Đơn tối thiểu (đ, không bắt buộc)</span>
            <input type="number" min="0" className={control} value={form.minOrderValue} onChange={set("minOrderValue")} /></label>
          <label className="space-y-1 text-zinc-400"><span>Dùng được trong (ngày)</span>
            <input type="number" min="1" max="365" className={control} value={form.validDays} onChange={set("validDays")} /></label>
          <label className="space-y-1 text-zinc-400 sm:col-span-2"><span>Lời nhắn (không bắt buộc)</span>
            <textarea rows={2} maxLength={500} className={`${control} h-auto py-2`} value={form.message} onChange={set("message")}
              placeholder="Lâu rồi không gặp bạn! Ghé Lunaria xem bộ sưu tập mới nhé." /></label>
        </fieldset>

        <fieldset className="space-y-2 rounded-2xl border border-zinc-800 p-4">
          <legend className="px-1 font-semibold text-zinc-200">Thời điểm gửi</legend>
          <div className="flex flex-wrap items-center gap-4">
            <label className="inline-flex items-center gap-2"><input type="radio" className="accent-amber-400" name="sendMode" value="now" checked={form.sendMode === "now"} onChange={set("sendMode")} />Gửi ngay</label>
            <label className="inline-flex items-center gap-2"><input type="radio" className="accent-amber-400" name="sendMode" value="schedule" checked={form.sendMode === "schedule"} onChange={set("sendMode")} />Hẹn giờ</label>
            {form.sendMode === "schedule" && <input type="datetime-local" className={`${control} w-56`} value={form.scheduledAt} onChange={set("scheduledAt")} />}
          </div>
          {form.sendMode === "schedule" && <p className="text-xs text-zinc-500">Nhóm khách được tính lại đúng lúc gửi.</p>}
        </fieldset>

        {error && <p role="alert" className="text-rose-300">{error}</p>}
      </AdminDialogBody>
      <AdminDialogFooter>
        <Button type="button" variant="secondary" className="rounded-xl px-4 py-2.5 text-sm" onClick={onClose} disabled={saving}>Đóng</Button>
        <Button type="button" variant="primary" permission="PROMOTION_CREATE" globalOnly className="rounded-xl px-4 py-2.5 text-sm font-semibold" loading={saving} onClick={save}>
          {form.sendMode === "now" ? "Gửi voucher" : "Hẹn gửi"}
        </Button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}
