import { useState } from "react";
import CollectionCard from "../Collection/CollectionCard";
import Pagination from "../../common/Pagination";

const PAGE_SIZE = 4;

export default function CollectionSection({ collections }) {
  const [page, setPage] = useState(1);

  const totalPages = Math.ceil(collections.length / PAGE_SIZE);

  const pagedData = collections.slice(
    (page - 1) * PAGE_SIZE,
    page * PAGE_SIZE
  );

  return (
    <section id="collections" className="mb-20">
      
      {/* HEADER */}
      <div className="flex justify-between items-end mb-8">
        <h2 className="text-3xl font-bold">Bộ sưu tập nổi bật</h2>
      </div>

      {/* GRID */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
        {pagedData.map((collection) => (
          <CollectionCard
            key={collection.id}
            collection={collection}
          />
        ))}
      </div>

      {/* PAGINATION */}
      <Pagination
        currentPage={page}
        totalPages={totalPages}
        onPageChange={setPage}
      />
    </section>
  );
}