export function isSupplierRestorable(row) {
  return row.status === "DELETED";
}
