import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { AlertTriangle, ArrowLeft, Lock } from "lucide-react";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { useSystemNotification } from "../../../components/common/SystemNotification";
import { Badge, ErrorText, Loading, Section, btnGhost, btnPrimary, control, textarea, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import {
  APPLICATION_STATUS, IMPORTANCE, RECOMMENDATIONS, STAGE_STATUS, evaluationPayload, formatDateTime, labelOf, passRuleText,
  previewSheet, suggestedRecommendation,
} from "./recruitmentLogic";

function marksFrom(evaluation) {
  const marks = {};
  (evaluation?.scores || []).forEach((s) => {
    marks[s.criterionId] = { score: s.score == null ? "" : Number(s.score), passed: s.passed, comment: s.comment || "" };
  });
  return marks;
}

/** Phiếu đánh giá ứng viên cho một vòng: chấm từng tiêu chí, xem trước điểm tổng hợp, lưu nháp hoặc nộp. */
export default function EvaluationPage() {
  const { id, stageId } = useParams();
  const notification = useSystemNotification();
  const { data, error, loading, setData } = useLoader((signal) => recruitmentApi.evaluationForm(id, stageId, { signal }), [id, stageId]);
  const [draft, setDraft] = useState(null);
  const [draftFor, setDraftFor] = useState(null);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");

  if (data && draftFor !== data) {
    setDraftFor(data);
    setDraft({ marks: marksFrom(data.mine), recommendation: data.mine?.recommendation || "", comment: data.mine?.comment || "" });
  }
  if (loading && !data) return <Loading />;
  if (!data || !draft) return <ErrorText>{error}</ErrorText>;

  const { stage, application: app } = data;
  const locked = data.locked || !data.canEvaluate;
  const sheet = previewSheet(stage, draft.marks);
  const suggested = suggestedRecommendation(stage, sheet);
  const setMark = (criterionId, patch) => setDraft((d) => ({ ...d, marks: { ...d.marks, [criterionId]: { ...d.marks[criterionId], ...patch } } }));

  const save = async (submit) => {
    setFormError("");
    if (submit) {
      if (!sheet.complete) return setFormError("Vui lòng chấm đủ mọi tiêu chí trước khi nộp.");
      if (!draft.recommendation && !suggested) return setFormError("Vui lòng chọn đề xuất.");
      if (!await notification.confirm("Nộp phiếu? Sau khi nộp sẽ không sửa được.", { confirmText: "Nộp phiếu" })) return;
    }
    setSaving(true);
    try {
      const result = await recruitmentApi.saveEvaluation(id, stageId, evaluationPayload(stage, draft.marks, { ...draft, submit }));
      setData(result);
      notification.success(submit ? "Đã nộp phiếu đánh giá." : "Đã lưu nháp.");
    } catch (e) {
      setFormError(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-5">
      <Link to={`/admin/recruitment/applications/${id}`} className="inline-flex items-center gap-1.5 text-sm text-zinc-400 hover:text-white"><ArrowLeft size={14} />Hồ sơ {app.code}</Link>
      <Section>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold">Phiếu đánh giá – {stage.name}</h2>
            <p className="text-sm text-zinc-400">{app.candidateName} · {app.positionTitle} · Thang {Number(stage.scaleMax)} · {passRuleText(stage)}</p>
            {stage.description && <p className="mt-1 text-sm text-zinc-300">{stage.description}</p>}
          </div>
          <div className="flex flex-wrap gap-2">
            <Badge map={APPLICATION_STATUS} value={app.status} />
            <Badge map={STAGE_STATUS} value={data.stageStatus} />
          </div>
        </div>
        {locked && (
          <p className="mt-3 flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-sm text-zinc-300">
            <Lock size={14} />
            {data.mine?.status === "SUBMITTED" ? `Bạn đã nộp phiếu lúc ${formatDateTime(data.mine.submittedAt)}.`
              : !data.canEvaluate ? "Bạn chỉ có quyền xem phiếu của vòng này."
                : data.stageStatus === "NOT_REACHED" ? "Hồ sơ chưa tới vòng này." : "Vòng đã có kết luận hoặc hồ sơ đã kết thúc."}
          </p>
        )}
      </Section>

      <div className="grid gap-5 xl:grid-cols-3">
        <Section title="Chấm theo tiêu chí" className="xl:col-span-2">
          {stage.criteria.length === 0 && <p className="text-sm text-zinc-500">Vòng này không có tiêu chí – chỉ cần đề xuất và nhận xét.</p>}
          <div className="space-y-3">
            {stage.criteria.map((c) => {
              const mark = draft.marks[c.id] || {};
              return (
                <div key={c.id} className="rounded-2xl border border-zinc-800 bg-zinc-950/50 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="font-semibold text-zinc-100">{c.name}
                        {c.importance === "CRITICAL" && <span className="ml-2 inline-flex items-center gap-1 text-xs font-normal text-rose-300"><AlertTriangle size={12} />Bắt buộc</span>}
                      </p>
                      <p className="text-xs text-zinc-500">{c.description || ""} {c.description ? "·" : ""} Trọng số ×{Number(c.weight)} · Mức {labelOf(IMPORTANCE, c.importance).toLowerCase()}</p>
                    </div>
                    {stage.scoringMode === "PASS_FAIL" ? (
                      <div className="flex gap-1">
                        {[[true, "Đạt"], [false, "Không đạt"]].map(([value, label]) => (
                          <button key={label} type="button" disabled={locked} onClick={() => setMark(c.id, { passed: value })}
                            className={`rounded-xl border px-3 py-1.5 text-sm ${mark.passed === value ? (value ? "border-emerald-400 bg-emerald-500/15 text-emerald-200" : "border-rose-400 bg-rose-500/15 text-rose-200") : "border-zinc-700 text-zinc-300"}`}>
                            {label}
                          </button>
                        ))}
                      </div>
                    ) : (
                      <label className="flex items-center gap-2 text-sm text-zinc-400">
                        <input type="number" min="0" max={Number(c.maxScore)} step="0.5" disabled={locked} className={`${control} w-24 text-right`}
                          value={mark.score ?? ""} onChange={(e) => setMark(c.id, { score: e.target.value })} aria-label={`Điểm ${c.name}`} />
                        / {Number(c.maxScore)}
                      </label>
                    )}
                  </div>
                  {stage.scoringMode === "SCORE" && (
                    <input type="range" min="0" max={Number(c.maxScore)} step="0.5" disabled={locked} className="mt-3 w-full accent-amber-400"
                      value={mark.score === "" || mark.score == null ? 0 : mark.score} onChange={(e) => setMark(c.id, { score: e.target.value })} aria-hidden="true" />
                  )}
                  <input className={`${control} mt-2`} placeholder="Nhận xét cho tiêu chí (không bắt buộc)" disabled={locked}
                    value={mark.comment || ""} onChange={(e) => setMark(c.id, { comment: e.target.value })} maxLength={1000} />
                </div>
              );
            })}
          </div>
        </Section>

        <div className="space-y-5">
          <Section title="Tổng hợp">
            <p className="text-sm text-zinc-400">Điểm phiếu của bạn</p>
            <p className="text-4xl font-semibold tabular-nums text-amber-300">{sheet.total ?? "—"}<span className="text-base text-zinc-500"> / {Number(stage.scaleMax)}</span></p>
            {stage.passScore != null && <p className="text-xs text-zinc-500">Điểm đạt của vòng: {Number(stage.passScore)}</p>}
            {sheet.criticalFailed && <p className="mt-2 text-sm text-rose-300">Có tiêu chí bắt buộc dưới 50% – vòng sẽ không đạt.</p>}
            <div className="mt-4 space-y-2">
              <p className="text-xs font-medium text-zinc-400">Đề xuất {suggested && !draft.recommendation && <span className="text-zinc-500">(gợi ý: {labelOf(RECOMMENDATIONS, suggested)})</span>}</p>
              <div className="flex gap-1">
                {RECOMMENDATIONS.map((r) => (
                  <button key={r.value} type="button" disabled={locked} onClick={() => setDraft((d) => ({ ...d, recommendation: d.recommendation === r.value ? "" : r.value }))}
                    className={`flex-1 rounded-xl border px-2 py-2 text-sm ${draft.recommendation === r.value ? "border-amber-400 bg-amber-400/15 text-amber-100" : "border-zinc-700 text-zinc-300"}`}>
                    {r.label}
                  </button>
                ))}
              </div>
              <textarea className={textarea} rows={4} placeholder="Nhận xét chung" disabled={locked} value={draft.comment}
                onChange={(e) => setDraft((d) => ({ ...d, comment: e.target.value }))} />
            </div>
            <ErrorText>{formError}</ErrorText>
            {!locked && (
              <div className="mt-3 flex gap-2">
                <button type="button" className={btnGhost} disabled={saving} onClick={() => save(false)}>Lưu nháp</button>
                <button type="button" className={`${btnPrimary} flex-1`} disabled={saving} onClick={() => save(true)}>Nộp phiếu</button>
              </div>
            )}
            {data.stageScore != null && <p className="mt-3 text-sm text-zinc-300">Điểm tổng hợp của vòng: <strong>{Number(data.stageScore)}</strong></p>}
          </Section>

          <Section title="Phiếu của người khác">
            {data.othersHidden && <p className="text-sm text-zinc-500">Phiếu của người khác hiện sau khi bạn nộp phiếu (tránh ảnh hưởng đánh giá).</p>}
            {!data.othersHidden && data.others.length === 0 && <p className="text-sm text-zinc-500">Chưa có phiếu nào khác.</p>}
            <ul className="space-y-3">
              {data.others.map((o) => (
                <li key={o.id} className="rounded-2xl border border-zinc-800 p-3">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-sm font-semibold">{o.evaluatorName}</p>
                    <span className="tabular-nums text-amber-300">{o.totalScore != null ? Number(o.totalScore) : ""}</span>
                  </div>
                  <p className="text-xs text-zinc-500">{labelOf(RECOMMENDATIONS, o.recommendation)} · {formatDateTime(o.submittedAt)}{o.criticalFailed ? " · trượt tiêu chí bắt buộc" : ""}</p>
                  {o.comment && <p className="mt-1 text-sm text-zinc-300">{o.comment}</p>}
                  <ul className="mt-2 space-y-0.5 text-xs text-zinc-400">
                    {o.scores.map((s) => {
                      const c = stage.criteria.find((x) => x.id === s.criterionId);
                      return <li key={s.criterionId}>{c?.name}: {s.passed != null ? (s.passed ? "Đạt" : "Không đạt") : `${Number(s.score)}/${Number(c?.maxScore)}`}{s.comment ? ` – ${s.comment}` : ""}</li>;
                    })}
                  </ul>
                </li>
              ))}
            </ul>
          </Section>
        </div>
      </div>
    </div>
  );
}
