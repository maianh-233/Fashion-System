import Button from "../../common/Button";
import CategoryFilter from "./CategoryFilter";
import CategoryTreeFilter from "./CategoryTreeFilter";
import PriceRangeSlider from "./PriceRangeSlider";
import { X } from "lucide-react";

/**
 * Bộ lọc sản phẩm. Các phần nối API (giới tính, cây danh mục, sắp xếp, còn hàng) chỉ hiện khi trang truyền dữ liệu;
 * trang chưa nối API dùng như trước.
 */
export default function ProductSidebar({
  priceRange,
  setPriceRange,
  priceBounds,
  selectedCategories = [],
  setSelectedCategories = () => {},
  genderOptions,
  categoryTree,
  selectedCategoryIds = [],
  setSelectedCategoryIds = () => {},
  sortOptions,
  sort,
  setSort = () => {},
  inStockOnly,
  setInStockOnly,
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

      {sortOptions && (
        <div className="customer-filter__section mb-6">
          <h3>Sắp xếp</h3>
          <select className="customer-filter__select" value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sắp xếp sản phẩm">
            {sortOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </div>
      )}

      {showCategories && (
        <CategoryFilter
          selected={selectedCategories}
          onChange={setSelectedCategories}
          {...(genderOptions ? { options: genderOptions, title: "Dành cho" } : {})}
        />
      )}

      {categoryTree && (
        <CategoryTreeFilter categories={categoryTree} selected={selectedCategoryIds} onChange={setSelectedCategoryIds} />
      )}

      <PriceRangeSlider
        priceRange={priceRange}
        setPriceRange={setPriceRange}
        {...(priceBounds ? { min: priceBounds[0], max: priceBounds[1] } : {})}
      />

      {setInStockOnly && (
        <div className="customer-filter__section mb-6">
          <label className="customer-filter__option flex items-center gap-2">
            <input type="checkbox" checked={Boolean(inStockOnly)} onChange={(e) => setInStockOnly(e.target.checked)} />
            Chỉ hiện sản phẩm còn hàng
          </label>
        </div>
      )}

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
