import test from "node:test";
import assert from "node:assert/strict";
import {
  candidatePayload, emptyCandidateForm, emptyStage, evaluationPayload, experienceText, moveItem, previewSheet,
  salaryText, splitTags, stagesToForm, stagesToPayload, suggestedRecommendation, validateCandidate, validateFile,
  validatePipeline,
} from "../../../src/pages/admin/recruitment/recruitmentLogic.js";

const stage = {
  id: "s1", scoringMode: "SCORE", scaleMax: 10, passScore: 7, criteria: [
    { id: "a", maxScore: 10, weight: 2, importance: "HIGH" },
    { id: "b", maxScore: 10, weight: 1, importance: "CRITICAL" },
  ],
};

test("score preview matches the backend formula", () => {
  const sheet = previewSheet(stage, { a: { score: 8 }, b: { score: 5 } });
  assert.equal(sheet.total, 7);
  assert.equal(sheet.criticalFailed, false);
  assert.equal(suggestedRecommendation(stage, sheet), "PASS");
  const knocked = previewSheet(stage, { a: { score: 10 }, b: { score: 4 } });
  assert.equal(knocked.criticalFailed, true);
  assert.equal(suggestedRecommendation(stage, knocked), "FAIL");
  assert.equal(previewSheet(stage, { a: { score: 8 } }).complete, false);
});

test("pass/fail preview counts passed criteria", () => {
  const pf = { ...stage, scoringMode: "PASS_FAIL", passScore: null };
  const sheet = previewSheet(pf, { a: { passed: true }, b: { passed: false } });
  assert.equal(sheet.total, 6.67);
  assert.equal(sheet.criticalFailed, true);
});

test("evaluation payload only sends marked criteria", () => {
  const payload = evaluationPayload(stage, { a: { score: "8", comment: " ok " } }, { recommendation: "", comment: "", submit: false });
  assert.deepEqual(payload.scores, [{ criterionId: "a", score: 8, passed: null, comment: "ok" }]);
  assert.equal(payload.recommendation, null);
  assert.equal(payload.submit, false);
});

test("pipeline round-trips and keeps ids", () => {
  const form = stagesToForm([{ id: "x", name: "Sàng lọc", stageType: "SCREENING", scoringMode: "SCORE", scaleMax: 10, passScore: 6,
    passRule: "AVERAGE_SCORE", minEvaluations: 1, autoAdvance: true, criteria: [{ id: "c", name: "KN", maxScore: 10, weight: 1, importance: "LOW" }] }]);
  const payload = stagesToPayload(form);
  assert.equal(payload[0].id, "x");
  assert.equal(payload[0].criteria[0].id, "c");
  assert.equal(stagesToPayload(form, { keepIds: false })[0].id, null);
});

test("pipeline validation mirrors the backend", () => {
  const base = emptyStage();
  const a = { ...base, name: "Phỏng vấn", criteria: [{ ...base.criteria[0], name: "Giao tiếp" }] };
  assert.equal(validatePipeline([a, { ...a, key: "z" }]), 'Tên vòng "Phỏng vấn" bị trùng');
  assert.match(validatePipeline([{ ...a, passScore: 11 }]), /vượt thang điểm/);
  assert.match(validatePipeline([{ ...a, passScore: "" }]), /cần điểm đạt/);
  assert.equal(validatePipeline([{ ...a, criteria: [{ ...a.criteria[0], name: "Giao tiếp" }] }]), "");
  assert.equal(validatePipeline([{ ...a, passRule: "MANUAL", criteria: [] }]), "");
});

test("reorder helper", () => {
  assert.deepEqual(moveItem([1, 2, 3], 0, 1), [2, 1, 3]);
  assert.deepEqual(moveItem([1, 2, 3], 0, -1), [1, 2, 3]);
});

test("candidate form", () => {
  const form = { ...emptyCandidateForm(), fullName: "An", phone: "0912345678", skills: "Bán hàng, bán hàng , Excel" };
  assert.equal(validateCandidate(form), "");
  assert.equal(validateCandidate({ ...form, phone: "" }), "Cần ít nhất email hoặc số điện thoại");
  assert.deepEqual(candidatePayload(form).skills, ["Bán hàng", "Excel"]);
  assert.equal(candidatePayload(form).email, null);
  assert.deepEqual(splitTags(" a, ,A,b"), ["a", "b"]);
});

test("file checks and position texts", () => {
  assert.equal(validateFile({ name: "cv.PDF", size: 1000 }), "");
  assert.match(validateFile({ name: "cv.exe", size: 10 }), /Chỉ nhận/);
  assert.match(validateFile({ name: "cv.pdf", size: 11 * 1024 * 1024 }), /vượt quá/);
  assert.equal(salaryText({ salaryMin: 7000000, salaryMax: 9000000 }), "7.000.000 – 9.000.000 ₫");
  assert.equal(salaryText({ salaryNegotiable: true, salaryMin: 1 }), "Thỏa thuận");
  assert.equal(experienceText({ experienceMin: 1 }), "Từ 1 năm");
});
