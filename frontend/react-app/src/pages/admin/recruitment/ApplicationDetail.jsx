import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, CalendarPlus, ChevronRight, ClipboardCheck, FileSignature, Mail, MessageSquarePlus, Upload, UserCheck, Users } from "lucide-react";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { useSystemNotification } from "../../../components/common/SystemNotification";
import {
  Badge, DocumentList, ErrorText, Loading, ReasonDialog, Section, StageStepper, Timeline, UploadDialog, btnDanger, btnGhost,
  btnPrimary, control, useLoader, useRecruitment,
} from "../../../components/admin/recruitment/RecruitmentUi";
import {
  APPLICATION_STATUS, INTERVIEW_MODES, INTERVIEW_STATUS, OFFER_STATUS, RECOMMENDATIONS, STAGE_STATUS, formatDate,
  formatDateTime, labelOf, money, passRuleText,
} from "./recruitmentLogic";
import { EvaluatorsDialog, HireDialog, InterviewDialog, OfferDialog } from "./ApplicationDialogs";
import { ComposeEmailDialog, EmailLogList } from "./EmailComponents";

const statusLabel = (s) => APPLICATION_STATUS[s]?.label || STAGE_STATUS[s]?.label || OFFER_STATUS[s]?.label || INTERVIEW_STATUS[s]?.label || s;

/** Chi tiết hồ sơ ứng tuyển: tiến độ từng vòng, trạng thái tổng thể, phân công, phỏng vấn, offer, tài liệu, lịch sử. */
export default function ApplicationDetail() {
  const { id } = useParams();
  const notification = useSystemNotification();
  const { sourceLabel } = useRecruitment();
  const { data, error, loading, setData, reload } = useLoader((signal) => recruitmentApi.application(id, { signal }), [id]);
  const [selectedStage, setSelectedStage] = useState(null);
  const [dialog, setDialog] = useState(null);
  const [statusTarget, setStatusTarget] = useState("");

  if (loading && !data) return <Loading />;
  if (!data) return <ErrorText>{error}</ErrorText>;
  const app = data.application;
  const stageId = selectedStage || app.currentStageId || data.stages[0]?.stage.id;
  const progress = data.stages.find((s) => s.stage.id === stageId);
  const closed = ["REJECTED", "WITHDRAWN", "HIRED", "OFFER_DECLINED"].includes(app.status);
  const current = data.stages.find((s) => s.stage.id === app.currentStageId);
  const canScore = Boolean(progress?.canEvaluate && !closed && !["PASSED", "FAILED", "SKIPPED"].includes(progress.status)
    && progress.myEvaluationStatus !== "SUBMITTED");
  const activeOffer = data.offers.find((o) => ["DRAFT", "PENDING_APPROVAL", "APPROVED", "SENT", "ACCEPTED"].includes(o.status));

  const run = async (action, message) => {
    try {
      const result = await action();
      if (result?.application) setData(result); else reload();
      if (message) notification.success(message);
      setDialog(null);
    } catch (e) {
      notification.error(e.message);
      throw e;
    }
  };

  const needsReason = (s) => ["REJECTED", "WITHDRAWN"].includes(s) || closed;

  return (
    <div className="space-y-5">
      <Link to={`/admin/recruitment/positions/${app.positionId}`} className="inline-flex items-center gap-1.5 text-sm text-zinc-400 hover:text-white">
        <ArrowLeft size={14} />{app.positionTitle}
      </Link>

      <Section>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-1">
            <div className="flex flex-wrap items-center gap-2">
              <Link to={`/admin/recruitment/candidates/${app.candidateId}`} className="text-xl font-semibold hover:text-amber-200">{app.candidateName}</Link>
              <Badge map={APPLICATION_STATUS} value={app.status} />
            </div>
            <p className="text-sm text-zinc-400">
              {app.code} · {app.positionTitle} · {app.campaignName} · Nguồn: {sourceLabel(app.source)} · Nộp {formatDate(app.appliedAt)}
            </p>
            <p className="text-sm text-zinc-400">{[app.candidateEmail, app.candidatePhone].filter(Boolean).join(" · ")}</p>
            {data.closedReason && <p className="text-sm text-rose-300">Lý do: {data.closedReason}</p>}
            {data.hiredEmployeeId && (
              <p className="text-sm text-emerald-300">
                Đã chuyển thành nhân viên {data.hiredEmployeeCode} lúc {formatDateTime(data.hiredAt)} ·{" "}
                <Link to="/admin/employees" className="underline">Danh sách nhân viên</Link>
              </p>
            )}
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {data.canManage && data.allowedStatuses.length > 0 && (
              <>
                <select className={`${control} w-56`} value={statusTarget} onChange={(e) => setStatusTarget(e.target.value)} aria-label="Đổi trạng thái">
                  <option value="">Đổi trạng thái…</option>
                  {data.allowedStatuses.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
                <button type="button" className={btnGhost} disabled={!statusTarget}
                  onClick={() => (needsReason(statusTarget) ? setDialog({ type: "status" })
                    : run(() => recruitmentApi.applicationStatus(id, statusTarget), "Đã đổi trạng thái").then(() => setStatusTarget("")).catch(() => {}))}>
                  Áp dụng
                </button>
              </>
            )}
            {data.canManage && app.status === "OFFER_ACCEPTED" && (
              <button type="button" className={btnPrimary} onClick={() => setDialog({ type: "hire" })}><UserCheck size={15} />Chuyển thành nhân viên</button>
            )}
            <button type="button" className={btnGhost} onClick={() => setDialog({ type: "note" })}><MessageSquarePlus size={14} />Ghi chú</button>
          </div>
        </div>
      </Section>

      {data.stages.length > 0 ? (
        <Section title="Tiến độ quy trình">
          <StageStepper stages={data.stages} currentStageId={app.currentStageId} selectedId={stageId} onSelect={setSelectedStage} />
          {data.canManage && !closed && (
            <div className="mt-4 flex flex-wrap gap-2">
              {current && ["PASSED", "SKIPPED"].includes(current.status) && (
                <button type="button" className={btnPrimary} onClick={() => run(() => recruitmentApi.advance(id), "Đã chuyển sang vòng tiếp theo").catch(() => {})}>
                  Chuyển vòng tiếp theo <ChevronRight size={15} />
                </button>
              )}
              {progress && progress.stage.id !== app.currentStageId && (
                <button type="button" className={btnGhost} onClick={() => setDialog({ type: "move" })}>Chuyển hồ sơ tới vòng "{progress.stage.name}"</button>
              )}
            </div>
          )}
        </Section>
      ) : (
        <Section><p className="text-sm text-amber-200">Vị trí chưa cấu hình vòng tuyển dụng. <Link className="underline" to={`/admin/recruitment/positions/${app.positionId}`}>Cấu hình quy trình</Link></p></Section>
      )}

      <div className="grid gap-5 xl:grid-cols-3">
        <div className="space-y-5 xl:col-span-2">
          {progress && (
            <Section title={`Vòng: ${progress.stage.name}`} action={<Badge map={STAGE_STATUS} value={progress.status} />}>
              <p className="text-sm text-zinc-400">
                {passRuleText(progress.stage)}{progress.stage.ownerName ? ` · Phụ trách: ${progress.stage.ownerName}` : ""}
                {progress.score != null ? ` · Điểm tổng hợp: ${Number(progress.score)}/${Number(progress.stage.scaleMax)}` : ""}
              </p>
              {progress.decidedAt && (
                <p className="mt-1 text-sm text-zinc-400">Kết luận {formatDateTime(progress.decidedAt)} bởi {progress.decidedByName || "hệ thống"}{progress.decisionNote ? ` – ${progress.decisionNote}` : ""}</p>
              )}
              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="text-xs text-zinc-400"><tr>{["Người đánh giá", "Phiếu", "Điểm", "Đề xuất", "Nộp lúc"].map((h) => <th key={h} className="px-2 py-2 text-left font-medium">{h}</th>)}</tr></thead>
                  <tbody>
                    {progress.evaluators.length === 0 && <tr><td colSpan="5" className="px-2 py-4 text-zinc-500">Chưa phân công người đánh giá.</td></tr>}
                    {progress.evaluators.map((e) => (
                      <tr key={e.userId} className="border-t border-zinc-800">
                        <td className="px-2 py-2 text-zinc-100">{e.fullName}</td>
                        <td className="px-2 py-2 text-xs text-zinc-300">{{ SUBMITTED: "Đã nộp", DRAFT: "Đang chấm", NOT_STARTED: "Chưa chấm" }[e.evaluationStatus]}</td>
                        <td className="px-2 py-2 tabular-nums">{e.totalScore != null ? Number(e.totalScore) : "—"}</td>
                        <td className="px-2 py-2">{e.recommendation ? <Badge tone={RECOMMENDATIONS.find((r) => r.value === e.recommendation)?.tone}>{labelOf(RECOMMENDATIONS, e.recommendation)}</Badge> : "—"}</td>
                        <td className="px-2 py-2 text-xs text-zinc-400">{formatDateTime(e.submittedAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                {(progress.canEvaluate || data.canManage || data.canApprove) && progress.status !== "NOT_REACHED" && (
                  <Link className={canScore ? btnPrimary : btnGhost} to={`/admin/recruitment/applications/${id}/evaluate/${progress.stage.id}`}>
                    <ClipboardCheck size={15} />{canScore ? "Chấm phiếu của tôi" : "Xem phiếu đánh giá"}
                  </Link>
                )}
                {data.canManage && !closed && progress.status !== "NOT_REACHED" && (
                  <button type="button" className={btnGhost} onClick={() => setDialog({ type: "evaluators" })}><Users size={14} />Phân công đánh giá</button>
                )}
                {(data.canManage || data.canApprove) && !closed && progress.status !== "NOT_REACHED" && (
                  <>
                    <button type="button" className={btnGhost} onClick={() => setDialog({ type: "decide", decision: "PASSED" })}>Kết luận Đạt</button>
                    <button type="button" className={btnDanger} onClick={() => setDialog({ type: "decide", decision: "FAILED" })}>Kết luận Không đạt</button>
                    {progress.stage.id === app.currentStageId && !["PASSED", "FAILED", "SKIPPED"].includes(progress.status) && (
                      <button type="button" className={btnGhost} onClick={() => setDialog({ type: "decide", decision: "SKIPPED" })}>Bỏ qua vòng</button>
                    )}
                  </>
                )}
              </div>
            </Section>
          )}

          <Section title="Lịch phỏng vấn" action={data.canManage && !closed && data.stages.length > 0 && (
            <button type="button" className={btnGhost} onClick={() => setDialog({ type: "interview" })}><CalendarPlus size={14} />Đặt lịch</button>)}>
            {data.interviews.length === 0 ? <p className="text-sm text-zinc-500">Chưa có lịch phỏng vấn.</p> : (
              <ul className="space-y-2">
                {data.interviews.map((i) => (
                  <li key={i.id} className="rounded-2xl border border-zinc-800 bg-zinc-950/50 p-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className="text-sm font-semibold text-zinc-100">{i.title} <span className="font-normal text-zinc-400">· {i.stageName}</span></p>
                      <Badge map={INTERVIEW_STATUS} value={i.status} />
                    </div>
                    <p className="mt-1 text-xs text-zinc-400">
                      {formatDateTime(i.startsAt)} – {formatDateTime(i.endsAt).split(" ").pop()} · {labelOf(INTERVIEW_MODES, i.mode)}
                      {i.location ? ` · ${i.location}` : ""}{i.meetingUrl && <> · <a href={i.meetingUrl} target="_blank" rel="noreferrer" className="text-amber-300 underline">Link họp</a></>}
                    </p>
                    <p className="text-xs text-zinc-400">Người phỏng vấn: {i.participants.map((u) => u.fullName).join(", ")}</p>
                    {i.resultNote && <p className="mt-1 text-xs text-zinc-300">Kết quả: {i.resultNote}</p>}
                    {i.status === "SCHEDULED" && (
                      <div className="mt-2 flex flex-wrap gap-2">
                        <button type="button" className={btnGhost} onClick={() => setDialog({ type: "interviewResult", interview: i, status: "COMPLETED" })}>Đã phỏng vấn</button>
                        <button type="button" className={btnGhost} onClick={() => setDialog({ type: "interviewResult", interview: i, status: "NO_SHOW" })}>Không đến</button>
                        {data.canManage && <button type="button" className={btnGhost} onClick={() => setDialog({ type: "interview", interview: i })}>Dời lịch</button>}
                        {data.canManage && <button type="button" className={btnDanger} onClick={() => setDialog({ type: "interviewResult", interview: i, status: "CANCELLED" })}>Hủy</button>}
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </Section>

          {(data.canManage || data.canApprove) && (
            <Section title="Offer" action={data.canManage && !activeOffer && ["QUALIFIED", "ROUND_PASSED", "OFFER_PENDING", "OFFER_DECLINED"].includes(app.status) && (
              <button type="button" className={btnPrimary} onClick={() => setDialog({ type: "offer" })}><FileSignature size={15} />Tạo offer</button>)}>
              {data.offers.length === 0 ? <p className="text-sm text-zinc-500">Chưa có offer. Hồ sơ cần đạt yêu cầu / đạt vòng trước khi tạo offer.</p> : (
                <ul className="space-y-2">
                  {data.offers.map((o) => <OfferCard key={o.id} offer={o} onEdit={() => setDialog({ type: "offer", offer: o })}
                    onAction={(action, body, message) => run(() => recruitmentApi.offerAction(o.id, action, body), message)}
                    onReason={(action, title, label) => setDialog({ type: "offerReason", offer: o, action, title, label })} />)}
                </ul>
              )}
            </Section>
          )}
        </div>

        <div className="space-y-5">
          {(data.canManage || data.canApprove) && (
            <Section title="Email ứng viên" action={data.canManage && (
              <button type="button" className={btnGhost} onClick={() => setDialog({ type: "email" })}><Mail size={14} />Soạn email</button>)}>
              <EmailLogList applicationId={id} emails={data.emails} canManage={data.canManage} onChanged={reload} />
            </Section>
          )}
          <Section title="Tài liệu" action={data.canManage && (
            <button type="button" className={btnGhost} onClick={() => setDialog({ type: "upload", docType: "CV" })}><Upload size={14} />Tải lên</button>)}>
            <DocumentList candidateId={app.candidateId} documents={data.documents} canManage={data.canManage}
              onChanged={reload} onUpload={(docType) => setDialog({ type: "upload", docType })} />
          </Section>
          {data.coverNote && <Section title="Thư ứng tuyển"><p className="whitespace-pre-line text-sm text-zinc-300">{data.coverNote}</p></Section>}
          <Section title="Lịch sử hồ sơ"><Timeline entries={data.history} statusLabel={statusLabel} /></Section>
        </div>
      </div>

      {dialog?.type === "status" && (
        <ReasonDialog title={`Chuyển sang "${APPLICATION_STATUS[statusTarget]?.label}"`} danger={["REJECTED", "WITHDRAWN"].includes(statusTarget)}
          checkbox={statusTarget === "REJECTED" ? { label: "Gửi email thông báo kết quả cho ứng viên", hint: "Dùng mẫu \"Thông báo kết quả\"; lý do nội bộ không được gửi.", defaultChecked: true } : null}
          onClose={() => setDialog(null)}
          onConfirm={(reason, notify) => run(() => recruitmentApi.applicationStatus(id, statusTarget, reason, notify), "Đã đổi trạng thái").then(() => setStatusTarget(""))} />
      )}
      {dialog?.type === "note" && (
        <ReasonDialog title="Thêm ghi chú" label="Ghi chú" confirmText="Lưu ghi chú" onClose={() => setDialog(null)}
          onConfirm={(message) => run(() => recruitmentApi.addNote(id, message), "Đã thêm ghi chú")} />
      )}
      {dialog?.type === "move" && (
        <ReasonDialog title={`Chuyển tới vòng "${progress.stage.name}"`} description="Các vòng ở giữa chưa có kết quả sẽ được đánh dấu Bỏ qua." label="Ghi chú" required={false}
          confirmText="Chuyển vòng" onClose={() => setDialog(null)} onConfirm={(note) => run(() => recruitmentApi.moveStage(id, progress.stage.id, note), "Đã chuyển vòng")} />
      )}
      {dialog?.type === "decide" && (
        <ReasonDialog title={`Kết luận vòng "${progress.stage.name}": ${STAGE_STATUS[dialog.decision].label}`}
          description="Kết luận thủ công ghi đè kết quả tự động và được lưu vào lịch sử." label={dialog.decision === "FAILED" ? "Lý do" : "Ghi chú"}
          required={dialog.decision === "FAILED"} danger={dialog.decision === "FAILED"} onClose={() => setDialog(null)}
          onConfirm={(note) => run(() => recruitmentApi.decideStage(id, progress.stage.id, dialog.decision, note), "Đã kết luận vòng")} />
      )}
      {dialog?.type === "evaluators" && (
        <EvaluatorsDialog applicationId={id} stage={progress.stage}
          current={progress.evaluators.map((e) => e.userId)} onClose={() => setDialog(null)}
          onSaved={(result) => { setData(result); setDialog(null); notification.success("Đã phân công người đánh giá."); }} />
      )}
      {dialog?.type === "interview" && (
        <InterviewDialog application={app} stages={data.stages} interview={dialog.interview}
          onClose={() => setDialog(null)} onSaved={() => { setDialog(null); reload(); notification.success("Đã lưu lịch phỏng vấn."); }} />
      )}
      {dialog?.type === "interviewResult" && (
        <ReasonDialog title={{ COMPLETED: "Ghi nhận đã phỏng vấn", NO_SHOW: "Ứng viên không đến", CANCELLED: "Hủy lịch phỏng vấn" }[dialog.status]}
          label={dialog.status === "COMPLETED" ? "Nhận xét nhanh" : "Ghi chú"} required={dialog.status === "NO_SHOW"} danger={dialog.status === "CANCELLED"}
          checkbox={dialog.status === "CANCELLED" ? { label: "Gửi email báo hủy lịch cho ứng viên", defaultChecked: true } : null}
          onClose={() => setDialog(null)}
          onConfirm={(note, notify) => run(() => recruitmentApi.interviewStatus(dialog.interview.id, dialog.status, note, dialog.status === "CANCELLED" ? notify : false), "Đã cập nhật lịch phỏng vấn")} />
      )}
      {dialog?.type === "offer" && (
        <OfferDialog application={app} offer={dialog.offer} onClose={() => setDialog(null)}
          onSaved={() => { setDialog(null); reload(); notification.success("Đã lưu offer."); }} />
      )}
      {dialog?.type === "offerReason" && (
        <ReasonDialog title={dialog.title} label={dialog.label} danger onClose={() => setDialog(null)}
          onConfirm={(reason) => run(() => recruitmentApi.offerAction(dialog.offer.id, dialog.action,
            dialog.action === "respond" ? { accepted: false, reason } : { reason }), "Đã cập nhật offer")} />
      )}
      {dialog?.type === "hire" && (
        <HireDialog application={app} onClose={() => setDialog(null)}
          onHired={() => { setDialog(null); reload(); notification.success("Ứng viên đã trở thành nhân viên."); }} />
      )}
      {dialog?.type === "email" && (
        <ComposeEmailDialog applicationId={id} toEmail={app.candidateEmail} onClose={() => setDialog(null)}
          onSent={() => { setDialog(null); reload(); notification.success("Đã đưa email vào hàng gửi."); }} />
      )}
      {dialog?.type === "upload" && (
        <UploadDialog candidateId={app.candidateId} applicationId={id} defaultType={dialog.docType} onClose={() => setDialog(null)}
          onUploaded={() => { setDialog(null); reload(); notification.success("Đã tải tài liệu."); }} />
      )}
    </div>
  );
}

function OfferCard({ offer, onEdit, onAction, onReason }) {
  const swallow = (p) => p.catch(() => {});
  return (
    <li className="rounded-2xl border border-zinc-800 bg-zinc-950/50 p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-semibold text-zinc-100">{offer.jobTitle} · {money(offer.salary)}</p>
        <Badge map={OFFER_STATUS} value={offer.status} />
      </div>
      <p className="mt-1 text-xs text-zinc-400">
        Bắt đầu {formatDate(offer.startDate)} · Hạn phản hồi {formatDate(offer.responseDeadline)} · Tạo bởi {offer.createdByName}
        {offer.approvedByName ? ` · Duyệt: ${offer.approvedByName}` : ""}
      </p>
      {offer.note && <p className="mt-1 text-xs text-zinc-300">{offer.note}</p>}
      {offer.declineReason && <p className="mt-1 text-xs text-rose-300">Lý do từ chối: {offer.declineReason}</p>}
      <div className="mt-2 flex flex-wrap gap-2">
        {offer.canManage && ["DRAFT", "PENDING_APPROVAL"].includes(offer.status) && <button type="button" className={btnGhost} onClick={onEdit}>Sửa</button>}
        {offer.canManage && offer.status === "DRAFT" && <button type="button" className={btnGhost} onClick={() => swallow(onAction("submit", undefined, "Đã gửi duyệt"))}>Gửi duyệt</button>}
        {offer.canApprove && offer.status === "PENDING_APPROVAL" && (
          <>
            <button type="button" className={btnPrimary} onClick={() => swallow(onAction("approve", undefined, "Đã duyệt offer"))}>Duyệt</button>
            <button type="button" className={btnDanger} onClick={() => onReason("reject-approval", "Trả lại offer", "Lý do trả lại")}>Trả lại</button>
          </>
        )}
        {offer.canManage && offer.status === "APPROVED" && (
          <>
            <button type="button" className={btnPrimary} onClick={() => swallow(onAction("send", { sendEmail: true }, "Đã gửi offer qua email"))}><Mail size={14} />Gửi offer qua email</button>
            <button type="button" className={btnGhost} onClick={() => swallow(onAction("send", { sendEmail: false }, "Đã đánh dấu đã gửi offer"))}>Đánh dấu đã gửi (ngoài hệ thống)</button>
          </>
        )}
        {offer.canManage && offer.status === "SENT" && (
          <>
            <button type="button" className={btnPrimary} onClick={() => swallow(onAction("respond", { accepted: true }, "Ứng viên đã nhận offer"))}>Ứng viên chấp nhận</button>
            <button type="button" className={btnDanger} onClick={() => onReason("respond", "Ứng viên từ chối offer", "Lý do từ chối")}>Ứng viên từ chối</button>
          </>
        )}
        {offer.canManage && ["DRAFT", "PENDING_APPROVAL", "APPROVED", "SENT"].includes(offer.status) && (
          <button type="button" className={btnDanger} onClick={() => onReason("cancel", "Hủy offer", "Lý do hủy")}>Hủy offer</button>
        )}
      </div>
    </li>
  );
}
