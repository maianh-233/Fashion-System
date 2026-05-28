import { NotebookIcon } from "lucide-react";

export default function OrderNote({
  value,
  onChange,
  readOnly = false,
}) {
  return (
    <div className="bg-zinc-900 rounded-2xl p-6 space-y-3">
      <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
        <NotebookIcon size={26} />
        <span>Ghi chú</span>
      </h2>

      <textarea
        value={value || ""}
        readOnly={readOnly}
        onChange={(e) => {
          if (!readOnly) onChange?.(e.target.value);
        }}
        rows={4}
        placeholder={
          readOnly
            ? "Không có ghi chú"
            : "VD: Giao giờ hành chính, gọi trước khi giao, lấy hàng sau 18h..."
        }
        className={`
          w-full
          px-4 py-3
          rounded-xl
          bg-zinc-800
          border
          resize-none
          focus:outline-none
          ${
            readOnly
              ? "border-zinc-700 opacity-70 cursor-not-allowed"
              : "border-zinc-700 focus:border-blue-500"
          }
        `}
      />

      {!readOnly && (
        <p className="text-xs text-gray-400">
          (Không bắt buộc)
        </p>
      )}
    </div>
  );
}