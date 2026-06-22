import { useState } from "react";
import BrandCard from "../../components/customer/Brand/BrandCard";
import Pagination from "../../components/common/Pagination";

export default function BrandPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");

  const mockBrands = Array.from({ length: 8 }).map((_, i) => ({
    id: i + 1,
    name: `Brand ${i + 1}`,
    code: `BRAND_${i + 1}`,
    logo: "https://upload.wikimedia.org/wikipedia/commons/a/a6/Logo_NIKE.svg",
  }));

  return (
    <div className="min-h-screen w-full overflow-x-hidden bg-zinc-950 text-zinc-200 flex flex-col">

      {/* HEADER */}
      <header className="shrink-0 border-b border-zinc-800 bg-zinc-900 sticky top-0 z-40">
        <div className="px-4 sm:px-8 py-4 sm:py-5">
          <div className="flex-1 relative">
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm kiếm thương hiệu, mã code..."
              className="w-full bg-zinc-800 border border-zinc-700 rounded-xl sm:rounded-2xl px-4 sm:px-5 py-2.5 sm:py-3 pl-10 sm:pl-12 focus:outline-none focus:border-amber-400"
            />
            <i className="fas fa-search absolute left-3 sm:left-4 top-1/2 -translate-y-1/2 text-zinc-400" />
          </div>
        </div>
      </header>

      {/* MAIN (ONLY SCROLL AREA) */}
      <main className="flex-1 overflow-y-auto p-4 sm:p-8">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 overflow-y-auto scrollbar-hide max-h-[calc(100vh-200px)]">
          {mockBrands.map((brand) => (
            <BrandCard key={brand.id} brand={brand} />
          ))}
        </div>
      </main>

      {/* FOOTER */}
      <footer className="shrink-0 border-t border-zinc-800 bg-zinc-900">
        <div className="flex justify-center">
          <Pagination
            currentPage={page}
            totalPages={5}
            onPageChange={setPage}
          />
        </div>
      </footer>
    </div>
  );
}