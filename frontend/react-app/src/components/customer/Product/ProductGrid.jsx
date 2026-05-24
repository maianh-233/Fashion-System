import ProductCard from "./ProductCard";
import Pagination from "../../common/Pagination";
export default function ProductGrid({ products, page, setPage }) {
  return (
    <>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 overflow-y-auto scrollbar-hide max-h-[calc(100vh-200px)]">
        {products.map((p) => (
            <ProductCard key={p.id} product={p} />
        ))}
        </div>

      <div className="mt-6">
        <Pagination
          currentPage={page}
          totalPages={5}
          onPageChange={setPage}
        />
      </div>
    </>
  );
}