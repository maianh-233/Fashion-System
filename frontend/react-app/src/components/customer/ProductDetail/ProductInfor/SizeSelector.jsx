import { useState } from "react";

const sizes = ["S", "M", "L", "XL"];

export default function SizeSelector() {
  const [size, setSize] = useState("L");

  return (
    <div className="mt-8">
      <h3 className="mb-3 text-gray-300">Kích thước</h3>
      <div className="flex gap-3">
        {sizes.map((s) => (
          <button
            key={s}
            onClick={() => setSize(s)}
            className={`w-12 h-12 rounded-2xl border
              ${
                size === s
                  ? "border-[#FFCC00] text-white border-2"
                  : "border-gray-700 text-gray-300"
              }`}
          >
            {s}
          </button>
        ))}
      </div>
    </div>
  );
}