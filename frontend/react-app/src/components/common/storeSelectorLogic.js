export const ALL_STORES = "ALL";
export const NO_STORE = "NO_STORE";

export function selectionToStoreFilter(value) {
  if (!value || value === ALL_STORES) return {};
  if (value === NO_STORE) return { noStore: true };
  return { storeId: value };
}
