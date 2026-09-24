export function initialCatalogStatusFilter(statusParam, isGlobal) {
  return statusParam === "status" && isGlobal ? "ALL" : "";
}

export function isInactiveCatalogRow(row) {
  return row.active === false || ["INACTIVE", "ARCHIVE", "DELETED"].includes(row.status);
}

export function catalogPageMetadata(result) {
  const metadata = result?.page || result || {};
  return {
    totalPages: Math.max(1, metadata.totalPages ?? 1),
    totalElements: metadata.totalElements ?? 0,
  };
}

export function isCatalogFieldVisible(field, dialog, hasPermission = () => true) {
  if (dialog === "create" && (field.generated || field.hideOnCreate)) return false;
  if (field.createOnly && dialog !== "create") return false;
  return !field.permission || hasPermission(field.permission);
}
