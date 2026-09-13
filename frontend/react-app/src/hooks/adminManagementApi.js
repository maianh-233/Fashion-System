import { requestAdmin } from "./auth/adminSession";

function queryString(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") query.set(key, value);
  });
  const value = query.toString();
  return value ? `?${value}` : "";
}

export const employeeApi = {
  list: (params, options = {}) => requestAdmin(`/api/employees${queryString(params)}`, options),
  detail: (id, options = {}) => requestAdmin(`/api/employees/${id}`, options),
  summary: (storeId, options = {}) => requestAdmin(`/api/employees/summary${queryString({ storeId })}`, options),
  scope: (options = {}) => requestAdmin("/api/employees/scope", options),
  availableStores: (options = {}) => requestAdmin("/api/employees/available-stores", options),
  create: (body) => requestAdmin("/api/employees", { method: "POST", body }),
  update: (id, body) => requestAdmin(`/api/employees/${id}`, { method: "PUT", body }),
  subordinates: (id, options = {}) => requestAdmin(`/api/employees/${id}/subordinates`, options),
  hierarchyImpact: (id, params, options = {}) =>
    requestAdmin(`/api/employees/${id}/hierarchy-impact${queryString(params)}`, options),
  eligibleSubordinates: (id, options = {}) => requestAdmin(`/api/employees/${id}/eligible-subordinates`, options),
  assignSubordinate: (id, email) => requestAdmin(`/api/employees/${id}/subordinates`, { method: "POST", body: { email } }),
  removeSubordinate: (id, subordinateId) => requestAdmin(`/api/employees/${id}/subordinates/${subordinateId}`, { method: "DELETE" }),
  setLocked: (id, locked) => requestAdmin(`/api/employees/${id}/lock?locked=${locked}`, { method: "PATCH" }),
  remove: (id) => requestAdmin(`/api/employees/${id}`, { method: "DELETE" }),
  restore: (id) => requestAdmin(`/api/employees/${id}/restore`, { method: "PATCH" }),
};

export const storeApi = {
  list: (params, options = {}) => requestAdmin(`/api/admin/stores${queryString(params)}`, options),
  coordinateAvailability: (params, options = {}) => requestAdmin(`/api/admin/stores/coordinate-availability${queryString(params)}`, options),
  create: (body) => requestAdmin("/api/admin/stores", { method: "POST", body }),
  update: (id, body) => requestAdmin(`/api/admin/stores/${id}`, { method: "PUT", body }),
  remove: (id) => requestAdmin(`/api/admin/stores/${id}`, { method: "DELETE" }),
};

export const departmentApi = {
  list: (params, options = {}) => requestAdmin(`/api/departments${queryString(params)}`, options),
  detail: (id, options = {}) => requestAdmin(`/api/departments/${id}`, options),
  create: (body) => requestAdmin("/api/departments", { method: "POST", body }),
  update: (id, body) => requestAdmin(`/api/departments/${id}`, { method: "PUT", body }),
  remove: (id) => requestAdmin(`/api/departments/${id}`, { method: "DELETE" }),
};

export const positionApi = {
  list: (params, options = {}) => requestAdmin(`/api/positions${queryString(params)}`, options),
  detail: (id, options = {}) => requestAdmin(`/api/positions/${id}`, options),
  hierarchyImpact: (id, params, options = {}) =>
    requestAdmin(`/api/positions/${id}/hierarchy-impact${queryString(params)}`, options),
  create: (body) => requestAdmin("/api/positions", { method: "POST", body }),
  update: (id, body) => requestAdmin(`/api/positions/${id}`, { method: "PUT", body }),
  remove: (id) => requestAdmin(`/api/positions/${id}`, { method: "DELETE" }),
};
