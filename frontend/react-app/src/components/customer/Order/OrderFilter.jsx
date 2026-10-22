import { ORDER_STATUS_FILTERS } from "../../../pages/customer/customerOrderLogic";

export default function OrderFilter({ value, onChange }) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      aria-label="Lọc đơn hàng theo trạng thái"
      className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-900 px-4 py-2.5 text-sm focus:border-amber-400 focus:outline-none sm:w-auto sm:rounded-2xl sm:px-5 sm:py-3"
    >
      {ORDER_STATUS_FILTERS.map((option) => <option key={option.value || "all"} value={option.value}>{option.label}</option>)}
    </select>
  );
}
