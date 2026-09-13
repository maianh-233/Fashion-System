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
  };
}

export const productApi = resource("/api/products");
export const brandApi = resource("/api/brands");
export const categoryApi = resource("/api/categories");
export const collectionApi = resource("/api/collections");
export const tagApi = resource("/api/tags");

export const variantApi = {
  list: (productId, params, options = {}) => requestAdmin(
    `/api/products/${productId}/variants${buildCatalogQuery(params)}`, options),
  create: (productId, body) => requestAdmin(`/api/products/${productId}/variants`, { method: "POST", body }),
  update: (productId, id, body) => requestAdmin(`/api/products/${productId}/variants/${id}`, { method: "PUT", body }),
  remove: (productId, id) => requestAdmin(`/api/products/${productId}/variants/${id}`, { method: "DELETE" }),
};

export const attributeApi = {
  list: (productId, params, options = {}) => requestAdmin(
    `/api/products/${productId}/attributes${buildCatalogQuery(params)}`, options),
  create: (productId, body) => requestAdmin(`/api/products/${productId}/attributes`, { method: "POST", body }),
  update: (productId, id, body) => requestAdmin(`/api/products/${productId}/attributes/${id}`, { method: "PUT", body }),
  remove: (productId, id) => requestAdmin(`/api/products/${productId}/attributes/${id}`, { method: "DELETE" }),
};
