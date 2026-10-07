import { ALL_STORES, NO_STORE } from "./storeSelectorLogic";

export { ALL_STORES, NO_STORE } from "./storeSelectorLogic";

export default function StoreSelector({
  value = "", onChange, stores = [], includeAll = false, includeNoStore = false,
  required = false, disabled = false, readOnly = false, loading = false,
  label = "Cửa hàng", placeholder = "Chọn cửa hàng", className = "",
}) {
  const select = <select
    aria-label={label}
    value={value ?? ""}
    required={required}
    disabled={disabled || readOnly || loading}
    onChange={event => onChange?.(event.target.value)}
    className={className}
  >
    {!required && !includeAll && <option value="">{loading ? "Đang tải cửa hàng…" : placeholder}</option>}
    {required && <option value="" disabled>{loading ? "Đang tải cửa hàng…" : placeholder}</option>}
    {includeAll && <option value={ALL_STORES}>Tất cả cửa hàng</option>}
    {includeNoStore && <option value={NO_STORE}>Không thuộc cửa hàng / Website</option>}
    {stores.map(store => <option key={store.id} value={store.id}>{store.code ? `${store.code} · ` : ""}{store.name}</option>)}
  </select>;
  return label ? <label className="flex flex-wrap items-center gap-3"><span>{label}</span>{select}</label> : select;
}
