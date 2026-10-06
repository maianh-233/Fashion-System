import { requestAdmin } from "./auth/adminSession.js";

export const SYSTEM_LOG_PAGE_SIZE = 25;

const clean = (value) => typeof value === "string" ? value.trim() : value;

export function buildSystemLogQuery(view, filters = {}, page = 1) {
  const common = {
    page: Math.max(0, Number(page || 1) - 1),
    size: SYSTEM_LOG_PAGE_SIZE,
    action: clean(filters.action)?.toUpperCase(),
    actorUserId: clean(filters.actorUserId),
    fromAt: clean(filters.fromAt),
    toAt: clean(filters.toAt),
  };
  if (view === "auth") return Object.fromEntries(Object.entries(common).filter(([, value]) => value !== "" && value != null));
  const business = {
    ...common,
    username: clean(filters.username),
    table: clean(filters.table),
    rowId: clean(filters.rowId),
    requestId: clean(filters.requestId),
  };
  return Object.fromEntries(Object.entries(business).filter(([, value]) => value !== "" && value != null));
}

export function presentSystemLog(log = {}, view = "business") {
  if (view === "auth") {
    return {
      actor: log.userId || "Ẩn danh",
      timestamp: log.createdAt || null,
      source: log.ipAddress || "—",
      summary: log.description || "—",
      severity: log.action === "LOGIN_FAILED" ? "WARN" : "INFO",
    };
  }
  const changedRows = Number.isFinite(Number(log.rowCount)) ? Number(log.rowCount) : (log.changes?.length || 0);
  return {
    actor: log.username || log.actorUserId || log.actorType || "SYSTEM",
    timestamp: log.occurredAt || log.createdAt || null,
    source: log.method && log.path ? `${log.method} ${log.path}` : (log.path || log.jobName || log.requestId || "—"),
    summary: log.detail || `${changedRows} dòng thay đổi`,
    severity: /FAILED|ERROR/.test(log.action || "") ? "WARN" : "INFO",
  };
}

export function queryString(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") query.set(key, value);
  });
  const result = query.toString();
  return result ? `?${result}` : "";
}

export function fetchAdminLogs(params = {}, options = {}) {
  return requestAdmin(`/api/admin/audit-logs${queryString(params)}`, options);
}

export const fetchBusinessAuditLogs = fetchAdminLogs;

export function fetchAuthAuditLogs(params = {}, options = {}) {
  return requestAdmin(`/api/admin/auth-audit-logs${queryString(params)}`, options);
}

export function normalizeAdminLogsPage(response) {
  return {
    rows: Array.isArray(response?.content) ? response.content : [],
    page: Number(response?.number ?? 0) + 1,
    totalPages: Math.max(1, Number(response?.totalPages ?? 1)),
    totalElements: Number(response?.totalElements ?? 0),
  };
}
