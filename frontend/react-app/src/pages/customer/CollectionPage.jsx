import { useMemo, useState, useEffect } from "react";
import CollectionCard from "../../components/customer/Collection/CollectionCard";
import Pagination from "../../components/common/Pagination";

export default function CollectionPage() {
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const ITEMS_PER_PAGE = 6; // card nhỏ, 3x2

  /* ================= MOCK DATA ================= */
  const collections = useMemo(
    () =>
      Array.from({ length: 12 }).map((_, i) => ({
        id: `collection-${i + 1}`,
        name: `Collection ${i + 1}`,
        brand: "GUCCI",
        season: ["Spring", "Summer", "Fall", "Winter"][i % 4],
        year: 2023 + (i % 3),
        cover_image: `https://picsum.photos/400/500?random=${i + 1}`,
      })),
    []
  );

  /* ================= FILTER ================= */
  const filteredCollections = collections.filter((c) =>
    c.name.toLowerCase().includes(search.toLowerCase())
  );

  /* reset page khi search */
  useEffect(() => {
    setCurrentPage(1);
  }, [search]);

  const totalPages = Math.ceil(filteredCollections.length / ITEMS_PER_PAGE);

  const paginatedCollections = filteredCollections.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE
  );

  return (
    <div className="min-h-screen w-full overflow-x-hidden bg-zinc-950 text-zinc-200 flex flex-col">
      {/* ================= HEADER ================= */}
      <header className="border-b border-zinc-800 bg-zinc-900 sticky top-0 z-40">
        <div className="px-6 sm:px-8 py-5">
          <div className="flex flex-col xl:flex-row xl:items-center gap-4">
            {/* Search */}
            <div className="flex-1 relative">
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm kiếm bộ sưu tập..."
                className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3 pl-12 focus:outline-none focus:border-amber-400"
              />
              <i className="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" />
            </div>
          </div>
        </div>
      </header>

      {/* CONTENT */}
    <main className="flex-1 px-6 sm:px-8 py-8 overflow-y-auto">
      {paginatedCollections.length === 0 ? (
        <div className="text-center text-zinc-500 mt-20">
          Không tìm thấy bộ sưu tập phù hợp
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 overflow-y-auto scrollbar-hide max-h-[calc(100vh-200px)]">
          {paginatedCollections.map((collection) => (
            <CollectionCard
              key={collection.id}
              collection={collection}
            />
          ))}
        </div>
      )}
    </main>

    {/* FOOTER */}
    <footer className="shrink-0 border-t border-zinc-800 bg-zinc-900">
      <Pagination
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={setCurrentPage}
      />
    </footer>
    </div>
  );
}