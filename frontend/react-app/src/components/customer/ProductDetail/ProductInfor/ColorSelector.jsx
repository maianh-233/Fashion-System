import { useState } from "react";

const colors = ["black", "white", "blue", "red"];

export default function ColorSelector() {
  const [selected, setSelected] = useState("black");

  return (
    <div className="mt-8">
      <h3 className="mb-3 text-gray-300">Màu sắc</h3>
      <div className="flex gap-4">
        {colors.map((color) => (
          <div
            key={color}
            onClick={() => setSelected(color)}
            className={`w-11 h-11 rounded-2xl cursor-pointer bg-${color}-500
              ${
                selected === color
                  ? "ring-2 ring-offset-4 ring-[#FFCC00]"
                  : ""
              }`}
          />
        ))}
      </div>
    </div>
  );
}