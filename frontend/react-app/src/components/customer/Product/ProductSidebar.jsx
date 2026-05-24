import CategoryFilter from "./CategoryFilter";
import PriceRangeSlider from "./PriceRangeSlider";

export default function ProductSidebar({ priceRange, setPriceRange }) {
  return (
    <div className="p-4 w-full text-sm">
      <h2 className="text-lg font-bold mb-4">Bộ lọc</h2>

      <CategoryFilter />

      <PriceRangeSlider
        priceRange={priceRange}
        setPriceRange={setPriceRange}
      />

      <button className="w-full bg-amber-400 text-black py-2 rounded-xl mt-4">
        Áp dụng
      </button>
    </div>
  );
}