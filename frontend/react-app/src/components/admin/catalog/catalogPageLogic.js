export function initialCatalogStatusFilter(statusParam, isGlobal) {
  return statusParam === "status" && isGlobal ? "ALL" : "";
}

export function isInactiveCatalogRow(row) {
  return row.active === false || ["INACTIVE", "ARCHIVE"].includes(row.status);
}
