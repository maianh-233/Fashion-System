import { useState } from "react";

export default function PromoInput({ onApply, disabled }) {
  const [code, setCode] = useState("");
  const [error, setError] = useState("");

  const handleApply = () => {
    if (!code.trim()) {
      setError("Vui lòng nhập mã");
      return;
    }

    setError("");
    onApply(code.trim().toUpperCase());
    setCode("");
  };

  return (
    <>
      <div className="flex gap-2">
        <input
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder="Mã giảm giá"
          disabled={disabled}
          className="
            flex-1
            bg-zinc-800 border border-zinc-700
            rounded-xl
            px-4 py-2.5
            text-sm
            focus:outline-none focus:border-amber-400
            disabled:opacity-50
          "
        />
        <button
          onClick={handleApply}
          disabled={disabled}
          className="
            bg-amber-400 hover:bg-amber-500
            disabled:opacity-50
            text-black
            px-4
            rounded-xl
            text-sm font-medium
          "
        >
          Áp dụng
        </button>
      </div>

      {error && <p className="text-red-400 text-xs mt-1">{error}</p>}
      {disabled && (
        <p className="text-zinc-400 text-xs mt-1">
          Tối đa 3 mã giảm giá
        </p>
      )}
    </>
  );
}