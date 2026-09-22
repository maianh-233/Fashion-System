import { useState } from "react";
import { LoaderCircle, Search, Store } from "lucide-react";
import BrandCard from "../../components/customer/Brand/BrandCard";
import Pagination from "../../components/common/Pagination";
import { useMobileInfiniteList } from "../../hooks/useMobileInfiniteList";
import CustomerPageIntro from "../../components/customer/CustomerPageIntro";
import { brands as PARTNER_BRANDS } from "../../mock/storefrontData";

const ITEMS_PER_PAGE = 12;
const MOCK_BRANDS = PARTNER_BRANDS.map((brand, index) => ({
  ...brand,
  code: `MAISON_${String(index + 1).padStart(2, "0")}`,
  description: "Di sản chế tác và ngôn ngữ thiết kế đã định hình thời trang đương đại.",
  status: "active",
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
    <div className="customer-page min-h-screen w-full overflow-x-clip text-zinc-200">
      <div className="customer-page__wide">
        <CustomerPageIntro eyebrow="Curated maisons" title="Thương hiệu" description="Những nhà mốt được chọn lọc bởi ngôn ngữ thiết kế và chất lượng chế tác." meta={`${filteredBrands.length} thương hiệu`}>
          <div className="customer-search-control relative w-full">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" size={18} />
            <input
              value={search}
              onChange={handleSearch}
              placeholder="Tìm kiếm thương hiệu, mã code..."
              className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 py-2.5 pl-11 pr-4 focus:border-amber-400 focus:outline-none sm:rounded-2xl"
            />
          </div>
        </CustomerPageIntro>
      </div>

      <main className="customer-page__wide px-4 py-5 sm:px-6 sm:py-7">
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
        <div className="customer-page__wide hidden px-4 py-5 sm:block">
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
