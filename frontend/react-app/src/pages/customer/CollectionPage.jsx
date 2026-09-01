import { useState } from "react";
import { Images, LoaderCircle, Search } from "lucide-react";
import CollectionCard from "../../components/customer/Collection/CollectionCard";
import Pagination from "../../components/common/Pagination";
import { useMobileInfiniteList } from "../../hooks/useMobileInfiniteList";
import CustomerPageIntro from "../../components/customer/CustomerPageIntro";

const ITEMS_PER_PAGE = 12;
const COLLECTION_IMAGES = [
  "https://images.unsplash.com/photo-1445205170230-053b83016050?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1483985988355-763728e1935b?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1525507119028-ed4bd977a94a?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?q=80&w=800&auto=format&fit=crop",
];
const COLLECTIONS = Array.from({ length: 36 }, (_, index) => ({
  id: `collection-${index + 1}`,
  name: `Bộ sưu tập ${String(index + 1).padStart(2, "0")}`,
  brand: ["GUCCI", "LUNARIA", "MAISON ÉLISE"][index % 3],
  season: ["Spring", "Summer", "Fall", "Winter"][index % 4],
  year: 2024 + (index % 3),
  cover_image: COLLECTION_IMAGES[index % COLLECTION_IMAGES.length],
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
    <div className="customer-page min-h-screen w-full overflow-x-clip text-zinc-200">
      <div className="customer-page__wide">
        <CustomerPageIntro eyebrow="Seasonal stories" title="Bộ sưu tập" description="Mỗi bộ sưu tập là một câu chuyện riêng về chất liệu, màu sắc và tinh thần mùa." meta={`${filteredCollections.length} bộ sưu tập`}>
          <div className="customer-search-control relative w-full">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" size={18} />
            <input
              value={search}
              onChange={handleSearch}
              placeholder="Tìm kiếm bộ sưu tập hoặc thương hiệu..."
              className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 py-2.5 pl-11 pr-4 focus:border-amber-400 focus:outline-none sm:rounded-2xl"
            />
          </div>
        </CustomerPageIntro>
      </div>

      <main className="customer-page__wide px-4 py-5 sm:px-6 sm:py-7">
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
        <div className="customer-page__wide hidden px-4 py-5 sm:block">
          <Pagination currentPage={safePage} totalPages={totalPages} onPageChange={setCurrentPage} />
        </div>
      )}
    </div>
  );
}
