import { requestAdmin } from "./adminSession";

const ROOT = "/api/admin/authorization";

export const getAuthorizationRoles = () => requestAdmin(`${ROOT}/roles`);
export const getAuthorizationRole = (id) => requestAdmin(`${ROOT}/roles/${id}`);
export const getRolePermissionCatalog = () => requestAdmin(`${ROOT}/role-catalog`);
export const createAuthorizationRole = (body) => requestAdmin(`${ROOT}/roles`, { method: "POST", body });
export const updateAuthorizationRole = (id, body) => requestAdmin(`${ROOT}/roles/${id}`, { method: "PUT", body });
export const deleteAuthorizationRole = (id) => requestAdmin(`${ROOT}/roles/${id}`, { method: "DELETE" });
export const replaceAuthorizationRolePermissions = (id, permissions) =>
  requestAdmin(`${ROOT}/roles/${id}/permissions`, { method: "PUT", body: permissions });

export const getAuthorizationModules = () => requestAdmin(`${ROOT}/modules`);
export const createAuthorizationModule = (body) => requestAdmin(`${ROOT}/modules`, { method: "POST", body });
export const updateAuthorizationModule = (id, body) => requestAdmin(`${ROOT}/modules/${id}`, { method: "PUT", body });
export const deleteAuthorizationModule = (id) => requestAdmin(`${ROOT}/modules/${id}`, { method: "DELETE" });

export const getAuthorizationGroups = () => requestAdmin(`${ROOT}/groups`);
export const createAuthorizationGroup = (body) => requestAdmin(`${ROOT}/groups`, { method: "POST", body });
export const updateAuthorizationGroup = (id, body) => requestAdmin(`${ROOT}/groups/${id}`, { method: "PUT", body });
export const deleteAuthorizationGroup = (id) => requestAdmin(`${ROOT}/groups/${id}`, { method: "DELETE" });

export const getAuthorizationPermissions = () => requestAdmin(`${ROOT}/permissions`);
export const createAuthorizationPermission = (body) => requestAdmin(`${ROOT}/permissions`, { method: "POST", body });
export const updateAuthorizationPermission = (id, body) => requestAdmin(`${ROOT}/permissions/${id}`, { method: "PUT", body });
export const deleteAuthorizationPermission = (id) => requestAdmin(`${ROOT}/permissions/${id}`, { method: "DELETE" });
