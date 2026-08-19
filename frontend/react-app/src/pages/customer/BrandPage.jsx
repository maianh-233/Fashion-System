import { useState } from "react";
import { LoaderCircle, Search, Store } from "lucide-react";
import BrandCard from "../../components/customer/Brand/BrandCard";
import Pagination from "../../components/common/Pagination";
import { useMobileInfiniteList } from "../../hooks/useMobileInfiniteList";

const ITEMS_PER_PAGE = 12;
const BRAND_NAMES = [
  "Nike",
  "Adidas",
  "Gucci",
  "Lunaria",
  "Local Brand",
  "Urban Studio",
  "Maison Élise",
  "New Balance",
];

const MOCK_BRANDS = Array.from({ length: 32 }, (_, index) => ({
  id: index + 1,
  name: `${BRAND_NAMES[index % BRAND_NAMES.length]} ${index + 1}`,
  code: `BRAND_${String(index + 1).padStart(2, "0")}`,
  description: "Thiết kế hiện đại dành cho phong cách sống năng động.",
  logo: `https://picsum.photos/seed/lunaria-brand-${index + 1}/500/300`,
}));

export default function BrandPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const filteredBrands = MOCK_BRANDS.filter((brand) =>
    `${brand.name} ${brand.code}`
      .toLocaleLowerCase("vi")
      .includes(search.trim().toLocaleLowerCase("vi")),
  );
  const {
    visibleCount,
    isLoadingMore,
    hasMore,
    loadMoreRef,
    reset,
  } = useMobileInfiniteList(filteredBrands.length);
  const totalPages = Math.max(
    1,
    Math.ceil(filteredBrands.length / ITEMS_PER_PAGE),
  );
  const safePage = Math.min(page, totalPages);
  const desktopBrands = filteredBrands.slice(
    (safePage - 1) * ITEMS_PER_PAGE,
    safePage * ITEMS_PER_PAGE,
  );
  const mobileBrands = filteredBrands.slice(0, visibleCount);

  const handleSearch = (event) => {
    setSearch(event.target.value);
    setPage(1);
    reset();
  };

  return (
    <div className="min-h-screen w-full overflow-x-clip bg-zinc-950 text-zinc-200">
      <header className="border-b border-zinc-800 bg-zinc-900">
        <div className="px-4 py-4 sm:px-8 sm:py-6">
          <h1 className="text-xl font-semibold sm:text-2xl">Thương hiệu</h1>
          <p className="mt-1 text-sm text-zinc-400">
            Khám phá {filteredBrands.length} thương hiệu nổi bật
          </p>
          <div className="relative mt-4">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" size={18} />
            <input
              value={search}
              onChange={handleSearch}
              placeholder="Tìm kiếm thương hiệu, mã code..."
              className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 py-2.5 pl-11 pr-4 focus:border-amber-400 focus:outline-none sm:rounded-2xl"
            />
          </div>
        </div>
      </header>

      <main className="px-4 py-5 sm:px-8 sm:py-8">
        {filteredBrands.length === 0 ? (
          <div className="flex min-h-72 flex-col items-center justify-center text-center text-zinc-500">
            <Store className="mb-3" size={34} />
            <p>Không tìm thấy thương hiệu phù hợp.</p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 gap-4 sm:hidden">
              {mobileBrands.map((brand) => <BrandCard key={brand.id} brand={brand} />)}
            </div>
            <div className="hidden gap-5 sm:grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {desktopBrands.map((brand) => <BrandCard key={brand.id} brand={brand} />)}
            </div>
            <MobileLoadStatus
              itemLabel="thương hiệu"
              loadMoreRef={loadMoreRef}
              isLoadingMore={isLoadingMore}
              hasMore={hasMore}
              total={filteredBrands.length}
            />
          </>
        )}
      </main>

      {filteredBrands.length > 0 && (
        <div className="hidden border-t border-zinc-800 bg-zinc-900 px-4 py-3 sm:block">
          <Pagination currentPage={safePage} totalPages={totalPages} onPageChange={setPage} />
        </div>
      )}
    </div>
  );
}

function MobileLoadStatus({ loadMoreRef, isLoadingMore, hasMore, total, itemLabel }) {
  return (
    <div ref={loadMoreRef} className="flex min-h-20 items-center justify-center sm:hidden" aria-live="polite">
      {isLoadingMore && (
        <span className="flex items-center gap-2 text-sm text-zinc-400">
          <LoaderCircle className="animate-spin" size={18} /> Đang tải thêm...
        </span>
      )}
      {!hasMore && <span className="text-sm text-zinc-500">Bạn đã xem hết {total} {itemLabel}</span>}
    </div>
  );
}
