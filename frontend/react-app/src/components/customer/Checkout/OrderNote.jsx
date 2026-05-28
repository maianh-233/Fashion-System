import { NotebookIcon } from "lucide-react";
export default function OrderNote({ value, onChange }) {
  return (
    <div className="bg-zinc-900 rounded-2xl p-6 space-y-3">
      <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
        <NotebookIcon size={26} />
        <span>Ghi chú</span>
      </h2>

      <textarea
        value={value}
        onChange={e => onChange(e.target.value)}
        rows={4}
        placeholder="VD: Giao giờ hành chính, gọi trước khi giao, lấy hàng sau 18h..."
        className="
          w-full
          px-4 py-3
          rounded-xl
          bg-zinc-800
          border border-zinc-700
          focus:outline-none
          focus:border-blue-500
          resize-none
        "
      />

      <p className="text-xs text-gray-400">
        (Không bắt buộc)
      </p>
    </div>
  );
}