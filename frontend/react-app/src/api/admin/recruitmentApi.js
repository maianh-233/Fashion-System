import { requestAdmin } from "../http/adminSession.js";

const PATH = "/api/admin/recruitment";

function qs(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    query.set(key, String(value));
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}

/** Module tuyển dụng (chiến dịch, vị trí, quy trình, ứng viên, hồ sơ, đánh giá, phỏng vấn, offer). */
export const recruitmentApi = {
  lookups: (options = {}) => requestAdmin(`${PATH}/lookups`, options),
  users: (params = {}, options = {}) => requestAdmin(`${PATH}/lookups/users${qs(params)}`, options),
  dashboard: (params = {}, options = {}) => requestAdmin(`${PATH}/dashboard${qs(params)}`, options),

  campaigns: (params = {}, options = {}) => requestAdmin(`${PATH}/campaigns${qs(params)}`, options),
  campaign: (id, options = {}) => requestAdmin(`${PATH}/campaigns/${id}`, options),
  createCampaign: (body) => requestAdmin(`${PATH}/campaigns`, { method: "POST", body }),
  updateCampaign: (id, body) => requestAdmin(`${PATH}/campaigns/${id}`, { method: "PUT", body }),
  campaignStatus: (id, status) => requestAdmin(`${PATH}/campaigns/${id}/status`, { method: "POST", body: { status } }),
  deleteCampaign: (id) => requestAdmin(`${PATH}/campaigns/${id}`, { method: "DELETE" }),

  positions: (params = {}, options = {}) => requestAdmin(`${PATH}/positions${qs(params)}`, options),
  position: (id, options = {}) => requestAdmin(`${PATH}/positions/${id}`, options),
  createPosition: (campaignId, body) => requestAdmin(`${PATH}/campaigns/${campaignId}/positions`, { method: "POST", body }),
  updatePosition: (id, body) => requestAdmin(`${PATH}/positions/${id}`, { method: "PUT", body }),
  positionStatus: (id, status) => requestAdmin(`${PATH}/positions/${id}/status`, { method: "POST", body: { status } }),
  deletePosition: (id) => requestAdmin(`${PATH}/positions/${id}`, { method: "DELETE" }),
  savePipeline: (id, stages) => requestAdmin(`${PATH}/positions/${id}/stages`, { method: "PUT", body: { stages } }),
  applyTemplate: (id, templateId) => requestAdmin(`${PATH}/positions/${id}/apply-template`, { method: "POST", body: { templateId } }),
  saveAsTemplate: (id, body) => requestAdmin(`${PATH}/positions/${id}/save-as-template`, { method: "POST", body }),

  templates: (options = {}) => requestAdmin(`${PATH}/templates`, options),
  createTemplate: (body) => requestAdmin(`${PATH}/templates`, { method: "POST", body }),
  updateTemplate: (id, body) => requestAdmin(`${PATH}/templates/${id}`, { method: "PUT", body }),
  deleteTemplate: (id) => requestAdmin(`${PATH}/templates/${id}`, { method: "DELETE" }),

  candidates: (params = {}, options = {}) => requestAdmin(`${PATH}/candidates${qs(params)}`, options),
  candidate: (id, options = {}) => requestAdmin(`${PATH}/candidates/${id}`, options),
  duplicates: (params = {}, options = {}) => requestAdmin(`${PATH}/candidates/duplicates${qs(params)}`, options),
  createCandidate: (body) => requestAdmin(`${PATH}/candidates`, { method: "POST", body }),
  updateCandidate: (id, body) => requestAdmin(`${PATH}/candidates/${id}`, { method: "PUT", body }),
  deleteCandidate: (id) => requestAdmin(`${PATH}/candidates/${id}`, { method: "DELETE" }),
  uploadDocument: (candidateId, file, { docType = "CV", note, applicationId } = {}) => {
    const form = new FormData();
    form.append("file", file);
    form.append("docType", docType);
    if (note) form.append("note", note);
    if (applicationId) form.append("applicationId", applicationId);
    return requestAdmin(`${PATH}/candidates/${candidateId}/documents`, { method: "POST", body: form });
  },
  documentUrl: (candidateId, documentId, disposition = "inline") =>
    requestAdmin(`${PATH}/candidates/${candidateId}/documents/${documentId}/url${qs({ disposition })}`),
  deleteDocument: (candidateId, documentId) =>
    requestAdmin(`${PATH}/candidates/${candidateId}/documents/${documentId}`, { method: "DELETE" }),

  applications: (params = {}, options = {}) => requestAdmin(`${PATH}/applications${qs(params)}`, options),
  myTasks: (options = {}) => requestAdmin(`${PATH}/applications/my-tasks`, options),
  application: (id, options = {}) => requestAdmin(`${PATH}/applications/${id}`, options),
  createApplication: (body) => requestAdmin(`${PATH}/applications`, { method: "POST", body }),
  applicationStatus: (id, status, reason, notifyCandidate = false) =>
    requestAdmin(`${PATH}/applications/${id}/status`, { method: "POST", body: { status, reason: reason || null, notifyCandidate } }),
  advance: (id, note) => requestAdmin(`${PATH}/applications/${id}/advance`, { method: "POST", body: { note: note || null } }),
  moveStage: (id, stageId, note) =>
    requestAdmin(`${PATH}/applications/${id}/move-stage`, { method: "POST", body: { stageId, note: note || null } }),
  decideStage: (id, stageId, decision, note) =>
    requestAdmin(`${PATH}/applications/${id}/stages/${stageId}/decision`, { method: "POST", body: { decision, note: note || null } }),
  assignEvaluators: (id, stageId, userIds) =>
    requestAdmin(`${PATH}/applications/${id}/stages/${stageId}/evaluators`, { method: "PUT", body: { userIds } }),
  addNote: (id, message) => requestAdmin(`${PATH}/applications/${id}/notes`, { method: "POST", body: { message } }),

  evaluationForm: (id, stageId, options = {}) => requestAdmin(`${PATH}/applications/${id}/stages/${stageId}/evaluation`, options),
  saveEvaluation: (id, stageId, body) =>
    requestAdmin(`${PATH}/applications/${id}/stages/${stageId}/evaluation`, { method: "PUT", body }),

  interviews: (params = {}, options = {}) => requestAdmin(`${PATH}/interviews${qs(params)}`, options),
  createInterview: (applicationId, body) => requestAdmin(`${PATH}/applications/${applicationId}/interviews`, { method: "POST", body }),
  updateInterview: (id, body) => requestAdmin(`${PATH}/interviews/${id}`, { method: "PUT", body }),
  interviewStatus: (id, status, resultNote, notifyCandidate = true) =>
    requestAdmin(`${PATH}/interviews/${id}/status`, { method: "POST", body: { status, resultNote: resultNote || null, notifyCandidate } }),

  offers: (params = {}, options = {}) => requestAdmin(`${PATH}/offers${qs(params)}`, options),
  createOffer: (applicationId, body) => requestAdmin(`${PATH}/applications/${applicationId}/offers`, { method: "POST", body }),
  updateOffer: (id, body) => requestAdmin(`${PATH}/offers/${id}`, { method: "PUT", body }),
  offerAction: (id, action, body) => requestAdmin(`${PATH}/offers/${id}/${action}`, { method: "POST", body }),
  hire: (applicationId, body) => requestAdmin(`${PATH}/applications/${applicationId}/hire`, { method: "POST", body }),

  emails: (applicationId, options = {}) => requestAdmin(`${PATH}/applications/${applicationId}/emails`, options),
  previewEmail: (applicationId, templateCode) =>
    requestAdmin(`${PATH}/applications/${applicationId}/emails/preview${qs({ templateCode })}`),
  sendEmail: (applicationId, body) => requestAdmin(`${PATH}/applications/${applicationId}/emails`, { method: "POST", body }),
  retryEmail: (applicationId, emailId) =>
    requestAdmin(`${PATH}/applications/${applicationId}/emails/${emailId}/retry`, { method: "POST" }),
  emailTemplates: (options = {}) => requestAdmin(`${PATH}/email-templates`, options),
  updateEmailTemplate: (code, body) => requestAdmin(`${PATH}/email-templates/${code}`, { method: "PUT", body }),
};
