import { useState } from "react";
import { ArrowDown, ArrowUp, ChevronDown, ChevronRight, Plus, Trash2 } from "lucide-react";
import { ErrorText, Field, btnDanger, btnGhost, btnPrimary, control, textarea } from "../../../components/admin/recruitment/RecruitmentUi";
import {
  IMPORTANCE, PASS_RULES, SCORING_MODES, STAGE_TYPES, emptyCriterion, emptyStage, labelOf, moveItem, passRuleText,
  validatePipeline,
} from "./recruitmentLogic";

/**
 * Trình cấu hình quy trình: thêm / sửa / xóa / sắp xếp vòng và tiêu chí của từng vòng.
 * Dùng cho quy trình của vị trí và cho mẫu quy trình (owners = null thì ẩn người phụ trách).
 */
export default function PipelineEditor({ value, onChange, owners, readOnly, onSave, saving, saveText = "Lưu quy trình", extraActions }) {
  const [open, setOpen] = useState(() => new Set(value.slice(0, 1).map((s) => s.key)));
  const [error, setError] = useState("");
  const update = (index, patch) => onChange(value.map((s, i) => (i === index ? { ...s, ...patch } : s)));
  const updateCriterion = (si, ci, patch) =>
    update(si, { criteria: value[si].criteria.map((c, j) => (j === ci ? { ...c, ...patch } : c)) });
  const toggle = (key) => setOpen((set) => { const next = new Set(set); if (next.has(key)) next.delete(key); else next.add(key); return next; });

  const addStage = () => {
    const stage = emptyStage();
    onChange([...value, stage]);
    setOpen((set) => new Set(set).add(stage.key));
  };

  const save = () => {
    const problem = validatePipeline(value);
    setError(problem);
    if (!problem) onSave?.();
  };

  return (
    <div className="space-y-3">
      {value.length === 0 && <p className="rounded-2xl border border-dashed border-zinc-700 p-6 text-center text-sm text-zinc-400">Chưa có vòng nào. Thêm vòng hoặc áp dụng một mẫu quy trình.</p>}
      {value.map((stage, si) => {
        const expanded = open.has(stage.key);
        return (
          <div key={stage.key} className="rounded-2xl border border-zinc-800 bg-zinc-950/50">
            <div className="flex flex-wrap items-center gap-2 p-3">
              <button type="button" className="flex min-w-0 flex-1 items-center gap-2 text-left" onClick={() => toggle(stage.key)} aria-expanded={expanded}>
                {expanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-zinc-800 text-xs font-bold">{si + 1}</span>
                <span className="truncate font-semibold text-zinc-100">{stage.name || <em className="text-zinc-500">Vòng chưa đặt tên</em>}</span>
                <span className="hidden text-xs text-zinc-500 sm:inline">· {labelOf(STAGE_TYPES, stage.stageType)} · {stage.criteria.length} tiêu chí · {passRuleText(stage)}</span>
              </button>
              {!readOnly && (
                <div className="flex gap-1">
                  <button type="button" className={btnGhost} aria-label="Lên" disabled={si === 0} onClick={() => onChange(moveItem(value, si, -1))}><ArrowUp size={14} /></button>
                  <button type="button" className={btnGhost} aria-label="Xuống" disabled={si === value.length - 1} onClick={() => onChange(moveItem(value, si, 1))}><ArrowDown size={14} /></button>
                  <button type="button" className={btnDanger} aria-label="Xóa vòng" onClick={() => onChange(value.filter((_, i) => i !== si))}><Trash2 size={14} /></button>
                </div>
              )}
            </div>
            {expanded && (
              <fieldset disabled={readOnly} className="space-y-4 border-t border-zinc-800 p-4">
                <div className="grid gap-3 md:grid-cols-4">
                  <Field label="Tên vòng" required className="md:col-span-2">
                    <input className={control} value={stage.name} maxLength={150} onChange={(e) => update(si, { name: e.target.value })} />
                  </Field>
                  <Field label="Loại vòng">
                    <select className={control} value={stage.stageType} onChange={(e) => update(si, { stageType: e.target.value })}>
                      {STAGE_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                    </select>
                  </Field>
                  {owners && (
                    <Field label="Người phụ trách vòng">
                      <select className={control} value={stage.ownerId} onChange={(e) => update(si, { ownerId: e.target.value })}>
                        <option value="">—</option>
                        {owners.map((u) => <option key={u.id} value={u.id}>{u.fullName}</option>)}
                      </select>
                    </Field>
                  )}
                  <Field label="Mô tả" className="md:col-span-4">
                    <textarea className={textarea} rows={2} value={stage.description} onChange={(e) => update(si, { description: e.target.value })} />
                  </Field>
                  <Field label="Cách chấm">
                    <select className={control} value={stage.scoringMode} onChange={(e) => update(si, { scoringMode: e.target.value })}>
                      {SCORING_MODES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                    </select>
                  </Field>
                  <Field label="Thang điểm">
                    <input type="number" min="1" step="1" className={control} value={stage.scaleMax} onChange={(e) => update(si, { scaleMax: e.target.value })} />
                  </Field>
                  <Field label="Điều kiện đạt" className="md:col-span-2">
                    <select className={control} value={stage.passRule} onChange={(e) => update(si, { passRule: e.target.value })}>
                      {PASS_RULES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                    </select>
                  </Field>
                  <Field label="Điểm đạt tối thiểu" hint={stage.passRule === "AVERAGE_SCORE" ? "Bắt buộc" : "Dùng để gợi ý đề xuất"}>
                    <input type="number" min="0" step="0.1" className={control} value={stage.passScore} onChange={(e) => update(si, { passScore: e.target.value })} />
                  </Field>
                  <Field label="Số phiếu tối thiểu" hint="Đủ phiếu và mọi người được giao đã nộp mới kết luận">
                    <input type="number" min="1" max="20" className={control} value={stage.minEvaluations} onChange={(e) => update(si, { minEvaluations: e.target.value })} />
                  </Field>
                  <label className="flex items-center gap-2 text-sm text-zinc-300 md:col-span-2">
                    <input type="checkbox" checked={stage.autoAdvance} onChange={(e) => update(si, { autoAdvance: e.target.checked })} />
                    Đạt thì tự chuyển sang vòng tiếp theo
                  </label>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-semibold text-zinc-200">Tiêu chí đánh giá</p>
                    {!readOnly && <button type="button" className={btnGhost} onClick={() => update(si, { criteria: [...stage.criteria, emptyCriterion()] })}><Plus size={14} />Thêm tiêu chí</button>}
                  </div>
                  {stage.criteria.length === 0 && <p className="text-xs text-zinc-500">Không có tiêu chí – người chấm chỉ chọn đề xuất và nhận xét.</p>}
                  {stage.criteria.map((c, ci) => (
                    <div key={c.key} className="grid gap-2 rounded-xl border border-zinc-800 p-3 md:grid-cols-12">
                      <input className={`${control} md:col-span-3`} placeholder="Tên tiêu chí" value={c.name} maxLength={150}
                        onChange={(e) => updateCriterion(si, ci, { name: e.target.value })} />
                      <input className={`${control} md:col-span-4`} placeholder="Mô tả / cách chấm" value={c.description}
                        onChange={(e) => updateCriterion(si, ci, { description: e.target.value })} />
                      <label className="flex items-center gap-1 text-xs text-zinc-400 md:col-span-1" title="Điểm tối đa">
                        <span>Max</span><input type="number" min="1" step="0.5" className={control} value={c.maxScore} onChange={(e) => updateCriterion(si, ci, { maxScore: e.target.value })} />
                      </label>
                      <label className="flex items-center gap-1 text-xs text-zinc-400 md:col-span-1" title="Trọng số">
                        <span>×</span><input type="number" min="0.1" step="0.1" className={control} value={c.weight} onChange={(e) => updateCriterion(si, ci, { weight: e.target.value })} />
                      </label>
                      <select className={`${control} md:col-span-2`} value={c.importance} onChange={(e) => updateCriterion(si, ci, { importance: e.target.value })}>
                        {IMPORTANCE.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                      </select>
                      {!readOnly && (
                        <div className="flex gap-1 md:col-span-1">
                          <button type="button" className={btnGhost} aria-label="Lên" disabled={ci === 0} onClick={() => update(si, { criteria: moveItem(stage.criteria, ci, -1) })}><ArrowUp size={13} /></button>
                          <button type="button" className={btnDanger} aria-label="Xóa tiêu chí" onClick={() => update(si, { criteria: stage.criteria.filter((_, j) => j !== ci) })}><Trash2 size={13} /></button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </fieldset>
            )}
          </div>
        );
      })}
      <ErrorText>{error}</ErrorText>
      {!readOnly && (
        <div className="flex flex-wrap items-center gap-2">
          <button type="button" className={btnGhost} onClick={addStage}><Plus size={14} />Thêm vòng</button>
          {extraActions}
          {onSave && <button type="button" className={`${btnPrimary} ml-auto`} disabled={saving} onClick={save}>{saving ? "Đang lưu…" : saveText}</button>}
        </div>
      )}
    </div>
  );
}
