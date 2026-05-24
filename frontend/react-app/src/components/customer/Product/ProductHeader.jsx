import SearchBar from "./SearchBar";
import SearchTypeDropdown from "./SearchTypeDropdown";

export default function ProductHeader({
  search,
  setSearch,
  searchType,
  setSearchType,
  setOpenFilter, // ✅ THÊM DÒNG NÀY
}) {
  return (
    <header className="border-b border-zinc-800 bg-zinc-900">
      <div className="px-4 py-3 sm:px-6 sm:py-4 lg:p-6">
        
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center">

          {/* Search */}
          <div className="flex-1">
            <SearchBar
              value={search}
              onChange={setSearch}
              placeholder="Tìm kiếm sản phẩm..."
            />
          </div>

          {/* Dropdown */}
          <div className="w-full lg:w-auto">
            <SearchTypeDropdown
              value={searchType}
              onChange={setSearchType}
            />
          </div>

          {/* BUTTON FILTER MOBILE */}
          <button
            className="lg:hidden px-4 py-2 bg-amber-400 rounded-xl"
            onClick={() => setOpenFilter(true)}  // ✅ OK sau khi fix
          >
            Bộ lọc
          </button>

        </div>

      </div>
    </header>
  );
}