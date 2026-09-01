export default function PriceRangeSlider({ priceRange, setPriceRange }) {
  const format = (v) => v.toLocaleString("vi-VN") + "đ";
  const step = 50000;

  return (
    <div className="customer-filter__section mb-6">
      <h3>Khoảng giá</h3>

      {/* value display */}
      <div className="product-price-values">
        <span>{format(priceRange[0])}</span>
        <span>{format(priceRange[1])}</span>
      </div>

      {/* min */}
      <input
        type="range"
        min={0}
        max={5000000}
        step={step}
        value={priceRange[0]}
        onChange={(e) =>
          setPriceRange([
            Math.min(Number(e.target.value), priceRange[1] - step),
            priceRange[1],
          ])
        }
        className="product-price-range"
        aria-label="Giá thấp nhất"
      />

      {/* max */}
      <input
        type="range"
        min={0}
        max={5000000}
        step={step}
        value={priceRange[1]}
        onChange={(e) =>
          setPriceRange([
            priceRange[0],
            Math.max(Number(e.target.value), priceRange[0] + step),
          ])
        }
        className="product-price-range"
        aria-label="Giá cao nhất"
      />
    </div>
  );
}
