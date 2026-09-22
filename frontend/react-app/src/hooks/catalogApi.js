import { requestAdmin } from "./auth/adminSession.js";
import { buildCatalogQuery } from "./catalogQuery.js";

export { buildCatalogQuery } from "./catalogQuery.js";

function resource(path) {
  return {
    list: (params, options = {}) => requestAdmin(`${path}${buildCatalogQuery(params)}`, options),
    detail: (id, options = {}) => requestAdmin(`${path}/${id}`, options),
    create: (body) => requestAdmin(path, { method: "POST", body }),
    update: (id, body) => requestAdmin(`${path}/${id}`, { method: "PUT", body }),
    remove: (id) => requestAdmin(`${path}/${id}`, { method: "DELETE" }),
    restore: (id) => requestAdmin(`${path}/${id}/restore`, { method: "PATCH" }),
    impact: (id) => requestAdmin(`${path}/${id}/impact`),
  };
}

function upload(path, id, file) {
  const body = new FormData();
  body.append("file", file);
  return requestAdmin(`${path}/${id}/${path === "/api/brands" ? "logo" : "image"}`, { method: "POST", body });
}

export const productApi = { ...resource("/api/products"), uploadImage: (id, file) => upload("/api/products", id, file) };
export const brandApi = { ...resource("/api/brands"), uploadImage: (id, file) => upload("/api/brands", id, file) };
export const categoryApi = resource("/api/categories");
export const collectionApi = { ...resource("/api/collections"), uploadImage: (id, file) => upload("/api/collections", id, file) };
export const tagApi = { ...resource("/api/tags"), impact: null };

export const variantApi = {
  listAll: (params, options = {}) => requestAdmin(`/api/product-variants${buildCatalogQuery(params)}`, options),
  list: (productId, params, options = {}) => requestAdmin(
    `/api/products/${productId}/variants${buildCatalogQuery(params)}`, options),
  create: (productId, body) => requestAdmin(`/api/products/${productId}/variants`, { method: "POST", body }),
  update: (productId, id, body) => requestAdmin(`/api/products/${productId}/variants/${id}`, { method: "PUT", body }),
  remove: (productId, id) => requestAdmin(`/api/products/${productId}/variants/${id}`, { method: "DELETE" }),
  restore: (productId, id) => requestAdmin(`/api/products/${productId}/variants/${id}/activate`, { method: "PATCH" }),
};

export const attributeApi = {
  list: (productId, params, options = {}) => requestAdmin(
    `/api/products/${productId}/attributes${buildCatalogQuery(params)}`, options),
  create: (productId, body) => requestAdmin(`/api/products/${productId}/attributes`, { method: "POST", body }),
  update: (productId, id, body) => requestAdmin(`/api/products/${productId}/attributes/${id}`, { method: "PUT", body }),
  remove: (productId, id) => requestAdmin(`/api/products/${productId}/attributes/${id}`, { method: "DELETE" }),
};
