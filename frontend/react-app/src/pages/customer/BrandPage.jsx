import { useState } from "react";
import BrandCard from "../../components/customer/Brand/BrandCard";
import SearchTypeDropdown from "../../components/customer/Product/SearchTypeDropdown";
import Pagination from "../../components/common/Pagination";

export default function BrandPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [searchType, setSearchType] = useState("brand");

  // ===== MOCK DATA =====
  const mockBrands = Array.from({ length: 8 }).map((_, i) => ({
    id: i + 1,
    name: `Brand ${i + 1}`,
    code: `BRAND_${i + 1}`,
    logo: "https://upload.wikimedia.org/wikipedia/commons/a/a6/Logo_NIKE.svg",
  }));

  return (
    <div className="w-screen min-h-screen bg-zinc-950 text-zinc-200 flex flex-col">
      {/* HEADER */}
      <header className="border-b border-zinc-800 bg-zinc-900 sticky top-0 z-40">
        <div className="px-4 sm:px-8 py-4 sm:py-5">
          <div className="flex flex-col xl:flex-row xl:items-center gap-4">
            <div className="flex-1 relative">
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm kiếm thương hiệu, mã code..."
                className="
              w-full bg-zinc-800 border border-zinc-700
              rounded-xl sm:rounded-2xl
              px-4 sm:px-5 py-2.5 sm:py-3 pl-10 sm:pl-12
              focus:outline-none focus:border-amber-400
            "
              />
              <i className="fas fa-search absolute left-3 sm:left-4 top-1/2 -translate-y-1/2 text-zinc-400" />
            </div>
          </div>
        </div>
      </header>

      {/* BODY */}
      <div className="flex-1 overflow-y-auto">
        <main className="p-4 sm:p-8">
          {/* GRID */}
          <div
            className="
    grid gap-4 sm:gap-6

    grid-cols-1
    sm:grid-cols-2
    lg:grid-cols-[repeat(auto-fill,minmax(280px,1fr))]

    overflow-y-auto
    max-h-[calc(100vh-200px)]

    scrollbar-none
    [&::-webkit-scrollbar]:hidden
    [-ms-overflow-style:none]
    [scrollbar-width:none]
  "
          >
            {mockBrands.map((brand) => (
              <BrandCard key={brand.id} brand={brand} />
            ))}
          </div>

          {/* PAGINATION */}
          <div className="mt-10 flex justify-center">
            <Pagination
              currentPage={page}
              totalPages={5}
              onPageChange={setPage}
            />
          </div>
        </main>
      </div>
    </div>
  );
}
