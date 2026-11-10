import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  AlertTriangle, ArrowLeft, Ban, CheckCircle2, Link2, Megaphone, Pencil, Plus, RotateCcw, Send, Trash2, Undo2, Users, XCircle,
} from "lucide-react";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import { useSystemNotification } from "../../../components/common/SystemNotification";
import {
  Badge, Empty, ErrorText, Loading, ReasonDialog, Section, Stat, btnDanger, btnGhost, btnPrimary, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { marketingApi } from "../../../api/admin/marketingApi";
import CampaignFormDialog from "./CampaignFormDialog";
import DispatchDialog from "./DispatchDialog";
import ExpenseDialog from "./ExpenseDialog";
import OfferLinkDialog from "./OfferLinkDialog";
import PerformancePanel from "./PerformancePanel";
import RecipientsDialog from "./RecipientsDialog";
import {
  CAMPAIGN_PHASE, CAMPAIGN_STATUS, CHANNELS, DISPATCH_STATUS, EXPENSE_CATEGORIES, HISTORY_LABEL, OBJECTIVES, OFFER_STATUS, OFFER_TYPES,
  budgetUsage, describeAudience, dispatchProgress, formatDate, formatDateTime, formatMoney, formatWindow, offerLabel,
} from "./marketingLogic";

/** Chi tiết chiến dịch: thông tin, ưu đãi liên kết, chi phí, lịch sử và các thao tác theo quyền của người xem. */
export default function MarketingCampaignDetail() {
  const { id } = useParams();
  const notification = useSystemNotification();
  const { data, error, loading, setData } = useLoader((signal) => marketingApi.detail(id, { signal }), [id]);
  const lookups = useLoader((signal) => marketingApi.lookups({ signal }), []);
  const [dialog, setDialog] = useState(null);
  const [actionError, setActionError] = useState("");
  const [busy, setBusy] = useState(false);

  const run = async (action, message) => {
    setBusy(true);
    setActionError("");
    try {
      setData(await action());
      if (message) notification.success(message);
    } catch (e) {
      setActionError(e.message);
    } finally {
      setBusy(false);
    }
  };
  /** Dùng trong dialog: lỗi được dialog hiển thị, thành công thì đóng dialog. */
  const confirm = async (action, message) => {
    setData(await action());
    setDialog(null);
    if (message) notification.success(message);
  };
  const ask = async (title, message, action, success) => {
    if (await notification.confirm({ title, message, confirmText: title, cancelText: "Không", destructive: true })) run(action, success);
  };

  const back = <Link to="/admin/marketing/campaigns" className="inline-flex items-center gap-1.5 text-sm text-zinc-400 hover:text-zinc-200"><ArrowLeft size={15} />Chiến dịch marketing</Link>;
  if (!data) {
    return <div className="admin-catalog-page space-y-5 text-zinc-100">{back}<ErrorText>{error}</ErrorText>{loading && <Loading />}</div>;
  }

  const { campaign: c, offers, dispatches, expenses, history, actions, warnings } = data;
  const usage = budgetUsage(c.expenseTotal, c.plannedBudget);
  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
      {back}
      <AdminCatalogPageHeader icon={Megaphone} eyebrow={c.code} title={c.name}
        description={`${OBJECTIVES[c.objective]?.label || c.objective} · ${formatWindow(c.startsAt, c.endsAt)}`}
        action={<div className="flex flex-wrap gap-1.5"><Badge map={CAMPAIGN_STATUS} value={c.status} />{c.phase && <Badge map={CAMPAIGN_PHASE} value={c.phase} />}</div>} />
      <ErrorText>{actionError}</ErrorText>

      {c.status === "REJECTED" && c.decisionNote && (
        <p className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">Bị từ chối: {c.decisionNote}</p>
      )}
      {c.status === "CANCELLED" && c.cancelReason && (
        <p className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">Đã hủy: {c.cancelReason}</p>
      )}
      {warnings.length > 0 && (
        <ul className="space-y-2" aria-label="Cảnh báo">
          {warnings.map((w) => (
            <li key={w} className="flex items-start gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
              <AlertTriangle size={16} className="mt-0.5 shrink-0" /><span className="break-words">{w}</span>
            </li>
          ))}
        </ul>
      )}

      <div className="flex flex-wrap gap-2">
        {actions.edit && <button type="button" className={btnGhost} disabled={busy} onClick={() => setDialog("edit")}><Pencil size={15} />Sửa</button>}
        {actions.submit && <button type="button" className={btnPrimary} disabled={busy} onClick={() => run(() => marketingApi.submit(id), "Đã gửi duyệt chiến dịch.")}><Send size={15} />Gửi duyệt</button>}
        {actions.withdraw && <button type="button" className={btnGhost} disabled={busy} onClick={() => run(() => marketingApi.withdraw(id), "Đã rút lại về Nháp.")}><Undo2 size={15} />Rút lại</button>}
        {actions.approve && <button type="button" className={btnPrimary} disabled={busy} onClick={() => setDialog("approve")}><CheckCircle2 size={16} />Duyệt</button>}
        {actions.reject && <button type="button" className={btnGhost} disabled={busy} onClick={() => setDialog("reject")}><XCircle size={15} />Từ chối</button>}
        {actions.reopen && <button type="button" className={btnGhost} disabled={busy} onClick={() => setDialog("reopen")}><RotateCcw size={15} />Mở lại để sửa</button>}
        {actions.cancel && <button type="button" className={btnDanger} disabled={busy} onClick={() => setDialog("cancel")}><Ban size={15} />Hủy chiến dịch</button>}
      </div>
      {c.status === "PENDING_APPROVAL" && actions.reject && !actions.approve && (
        <p className="text-xs text-zinc-500">Bạn không thể tự duyệt chiến dịch do mình tạo hoặc gửi duyệt; người có quyền duyệt khác sẽ duyệt.</p>
      )}

      <div className="grid gap-5 xl:grid-cols-[3fr_2fr] [&>*]:min-w-0">
        <div className="space-y-5 [&>*]:min-w-0">
          {actions.viewFinancials && (
            <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
              <Stat label="Ngân sách dự kiến" value={formatMoney(c.plannedBudget)} />
              <Stat label="Chi phí thực tế" value={formatMoney(c.expenseTotal)} tone={usage !== null && usage > 100 ? "rose" : "amber"}
                hint={usage !== null ? `${usage}% ngân sách` : "Chưa đặt ngân sách"} />
              <Stat label="Chỉ tiêu doanh thu" value={formatMoney(c.targetRevenue)} tone="emerald" />
              <Stat label="Chỉ tiêu đơn / khách" value={`${c.targetOrders ?? "—"} / ${c.targetBuyers ?? "—"}`} tone="sky" />
            </div>
          )}

          {actions.viewFinancials && <PerformancePanel campaignId={id} campaignCode={c.code} />}

          <Section title={`Ưu đãi liên kết (${offers.length})`}
            action={actions.linkOffers && <button type="button" className={btnGhost} disabled={busy} onClick={() => setDialog("link")}><Link2 size={15} />Liên kết ưu đãi</button>}>
            {offers.length === 0 ? <Empty>Chưa liên kết ưu đãi nào.</Empty> : (
              <ul className="space-y-2">
                {offers.map((o) => (
                  <li key={o.id} className="flex flex-wrap items-start justify-between gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/60 px-3 py-2.5">
                    <div className="min-w-0 flex-1">
                      <p className="text-xs text-zinc-500">{OFFER_TYPES[o.type]?.label}</p>
                      <p className="break-words text-sm font-semibold text-zinc-100">{offerLabel(o)}</p>
                      <p className="text-xs text-zinc-400">{o.detail}</p>
                      <p className={`text-xs ${o.outsideWindow ? "text-amber-300" : "text-zinc-500"}`}>
                        {formatWindow(o.validFrom, o.validTo)}{o.outsideWindow ? " · Ngoài thời gian chiến dịch" : ""}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge map={OFFER_STATUS} value={o.itemStatus} />
                      {actions.unlinkOffers && (
                        <button type="button" className={btnGhost} disabled={busy} aria-label={`Gỡ ${offerLabel(o)}`}
                          onClick={() => ask("Gỡ ưu đãi", `Gỡ ${offerLabel(o)} khỏi chiến dịch? Ưu đãi vẫn hoạt động bình thường.`,
                            () => marketingApi.unlink(id, o.id), "Đã gỡ ưu đãi.")}><Trash2 size={15} /></button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            )}
            {actions.linkOffers && <p className="mt-3 text-xs text-zinc-500">Cần ưu đãi mới? Tạo ở màn hình Khuyến mãi, Chiến dịch voucher hoặc Flash sale rồi quay lại liên kết.</p>}
          </Section>

          {actions.viewHistory && (
            <Section title={`Đợt gửi thông báo (${dispatches.length})`}
              action={actions.addDispatch && <button type="button" className={btnGhost} disabled={busy} onClick={() => setDialog({ dispatch: null })}><Plus size={15} />Thêm đợt gửi</button>}>
              {dispatches.length === 0 ? <Empty>Chưa có đợt gửi nào.</Empty> : (
                <ul className="space-y-2">
                  {dispatches.map((d) => {
                    const progress = dispatchProgress(d);
                    return (
                      <li key={d.id} className="space-y-2 rounded-2xl border border-zinc-800 bg-zinc-950/60 px-3 py-2.5">
                        <div className="flex flex-wrap items-start justify-between gap-2">
                          <div className="min-w-0 flex-1">
                            <p className="text-xs text-zinc-500">{d.code} · {CHANNELS[d.channel]?.label || d.channel} · {formatDateTime(d.scheduledAt)}{d.holdoutPercent > 0 ? ` · đối chứng ${d.holdoutPercent}%` : ""}</p>
                            <p className="break-words text-sm font-semibold text-zinc-100">{d.title}</p>
                            <p className="break-words text-xs text-zinc-400">{d.message}</p>
                            <p className="text-xs text-zinc-500">{describeAudience(d.audience)}{d.actionUrl ? ` · mở ${d.actionUrl}` : ""}</p>
                            {d.outsideWindow && <p className="text-xs text-amber-300">Giờ gửi nằm ngoài khoảng cho phép</p>}
                            {d.cancelReason && <p className="break-words text-xs text-rose-300">{d.cancelReason}</p>}
                          </div>
                          <Badge map={DISPATCH_STATUS} value={d.status} />
                        </div>
                        {d.audienceCount !== null && (
                          <div className="space-y-1">
                            <div className="h-1.5 overflow-hidden rounded-full bg-zinc-800" role="progressbar" aria-valuenow={progress ?? 0} aria-valuemin={0} aria-valuemax={100}>
                              <div className="h-full bg-amber-400" style={{ width: `${progress ?? 0}%` }} />
                            </div>
                            <p className="text-xs tabular-nums text-zinc-400">
                              Chốt {d.audienceCount} · đã gửi {d.sent}{d.channel === "IN_APP" ? ` · đã đọc ${d.read}` : ""} · bỏ qua {d.skipped} · lỗi {d.failed}{d.holdout ? ` · đối chứng ${d.holdout}` : ""}{d.pending ? ` · đang chờ ${d.pending}` : ""}
                            </p>
                          </div>
                        )}
                        <div className="flex flex-wrap gap-1.5">
                          {d.audienceCount !== null && <button type="button" className={btnGhost} onClick={() => setDialog({ recipients: d })}><Users size={14} />Người nhận</button>}
                          {d.actions.edit && <button type="button" className={btnGhost} disabled={busy} onClick={() => setDialog({ dispatch: d })}><Pencil size={14} />Sửa</button>}
                          {d.actions.approve && <button type="button" className={btnPrimary} disabled={busy} onClick={() => run(() => marketingApi.approveDispatch(id, d.id), "Đã duyệt đợt gửi.")}><CheckCircle2 size={14} />Duyệt</button>}
                          {d.actions.reject && <button type="button" className={btnGhost} disabled={busy} onClick={() => setDialog({ rejectDispatch: d })}><XCircle size={14} />Từ chối</button>}
                          {d.actions.retry && <button type="button" className={btnGhost} disabled={busy} onClick={() => run(() => marketingApi.retryDispatch(id, d.id), "Đang gửi lại cho người nhận lỗi.")}><RotateCcw size={14} />Gửi lại người lỗi</button>}
                          {d.actions.cancel && <button type="button" className={btnDanger} disabled={busy} onClick={() => setDialog({ cancelDispatch: d })}><Ban size={14} />Hủy đợt</button>}
                        </div>
                      </li>
                    );
                  })}
                </ul>
              )}
              <p className="mt-3 text-xs text-zinc-500">Danh sách người nhận được chốt đúng lúc bắt đầu gửi. Email chỉ tới khách đã đồng ý nhận email ưu đãi; mỗi email có link hủy đăng ký.</p>
            </Section>
          )}

          {actions.viewFinancials && (
            <Section title={`Chi phí thực tế (${expenses.length})`}
              action={actions.manageExpenses && <button type="button" className={btnGhost} disabled={busy} onClick={() => setDialog({ expense: null })}><Plus size={15} />Thêm chi phí</button>}>
              {expenses.length === 0 ? <Empty>Chưa có khoản chi nào.</Empty> : (
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[560px] text-sm">
                    <thead className="text-left text-xs text-zinc-400">
                      <tr className="border-b border-zinc-800">
                        <th className="py-2 pr-3 font-medium">Khoản chi</th>
                        <th className="py-2 pr-3 font-medium">Ngày</th>
                        <th className="py-2 pr-3 text-right font-medium">Số tiền</th>
                        {actions.manageExpenses && <th className="py-2 font-medium"><span className="sr-only">Thao tác</span></th>}
                      </tr>
                    </thead>
                    <tbody>
                      {expenses.map((e) => (
                        <tr key={e.id} className="border-b border-zinc-800/70">
                          <td className="py-2 pr-3">
                            <p className="text-zinc-100">{EXPENSE_CATEGORIES[e.category] || e.category}{e.vendor ? ` · ${e.vendor}` : ""}</p>
                            {e.note && <p className="break-words text-xs text-zinc-500">{e.note}</p>}
                          </td>
                          <td className="py-2 pr-3 text-zinc-300">{formatDate(e.spentOn)}</td>
                          <td className="py-2 pr-3 text-right tabular-nums text-zinc-100">{formatMoney(e.amount)}</td>
                          {actions.manageExpenses && (
                            <td className="py-2 text-right">
                              <div className="inline-flex gap-1">
                                <button type="button" className={btnGhost} disabled={busy} aria-label="Sửa khoản chi" onClick={() => setDialog({ expense: e })}><Pencil size={14} /></button>
                                <button type="button" className={btnGhost} disabled={busy} aria-label="Xóa khoản chi"
                                  onClick={() => ask("Xóa khoản chi", `Xóa ${EXPENSE_CATEGORIES[e.category] || e.category} ${formatMoney(e.amount)}?`,
                                    () => marketingApi.deleteExpense(id, e.id), "Đã xóa khoản chi.")}><Trash2 size={14} /></button>
                              </div>
                            </td>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Section>
          )}
        </div>

        <div className="space-y-5 [&>*]:min-w-0">
          <Section title="Thông tin">
            <dl className="space-y-2.5 text-sm">
              <Info label="Mục tiêu">{OBJECTIVES[c.objective]?.label || c.objective}</Info>
              <Info label="Phạm vi">{c.storeName ? `Cửa hàng ${c.storeName}` : "Toàn chuỗi"}</Info>
              <Info label="Bắt đầu">{formatDateTime(c.startsAt)}</Info>
              <Info label="Kết thúc">{formatDateTime(c.endsAt)}</Info>
              <Info label="Phụ trách">{c.ownerName || "—"}</Info>
              <Info label="Người tạo">{c.createdByName || "—"}</Info>
              {c.submittedAt && <Info label="Gửi duyệt">{formatDateTime(c.submittedAt)} · {c.submittedByName}</Info>}
              {c.decidedAt && <Info label={c.status === "REJECTED" ? "Từ chối" : "Duyệt"}>{formatDateTime(c.decidedAt)} · {c.decidedByName}</Info>}
              {c.cancelledAt && <Info label="Hủy">{formatDateTime(c.cancelledAt)} · {c.cancelledByName}</Info>}
            </dl>
            {c.description && <p className="mt-4 whitespace-pre-wrap break-words border-t border-zinc-800 pt-4 text-sm text-zinc-200">{c.description}</p>}
          </Section>
          {actions.viewHistory && (
            <Section title="Lịch sử">
              <ol className="relative space-y-4 border-l border-zinc-800 pl-5">
                {history.map((h) => (
                  <li key={h.id} className="relative">
                    <span className="absolute -left-[26px] top-1.5 h-2.5 w-2.5 rounded-full bg-amber-400" />
                    <p className="text-sm text-zinc-100">{HISTORY_LABEL[h.action] || h.action}</p>
                    {h.note && <p className="mt-0.5 break-words text-xs text-zinc-400">{h.note}</p>}
                    <p className="mt-0.5 text-xs text-zinc-500">{formatDateTime(h.createdAt)} · {h.actorName || "Hệ thống"}</p>
                  </li>
                ))}
              </ol>
            </Section>
          )}
        </div>
      </div>

      {dialog === "edit" && <CampaignFormDialog campaign={c} owners={lookups.data?.owners || []} stores={lookups.data?.stores || []}
        ownStoreName={lookups.data?.storeName} onClose={() => setDialog(null)}
        onSaved={(detail) => { setData(detail); setDialog(null); notification.success("Đã lưu chiến dịch."); }} />}
      {dialog === "link" && <OfferLinkDialog campaignId={id} onClose={() => setDialog(null)}
        onLinked={(detail) => { setData(detail); setDialog(null); notification.success("Đã liên kết ưu đãi."); }} />}
      {dialog && typeof dialog === "object" && "expense" in dialog && (
        <ExpenseDialog campaignId={id} expense={dialog.expense} onClose={() => setDialog(null)}
          onSaved={(detail) => { setData(detail); setDialog(null); notification.success("Đã lưu chi phí."); }} />
      )}
      {dialog && typeof dialog === "object" && "dispatch" in dialog && (
        <DispatchDialog campaign={c} dispatch={dialog.dispatch} tiers={lookups.data?.tiers || []} categories={lookups.data?.categories || []}
          approved={c.status === "APPROVED"} onClose={() => setDialog(null)}
          onSaved={(detail) => { setData(detail); setDialog(null); notification.success("Đã lưu đợt gửi."); }} />
      )}
      {dialog?.recipients && <RecipientsDialog campaignId={id} dispatch={dialog.recipients} onClose={() => setDialog(null)} />}
      {dialog?.rejectDispatch && (
        <ReasonDialog title={`Từ chối đợt ${dialog.rejectDispatch.code}`} description="Đợt gửi sẽ bị hủy; người tạo có thể thêm đợt mới."
          confirmText="Từ chối" onClose={() => setDialog(null)}
          onConfirm={(reason) => confirm(() => marketingApi.rejectDispatch(id, dialog.rejectDispatch.id, reason), "Đã từ chối đợt gửi.")} />
      )}
      {dialog?.cancelDispatch && (
        <ReasonDialog title={`Hủy đợt ${dialog.cancelDispatch.code}`} danger confirmText="Hủy đợt gửi"
          description="Người chưa nhận sẽ không được gửi nữa; người đã nhận vẫn giữ thông báo."
          onClose={() => setDialog(null)}
          onConfirm={(reason) => confirm(() => marketingApi.cancelDispatch(id, dialog.cancelDispatch.id, reason), "Đã hủy đợt gửi.")} />
      )}
      {dialog === "approve" && (
        <ReasonDialog title="Duyệt chiến dịch" label="Ghi chú" required={false} confirmText="Duyệt"
          description="Sau khi duyệt, chiến dịch sẽ chạy theo thời gian đã đặt. Người tạo và người phụ trách nhận được thông báo."
          onClose={() => setDialog(null)} onConfirm={(note) => confirm(() => marketingApi.approve(id, note), "Đã duyệt chiến dịch.")} />
      )}
      {dialog === "reject" && (
        <ReasonDialog title="Từ chối chiến dịch" description="Người tạo sẽ nhận được lý do để sửa và gửi duyệt lại (ít nhất 5 ký tự)."
          confirmText="Từ chối" onClose={() => setDialog(null)} onConfirm={(reason) => confirm(() => marketingApi.reject(id, reason), "Đã từ chối chiến dịch.")} />
      )}
      {dialog === "reopen" && (
        <ReasonDialog title="Mở lại để sửa" description="Chiến dịch quay về Nháp và phải được duyệt lại trước khi chạy."
          confirmText="Mở lại" onClose={() => setDialog(null)} onConfirm={(reason) => confirm(() => marketingApi.reopen(id, reason), "Chiến dịch đã về Nháp.")} />
      )}
      {dialog === "cancel" && (
        <ReasonDialog title="Hủy chiến dịch" danger confirmText="Hủy chiến dịch"
          description="Khuyến mãi, voucher và flash sale đã liên kết KHÔNG bị tắt. Sau khi hủy, hệ thống sẽ liệt kê các ưu đãi còn hiệu lực để bạn tự xử lý."
          onClose={() => setDialog(null)} onConfirm={(reason) => confirm(() => marketingApi.cancel(id, reason), "Đã hủy chiến dịch.")} />
      )}
    </div>
  );
}

function Info({ label, children }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <dt className="shrink-0 text-zinc-400">{label}</dt>
      <dd className="min-w-0 break-words text-right text-zinc-100">{children}</dd>
    </div>
  );
}
