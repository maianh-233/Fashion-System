export default function PriceRangeSlider({ priceRange, setPriceRange }) {
  const format = (v) => v.toLocaleString("vi-VN") + "đ";

  return (
    <div className="mb-8">
      <h3 className="text-amber-400 font-medium mb-3">Khoảng giá</h3>

      {/* value display */}
      <div className="flex justify-between text-xs text-zinc-300 mb-2">
        <span>{format(priceRange[0])}</span>
        <span>{format(priceRange[1])}</span>
      </div>

      {/* min */}
      <input
        type="range"
        min={0}
        max={5000000}
        step={50000}
        value={priceRange[0]}
        onChange={(e) =>
          setPriceRange([Number(e.target.value), priceRange[1]])
        }
        className="w-full accent-amber-400"
      />

      {/* max */}
      <input
        type="range"
        min={0}
        max={5000000}
        step={50000}
        value={priceRange[1]}
        onChange={(e) =>
          setPriceRange([priceRange[0], Number(e.target.value)])
        }
        className="w-full accent-amber-400 mt-2"
      />
    </div>
  );
}