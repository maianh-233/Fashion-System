import { useState } from "react";
import { Images, LoaderCircle, Search } from "lucide-react";
import CollectionCard from "../../components/customer/Collection/CollectionCard";
import Pagination from "../../components/common/Pagination";
import { useMobileInfiniteList } from "../../hooks/useMobileInfiniteList";

const ITEMS_PER_PAGE = 12;
const COLLECTIONS = Array.from({ length: 36 }, (_, index) => ({
  id: `collection-${index + 1}`,
  name: `Bộ sưu tập ${String(index + 1).padStart(2, "0")}`,
  brand: ["GUCCI", "LUNARIA", "MAISON ÉLISE"][index % 3],
  season: ["Spring", "Summer", "Fall", "Winter"][index % 4],
  year: 2024 + (index % 3),
  cover_image: `https://picsum.photos/seed/lunaria-collection-${index + 1}/600/750`,
}));

export default function CollectionPage() {
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const filteredCollections = COLLECTIONS.filter((collection) =>
    `${collection.name} ${collection.brand}`
      .toLocaleLowerCase("vi")
      .includes(search.trim().toLocaleLowerCase("vi")),
  );
  const {
    visibleCount,
    isLoadingMore,
    hasMore,
    loadMoreRef,
    reset,
  } = useMobileInfiniteList(filteredCollections.length);
  const totalPages = Math.max(
    1,
    Math.ceil(filteredCollections.length / ITEMS_PER_PAGE),
  );
  const safePage = Math.min(currentPage, totalPages);
  const desktopCollections = filteredCollections.slice(
    (safePage - 1) * ITEMS_PER_PAGE,
    safePage * ITEMS_PER_PAGE,
  );
  const mobileCollections = filteredCollections.slice(0, visibleCount);

  const handleSearch = (event) => {
    setSearch(event.target.value);
    setCurrentPage(1);
    reset();
  };

  return (
    <div className="min-h-screen w-full overflow-x-clip bg-zinc-950 text-zinc-200">
      <header className="border-b border-zinc-800 bg-zinc-900">
        <div className="px-4 py-4 sm:px-8 sm:py-6">
          <h1 className="text-xl font-semibold sm:text-2xl">Bộ sưu tập</h1>
          <p className="mt-1 text-sm text-zinc-400">
            {filteredCollections.length} bộ sưu tập đang chờ bạn khám phá
          </p>
          <div className="relative mt-4">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" size={18} />
            <input
              value={search}
              onChange={handleSearch}
              placeholder="Tìm kiếm bộ sưu tập hoặc thương hiệu..."
              className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 py-2.5 pl-11 pr-4 focus:border-amber-400 focus:outline-none sm:rounded-2xl"
            />
          </div>
        </div>
      </header>

      <main className="px-4 py-5 sm:px-8 sm:py-8">
        {filteredCollections.length === 0 ? (
          <div className="flex min-h-72 flex-col items-center justify-center text-center text-zinc-500">
            <Images className="mb-3" size={34} />
            <p>Không tìm thấy bộ sưu tập phù hợp.</p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 gap-4 sm:hidden">
              {mobileCollections.map((collection) => (
                <CollectionCard key={collection.id} collection={collection} />
              ))}
            </div>
            <div className="hidden gap-5 sm:grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {desktopCollections.map((collection) => (
                <CollectionCard key={collection.id} collection={collection} />
              ))}
            </div>
            <div ref={loadMoreRef} className="flex min-h-20 items-center justify-center sm:hidden" aria-live="polite">
              {isLoadingMore && (
                <span className="flex items-center gap-2 text-sm text-zinc-400">
                  <LoaderCircle className="animate-spin" size={18} /> Đang tải thêm...
                </span>
              )}
              {!hasMore && (
                <span className="text-sm text-zinc-500">Bạn đã xem hết {filteredCollections.length} bộ sưu tập</span>
              )}
            </div>
          </>
        )}
      </main>

      {filteredCollections.length > 0 && (
        <div className="hidden border-t border-zinc-800 bg-zinc-900 px-4 py-3 sm:block">
          <Pagination currentPage={safePage} totalPages={totalPages} onPageChange={setCurrentPage} />
        </div>
      )}
    </div>
  );
}
