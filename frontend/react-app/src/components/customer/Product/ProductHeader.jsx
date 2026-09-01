import Button from "../../common/Button";
import SearchBar from "./SearchBar";
import SearchTypeDropdown from "./SearchTypeDropdown";
import { SlidersHorizontal } from "lucide-react";
import CustomerPageIntro from "../CustomerPageIntro";

export default function ProductHeader({
  search,
  setSearch,
  searchType,
  setSearchType,
  setOpenFilter,
  productCount = 0,
  activeFilterCount = 0,
}) {
  return (
    <CustomerPageIntro
      eyebrow="Lunaria selection"
      title="Sản phẩm"
      description="Những thiết kế được tuyển chọn cho tủ đồ hiện đại."
      meta={`${productCount} sản phẩm`}
    >
        <div className="flex w-full flex-col gap-3 lg:flex-row lg:items-center">

          {/* Search */}
          <div className="flex-1">
            <SearchBar
              value={search}
              onChange={setSearch}
              placeholder="Tìm kiếm sản phẩm..."
            />
          </div>

          <div className="flex w-full gap-3 lg:w-auto">
            <div className="min-w-0 flex-1 lg:w-64">
            <SearchTypeDropdown
              value={searchType}
              onChange={setSearchType}
            />
            </div>

            <Button
              variant="unstyled"
              className="promotion-filter-trigger promotion-filter-trigger--product"
              onClick={() => setOpenFilter(true)}
              aria-label="Mở bộ lọc sản phẩm"
            >
              <SlidersHorizontal size={18} />
              {activeFilterCount > 0 && <span>{activeFilterCount}</span>}
            </Button>
          </div>

        </div>
    </CustomerPageIntro>
  );
}
