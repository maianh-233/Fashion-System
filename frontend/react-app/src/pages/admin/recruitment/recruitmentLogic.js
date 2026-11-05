// Logic thuần (không React) của module tuyển dụng: nhãn, màu trạng thái, chuyển form ↔ payload, tính điểm xem trước.

export const CAMPAIGN_STATUS = {
  DRAFT: { label: "Nháp", tone: "zinc" },
  OPEN: { label: "Đang tuyển", tone: "emerald" },
  PAUSED: { label: "Tạm dừng", tone: "amber" },
  CLOSED: { label: "Đã đóng", tone: "zinc" },
  CANCELLED: { label: "Đã hủy", tone: "rose" },
};

export const POSITION_STATUS = {
  DRAFT: { label: "Nháp", tone: "zinc" },
  OPEN: { label: "Đang tuyển", tone: "emerald" },
  PAUSED: { label: "Tạm dừng", tone: "amber" },
  CLOSED: { label: "Đã đóng", tone: "zinc" },
  FILLED: { label: "Đã tuyển đủ", tone: "sky" },
};

export const APPLICATION_STATUS = {
  NEW: { label: "Mới tiếp nhận", tone: "sky" },
  SCREENING_PENDING: { label: "Chờ sàng lọc", tone: "sky" },
  PROCESSING: { label: "Đang xử lý", tone: "amber" },
  QUALIFIED: { label: "Đạt yêu cầu", tone: "emerald" },
  REJECTED: { label: "Không đạt", tone: "rose" },
  INTERVIEW_PENDING: { label: "Chờ phỏng vấn", tone: "violet" },
  INTERVIEWING: { label: "Đang phỏng vấn", tone: "violet" },
  EVALUATION_PENDING: { label: "Chờ đánh giá", tone: "amber" },
  ROUND_PASSED: { label: "Đạt vòng tuyển dụng", tone: "emerald" },
  ROUND_FAILED: { label: "Không đạt vòng tuyển dụng", tone: "rose" },
  OFFER_PENDING: { label: "Chờ Offer", tone: "amber" },
  OFFER_SENT: { label: "Đã gửi Offer", tone: "violet" },
  OFFER_ACCEPTED: { label: "Đã chấp nhận Offer", tone: "emerald" },
  OFFER_DECLINED: { label: "Từ chối Offer", tone: "rose" },
  HIRED: { label: "Đã tuyển dụng", tone: "emerald" },
  WITHDRAWN: { label: "Ứng viên rút hồ sơ", tone: "zinc" },
};

export const STAGE_STATUS = {
  NOT_REACHED: { label: "Chưa tới", tone: "zinc" },
  PENDING: { label: "Chưa bắt đầu", tone: "sky" },
  IN_PROGRESS: { label: "Đang thực hiện", tone: "amber" },
  AWAITING_EVALUATION: { label: "Chờ đánh giá", tone: "amber" },
  PASSED: { label: "Đạt", tone: "emerald" },
  FAILED: { label: "Không đạt", tone: "rose" },
  SKIPPED: { label: "Bỏ qua", tone: "zinc" },
};

export const OFFER_STATUS = {
  DRAFT: { label: "Nháp", tone: "zinc" },
  PENDING_APPROVAL: { label: "Chờ duyệt", tone: "amber" },
  APPROVED: { label: "Đã duyệt", tone: "sky" },
  SENT: { label: "Đã gửi", tone: "violet" },
  ACCEPTED: { label: "Ứng viên nhận", tone: "emerald" },
  DECLINED: { label: "Ứng viên từ chối", tone: "rose" },
  EXPIRED: { label: "Hết hạn", tone: "zinc" },
  CANCELLED: { label: "Đã hủy", tone: "zinc" },
};

export const INTERVIEW_STATUS = {
  SCHEDULED: { label: "Đã lên lịch", tone: "sky" },
  COMPLETED: { label: "Đã phỏng vấn", tone: "emerald" },
  CANCELLED: { label: "Đã hủy", tone: "zinc" },
  NO_SHOW: { label: "Không đến", tone: "rose" },
};

export const INTERVIEW_MODES = [
  { value: "ONSITE", label: "Trực tiếp" },
  { value: "ONLINE", label: "Online" },
  { value: "PHONE", label: "Điện thoại" },
];

export const STAGE_TYPES = [
  { value: "SCREENING", label: "Sàng lọc CV" },
  { value: "TEST", label: "Kiểm tra / thử việc" },
  { value: "INTERVIEW", label: "Phỏng vấn" },
  { value: "OFFER", label: "Offer" },
  { value: "OTHER", label: "Khác" },
];

export const SCORING_MODES = [
  { value: "SCORE", label: "Chấm điểm" },
  { value: "PASS_FAIL", label: "Đạt / Không đạt" },
];

export const PASS_RULES = [
  { value: "AVERAGE_SCORE", label: "Điểm trung bình ≥ điểm đạt" },
  { value: "ALL_PASS", label: "Tất cả người chấm đề xuất Đạt" },
  { value: "MAJORITY_PASS", label: "Quá nửa người chấm đề xuất Đạt" },
  { value: "MANUAL", label: "Người phụ trách tự quyết định" },
];

export const IMPORTANCE = [
  { value: "LOW", label: "Thấp" },
  { value: "MEDIUM", label: "Trung bình" },
  { value: "HIGH", label: "Cao" },
  { value: "CRITICAL", label: "Bắt buộc (loại nếu < 50%)" },
];

export const DOC_TYPES = [
  { value: "CV", label: "CV" },
  { value: "COVER_LETTER", label: "Thư xin việc" },
  { value: "PORTFOLIO", label: "Portfolio" },
  { value: "CERTIFICATE", label: "Chứng chỉ" },
  { value: "OTHER", label: "Khác" },
];

export const RECOMMENDATIONS = [
  { value: "PASS", label: "Đạt", tone: "emerald" },
  { value: "CONSIDER", label: "Cân nhắc", tone: "amber" },
  { value: "FAIL", label: "Không đạt", tone: "rose" },
];

export const TONES = {
  zinc: "border-zinc-600 bg-zinc-800/60 text-zinc-300",
  emerald: "border-emerald-500/40 bg-emerald-500/10 text-emerald-300",
  amber: "border-amber-500/40 bg-amber-500/10 text-amber-300",
  rose: "border-rose-500/40 bg-rose-500/10 text-rose-300",
  sky: "border-sky-500/40 bg-sky-500/10 text-sky-300",
  violet: "border-violet-500/40 bg-violet-500/10 text-violet-300",
};

export const labelOf = (options, value) => options.find((o) => o.value === value)?.label || value || "";

export const toneOf = (map, value) => TONES[map[value]?.tone || "zinc"];

export const ACCEPTED_FILES = ".pdf,.doc,.docx,.jpg,.jpeg,.png";
export const MAX_FILE_MB = 10;

/** Kiểm tra file trước khi tải lên (backend kiểm tra lại cả chữ ký nội dung). */
export function validateFile(file) {
  if (!file) return "Vui lòng chọn file";
  const ext = file.name.split(".").pop()?.toLowerCase();
  if (!["pdf", "doc", "docx", "jpg", "jpeg", "png"].includes(ext)) return "Chỉ nhận file PDF, DOC, DOCX, JPG, PNG";
  if (file.size > MAX_FILE_MB * 1024 * 1024) return `File vượt quá ${MAX_FILE_MB}MB`;
  return "";
}

export const canPreview = (doc) => doc?.contentType === "application/pdf" || doc?.contentType?.startsWith("image/");

export function formatBytes(size) {
  if (!size) return "0 KB";
  if (size < 1024 * 1024) return `${Math.max(1, Math.round(size / 1024))} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

export const formatDate = (value) => (value ? new Date(value).toLocaleDateString("vi-VN") : "");
export const formatDateTime = (value) =>
  value ? new Date(value).toLocaleString("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric" }) : "";
export const money = (value) => (value == null || value === "" ? "" : `${Number(value).toLocaleString("vi-VN")} ₫`);

export function salaryText(position) {
  if (position?.salaryNegotiable) return "Thỏa thuận";
  const { salaryMin: min, salaryMax: max } = position || {};
  if (min && max) return `${Number(min).toLocaleString("vi-VN")} – ${Number(max).toLocaleString("vi-VN")} ₫`;
  if (min) return `Từ ${money(min)}`;
  if (max) return `Đến ${money(max)}`;
  return "Thỏa thuận";
}

export function experienceText(position) {
  const { experienceMin: min, experienceMax: max } = position || {};
  if (min == null && max == null) return "Không yêu cầu";
  if (min != null && max != null) return `${Number(min)} – ${Number(max)} năm`;
  if (min != null) return Number(min) === 0 ? "Không yêu cầu" : `Từ ${Number(min)} năm`;
  return `Tối đa ${Number(max)} năm`;
}

/** Chuỗi "a, b , c" → ["a","b","c"] (bỏ trống, bỏ trùng không phân biệt hoa thường). */
export function splitTags(text) {
  const seen = new Set();
  return String(text || "").split(",").map((s) => s.trim()).filter((s) => {
    const key = s.toLowerCase();
    if (!s || seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

// ------------------------------------------------------------------ quy trình

let draftKey = 0;
const nextKey = () => `k${++draftKey}`;

export function emptyCriterion() {
  return { key: nextKey(), id: null, name: "", description: "", maxScore: 10, weight: 1, importance: "MEDIUM" };
}

export function emptyStage(type = "INTERVIEW") {
  return {
    key: nextKey(), id: null, name: "", description: "", stageType: type, ownerId: "", scoringMode: "SCORE",
    scaleMax: 10, passScore: 6, passRule: "AVERAGE_SCORE", minEvaluations: 1, autoAdvance: false, criteria: [emptyCriterion()],
  };
}

/** Vòng từ API → state của trình chỉnh sửa. */
export function stagesToForm(stages = []) {
  return stages.map((s) => ({
    key: nextKey(), id: s.id || null, name: s.name || "", description: s.description || "", stageType: s.stageType,
    ownerId: s.ownerId || "", scoringMode: s.scoringMode, scaleMax: Number(s.scaleMax), passScore: s.passScore == null ? "" : Number(s.passScore),
    passRule: s.passRule, minEvaluations: s.minEvaluations || 1, autoAdvance: Boolean(s.autoAdvance),
    criteria: (s.criteria || []).map((c) => ({
      key: nextKey(), id: c.id || null, name: c.name, description: c.description || "", maxScore: Number(c.maxScore),
      weight: Number(c.weight), importance: c.importance,
    })),
  }));
}

/** State của trình chỉnh sửa → payload PUT /stages (giữ id để backend cập nhật thay vì tạo mới). */
export function stagesToPayload(stages, { keepIds = true } = {}) {
  return stages.map((s) => ({
    id: keepIds ? s.id || null : null,
    name: s.name.trim(), description: s.description?.trim() || null, stageType: s.stageType,
    ownerId: keepIds ? s.ownerId || null : null, scoringMode: s.scoringMode, scaleMax: Number(s.scaleMax),
    passScore: s.passScore === "" || s.passScore == null ? null : Number(s.passScore), passRule: s.passRule,
    minEvaluations: Number(s.minEvaluations) || 1, autoAdvance: Boolean(s.autoAdvance),
    criteria: s.criteria.map((c) => ({
      id: keepIds ? c.id || null : null, name: c.name.trim(), description: c.description?.trim() || null,
      maxScore: Number(c.maxScore), weight: Number(c.weight), importance: c.importance,
    })),
  }));
}

/** Lỗi cấu hình quy trình (trùng tên, thiếu điểm đạt...) – giống kiểm tra ở backend để báo sớm. */
export function validatePipeline(stages) {
  const names = new Set();
  for (const [i, s] of stages.entries()) {
    const label = s.name.trim() || `Vòng ${i + 1}`;
    if (!s.name.trim()) return `Vòng ${i + 1}: chưa nhập tên`;
    if (names.has(s.name.trim().toLowerCase())) return `Tên vòng "${label}" bị trùng`;
    names.add(s.name.trim().toLowerCase());
    if (!(Number(s.scaleMax) > 0)) return `${label}: thang điểm phải lớn hơn 0`;
    if (s.passScore !== "" && s.passScore != null && Number(s.passScore) > Number(s.scaleMax))
      return `${label}: điểm đạt vượt thang điểm`;
    if (s.passRule === "AVERAGE_SCORE") {
      if (s.passScore === "" || s.passScore == null) return `${label}: cần điểm đạt tối thiểu`;
      if (s.criteria.length === 0) return `${label}: cần ít nhất một tiêu chí`;
    }
    const criteria = new Set();
    for (const c of s.criteria) {
      if (!c.name.trim()) return `${label}: có tiêu chí chưa nhập tên`;
      if (criteria.has(c.name.trim().toLowerCase())) return `${label}: tiêu chí "${c.name}" bị trùng`;
      criteria.add(c.name.trim().toLowerCase());
      if (!(Number(c.maxScore) > 0) || !(Number(c.weight) > 0)) return `${label}: điểm tối đa và trọng số phải lớn hơn 0`;
    }
  }
  return "";
}

export function moveItem(list, index, delta) {
  const target = index + delta;
  if (target < 0 || target >= list.length) return list;
  const next = [...list];
  [next[index], next[target]] = [next[target], next[index]];
  return next;
}

/** Mô tả ngắn điều kiện đạt của một vòng. */
export function passRuleText(stage) {
  const scale = Number(stage.scaleMax);
  const min = stage.minEvaluations > 1 ? ` · tối thiểu ${stage.minEvaluations} phiếu` : "";
  const auto = stage.autoAdvance ? " · tự chuyển vòng" : "";
  switch (stage.passRule) {
    case "AVERAGE_SCORE": return `TB ≥ ${Number(stage.passScore)}/${scale}${min}${auto}`;
    case "ALL_PASS": return `Mọi phiếu Đạt${min}${auto}`;
    case "MAJORITY_PASS": return `Đa số Đạt${min}${auto}`;
    default: return "Người phụ trách quyết định";
  }
}

// ------------------------------------------------------------------ chấm điểm (giống RecruitmentScoring ở backend)

/**
 * Điểm xem trước của một phiếu: Σ(điểm/tối đa × trọng số)/Σ trọng số × thang. Trả về null nếu chưa chấm đủ.
 * criticalFailed: tiêu chí "Bắt buộc" dưới 50% điểm tối đa (hoặc Không đạt).
 */
export function previewSheet(stage, marks) {
  const criteria = stage?.criteria || [];
  if (criteria.length === 0) return { total: null, criticalFailed: false, complete: true };
  let weighted = 0;
  let weights = 0;
  let criticalFailed = false;
  for (const c of criteria) {
    const mark = marks[c.id] || {};
    let ratio;
    if (stage.scoringMode === "PASS_FAIL") {
      if (mark.passed == null) return { total: null, criticalFailed, complete: false };
      ratio = mark.passed ? 1 : 0;
    } else {
      if (mark.score === "" || mark.score == null) return { total: null, criticalFailed, complete: false };
      ratio = Number(mark.score) / Number(c.maxScore);
    }
    if (c.importance === "CRITICAL" && ratio < 0.5) criticalFailed = true;
    weighted += ratio * Number(c.weight);
    weights += Number(c.weight);
  }
  const total = Math.round((weighted / weights) * Number(stage.scaleMax) * 100) / 100;
  return { total, criticalFailed, complete: true };
}

/** Đề xuất mặc định khi người chấm không chọn: theo điểm đạt của vòng. */
export function suggestedRecommendation(stage, sheet) {
  if (sheet.criticalFailed) return "FAIL";
  if (sheet.total == null || stage.passScore == null) return "";
  return sheet.total >= Number(stage.passScore) ? "PASS" : "FAIL";
}

export function evaluationPayload(stage, marks, { recommendation, comment, submit }) {
  return {
    scores: (stage.criteria || []).filter((c) => marks[c.id] && (marks[c.id].score !== "" && marks[c.id].score != null || marks[c.id].passed != null))
      .map((c) => ({
        criterionId: c.id,
        score: stage.scoringMode === "SCORE" ? Number(marks[c.id].score) : null,
        passed: stage.scoringMode === "PASS_FAIL" ? marks[c.id].passed : null,
        comment: marks[c.id].comment?.trim() || null,
      })),
    recommendation: recommendation || null,
    comment: comment?.trim() || null,
    submit: Boolean(submit),
  };
}

// ------------------------------------------------------------------ chiến dịch / vị trí / ứng viên

export function emptyCampaignForm(scope) {
  return {
    name: "", storeId: scope?.storeId || "", departmentId: "", ownerId: "", startDate: new Date().toISOString().slice(0, 10),
    endDate: "", description: "", status: "OPEN", members: [],
  };
}

export function campaignToForm(detail) {
  const c = detail.campaign;
  return {
    name: c.name, storeId: c.storeId || "", departmentId: c.departmentId || "", ownerId: c.ownerId || "",
    startDate: c.startDate || "", endDate: c.endDate || "", description: detail.description || "", status: c.status,
    members: (detail.members || []).map((m) => ({ userId: m.userId, role: m.role })),
  };
}

export function campaignPayload(form, { includeStatus = true } = {}) {
  return {
    name: form.name.trim(), storeId: form.storeId || null, departmentId: form.departmentId || null, ownerId: form.ownerId || null,
    startDate: form.startDate, endDate: form.endDate || null, description: form.description?.trim() || null,
    status: includeStatus ? form.status : null, members: form.members.filter((m) => m.userId),
  };
}

export function validateCampaign(form) {
  if (!form.name.trim()) return "Vui lòng nhập tên chiến dịch";
  if (!form.startDate) return "Vui lòng chọn ngày bắt đầu";
  if (form.endDate && form.endDate < form.startDate) return "Ngày kết thúc phải sau ngày bắt đầu";
  return "";
}

export function emptyPositionForm() {
  return {
    title: "", departmentId: "", orgPositionId: "", hiringManagerId: "", headcount: 1, employmentType: "FULL_TIME",
    workLocation: "", jobDescription: "", requirements: "", benefits: "", skills: "", experienceMin: "", experienceMax: "",
    salaryMin: "", salaryMax: "", salaryNegotiable: false, deadline: "", status: "OPEN", isPublic: true, templateId: "",
  };
}

export function positionToForm(detail) {
  const p = detail.position;
  const str = (v) => (v == null ? "" : String(Number(v)));
  return {
    title: p.title, departmentId: p.departmentId || "", orgPositionId: detail.orgPositionId || "", hiringManagerId: p.hiringManagerId || "",
    headcount: p.headcount, employmentType: detail.employmentType || "", workLocation: detail.workLocation || "",
    jobDescription: detail.jobDescription || "", requirements: detail.requirements || "", benefits: detail.benefits || "",
    skills: (detail.skills || []).join(", "), experienceMin: str(detail.experienceMin), experienceMax: str(detail.experienceMax),
    salaryMin: str(detail.salaryMin), salaryMax: str(detail.salaryMax), salaryNegotiable: detail.salaryNegotiable,
    deadline: p.deadline || "", status: p.status, isPublic: p.isPublic, templateId: "",
  };
}

const num = (v) => (v === "" || v == null ? null : Number(v));

export function positionPayload(form) {
  return {
    title: form.title.trim(), departmentId: form.departmentId || null, orgPositionId: form.orgPositionId || null,
    hiringManagerId: form.hiringManagerId || null, headcount: Number(form.headcount), employmentType: form.employmentType || null,
    workLocation: form.workLocation.trim() || null, jobDescription: form.jobDescription.trim() || null,
    requirements: form.requirements.trim() || null, benefits: form.benefits.trim() || null, skills: splitTags(form.skills),
    experienceMin: num(form.experienceMin), experienceMax: num(form.experienceMax), salaryMin: num(form.salaryMin),
    salaryMax: num(form.salaryMax), salaryNegotiable: Boolean(form.salaryNegotiable), deadline: form.deadline || null,
    status: form.status || null, isPublic: Boolean(form.isPublic), templateId: form.templateId || null,
  };
}

export function validatePosition(form) {
  if (!form.title.trim()) return "Vui lòng nhập tên vị trí";
  if (!(Number(form.headcount) >= 1)) return "Số lượng cần tuyển phải từ 1";
  if (num(form.salaryMin) != null && num(form.salaryMax) != null && num(form.salaryMin) > num(form.salaryMax))
    return "Lương tối thiểu không được lớn hơn lương tối đa";
  if (num(form.experienceMin) != null && num(form.experienceMax) != null && num(form.experienceMin) > num(form.experienceMax))
    return "Kinh nghiệm tối thiểu không được lớn hơn tối đa";
  return "";
}

export function emptyCandidateForm() {
  return {
    fullName: "", email: "", phone: "", dateOfBirth: "", gender: "", address: "", currentTitle: "", currentCompany: "",
    yearsExperience: "", skills: "", education: "", summary: "", source: "WALK_IN", sourceDetail: "", tags: "",
    linkedinUrl: "", portfolioUrl: "", notes: "", positionIds: [],
  };
}

export function candidateToForm(c) {
  return {
    fullName: c.fullName, email: c.email || "", phone: c.phone || "", dateOfBirth: c.dateOfBirth || "", gender: c.gender || "",
    address: c.address || "", currentTitle: c.currentTitle || "", currentCompany: c.currentCompany || "",
    yearsExperience: c.yearsExperience == null ? "" : String(Number(c.yearsExperience)), skills: (c.skills || []).join(", "),
    education: c.education || "", summary: c.summary || "", source: c.source || "OTHER", sourceDetail: c.sourceDetail || "",
    tags: (c.tags || []).join(", "), linkedinUrl: c.linkedinUrl || "", portfolioUrl: c.portfolioUrl || "", notes: c.notes || "",
    positionIds: [],
  };
}

export function candidatePayload(form, { ignoreDuplicates = false } = {}) {
  const text = (v) => (v?.trim() ? v.trim() : null);
  return {
    fullName: form.fullName.trim(), email: text(form.email), phone: text(form.phone), dateOfBirth: form.dateOfBirth || null,
    gender: text(form.gender), address: text(form.address), currentTitle: text(form.currentTitle),
    currentCompany: text(form.currentCompany), yearsExperience: num(form.yearsExperience), skills: splitTags(form.skills),
    education: text(form.education), summary: text(form.summary), source: form.source || null, sourceDetail: text(form.sourceDetail),
    tags: splitTags(form.tags), linkedinUrl: text(form.linkedinUrl), portfolioUrl: text(form.portfolioUrl), notes: text(form.notes),
    positionIds: form.positionIds || [], ignoreDuplicates,
  };
}

export function validateCandidate(form) {
  if (!form.fullName.trim()) return "Vui lòng nhập họ tên";
  if (!form.email.trim() && !form.phone.trim()) return "Cần ít nhất email hoặc số điện thoại";
  if (form.email.trim() && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim())) return "Email không hợp lệ";
  if (form.phone.trim() && !/^\+?[0-9 .()-]{8,20}$/.test(form.phone.trim())) return "Số điện thoại không hợp lệ";
  return "";
}

/** Vị trí stepper: chỉ số vòng hiện tại (0-based) hoặc -1. */
export function currentStageIndex(stages, currentStageId) {
  return stages.findIndex((s) => s.stage.id === currentStageId);
}

/** Nhóm trạng thái cho bộ lọc nhanh danh sách hồ sơ. */
export const STATUS_GROUPS = [
  { key: "", label: "Tất cả" },
  { key: "NEW,SCREENING_PENDING", label: "Chờ xử lý" },
  { key: "PROCESSING,QUALIFIED,INTERVIEW_PENDING,INTERVIEWING,EVALUATION_PENDING,ROUND_PASSED,ROUND_FAILED", label: "Trong quy trình" },
  { key: "OFFER_PENDING,OFFER_SENT,OFFER_ACCEPTED", label: "Offer" },
  { key: "HIRED", label: "Đã tuyển" },
  { key: "REJECTED,WITHDRAWN,OFFER_DECLINED", label: "Đã đóng" },
];

export const pageTotal = (res) => res?.page?.totalElements ?? res?.totalElements ?? 0;
export const pageCount = (res) => Math.max(1, res?.page?.totalPages ?? res?.totalPages ?? 1);

// ------------------------------------------------------------------ email ứng viên

export const EMAIL_STATUS = {
  QUEUED: { label: "Đang gửi", tone: "amber" },
  SENT: { label: "Đã gửi", tone: "emerald" },
  FAILED: { label: "Lỗi", tone: "rose" },
};

export const EMAIL_TEMPLATE_HINTS = {
  APPLICATION_RECEIVED: "Gửi khi ứng viên nộp hồ sơ qua trang tuyển dụng (hoặc HR thêm hồ sơ và chọn gửi).",
  INTERVIEW_INVITATION: "Gửi khi đặt lịch phỏng vấn.",
  INTERVIEW_RESCHEDULED: "Gửi khi dời giờ phỏng vấn.",
  INTERVIEW_CANCELLED: "Gửi khi hủy lịch phỏng vấn.",
  OFFER_LETTER: "Gửi khi bấm \"Gửi offer qua email\".",
  REJECTION: "Gửi khi chuyển hồ sơ sang \"Không đạt\" và chọn thông báo cho ứng viên.",
  ONBOARDING: "Gửi khi chuyển ứng viên thành nhân viên.",
};

/** Mặc định của form chuyển thành nhân viên lấy từ vị trí tuyển dụng. */
export function hireDefaults(application, position) {
  return {
    username: "", email: application.candidateEmail || "", phone: application.candidatePhone || "",
    employmentType: position?.employmentType || "FULL_TIME", departmentId: position?.position?.departmentId || "",
    orgPositionId: position?.orgPositionId || "", roleCodes: ["STAFF"], storeId: position?.position?.storeId || "",
    sendWelcomeEmail: true,
  };
}
