import Button from "../../common/Button";
import SearchBar from "./SearchBar";
import SearchTypeDropdown from "./SearchTypeDropdown";
import { SlidersHorizontal } from "lucide-react";

export default function ProductHeader({
  search,
  setSearch,
  searchType,
  setSearchType,
  setOpenFilter,
  productCount = 0,
}) {
  return (
    <header className="border-b border-zinc-800 bg-zinc-900">
      <div className="px-4 py-4 sm:px-6 lg:p-6">
        <div className="mb-4">
          <h1 className="text-xl font-semibold text-zinc-100 sm:text-2xl">
            Sản phẩm
          </h1>
          <p className="mt-1 text-sm text-zinc-400">
            Khám phá {productCount} sản phẩm dành cho bạn
          </p>
        </div>

        <div className="flex flex-col gap-3 lg:flex-row lg:items-center">

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
              className="flex min-h-11 shrink-0 items-center gap-2 rounded-xl bg-amber-400 px-4 py-2 lg:hidden"
              onClick={() => setOpenFilter(true)}
              aria-label="Mở bộ lọc sản phẩm"
            >
              <SlidersHorizontal size={18} />
              <span className="hidden min-[380px]:inline">Bộ lọc</span>
            </Button>
          </div>

        </div>

      </div>
    </header>
  );
}
