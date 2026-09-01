import Button from "../../common/Button";
import CategoryFilter from "./CategoryFilter";
import PriceRangeSlider from "./PriceRangeSlider";
import { X } from "lucide-react";

export default function ProductSidebar({
  priceRange,
  setPriceRange,
  selectedCategories = [],
  setSelectedCategories = () => {},
  activeFilterCount = 0,
  resultCount,
  onReset = () => {},
  onClose,
  showCategories = true,
}) {
  return (
    <div className="customer-filter w-full text-sm">
      <div className="customer-filter__heading product-filter-heading">
        <div>
          <span>Tinh chỉnh</span>
          <h2>Bộ lọc</h2>
        </div>
        {onClose && (
          <Button
            type="button"
            variant="unstyled"
            className="promotion-filter-close lg:hidden"
            onClick={onClose}
            aria-label="Đóng bộ lọc sản phẩm"
          >
            <X size={18} />
          </Button>
        )}
      </div>

      {showCategories && (
        <CategoryFilter
          selected={selectedCategories}
          onChange={setSelectedCategories}
        />
      )}

      <PriceRangeSlider
        priceRange={priceRange}
        setPriceRange={setPriceRange}
      />

      <div className="promotion-filter-actions">
        <Button
          type="button"
          variant="unstyled"
          className="customer-filter__clear w-full text-sm"
          onClick={onReset}
          disabled={activeFilterCount === 0}
        >
          Xóa bộ lọc
        </Button>
        {onClose && (
          <Button
            type="button"
            variant="unstyled"
            className="customer-filter__apply w-full text-sm lg:hidden"
            onClick={onClose}
          >
            Xem {resultCount ?? 0} sản phẩm
          </Button>
        )}
      </div>
    </div>
  );
}
