import { useEffect, useRef } from "react";
import { LoaderCircle, PackageSearch, RefreshCw } from "lucide-react";
import ProductCard from "./ProductCard";
import Pagination from "../../common/Pagination";
import Button from "../../common/Button";

function SkeletonCards({ count }) {
  return Array.from({ length: count }, (_, index) => (
    <div key={index} className="customer-card product-card product-card--skeleton" aria-hidden="true">
      <div className="product-card__media" />
      <div className="product-card__content">
        <span />
        <span />
      </div>
    </div>
  ));
}

/**
 * Lưới sản phẩm phân trang phía server. Máy tính: phân trang theo trang. Điện thoại: cuộn tới cuối thì tải
 * trang tiếp theo và nối vào danh sách ({@code mobileProducts}).
 */
export default function RemoteProductGrid({
  products, mobileProducts, page, totalPages, totalElements, loading, error, onPageChange, onLoadMore, onRetry,
}) {
  const loadMoreRef = useRef(null);
  const hasMore = page < totalPages;

  useEffect(() => {
    const target = loadMoreRef.current;
    if (!target || !hasMore || loading || !window.matchMedia("(max-width: 639px)").matches) return undefined;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) onLoadMore();
    }, { rootMargin: "0px 0px 600px", threshold: 0 });
    observer.observe(target);
    return () => observer.disconnect();
  }, [hasMore, loading, onLoadMore]);

  if (error && !products.length) {
    return (
      <div className="flex min-h-72 flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-zinc-700 px-5 text-center">
        <PackageSearch className="text-zinc-500" size={34} />
        <p className="text-sm text-zinc-400">{error}</p>
        <Button variant="secondary" className="inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm" onClick={onRetry}>
          <RefreshCw size={15} /> Thử lại
        </Button>
      </div>
    );
  }

  if (!loading && totalElements === 0) {
    return (
      <div className="flex min-h-72 flex-col items-center justify-center rounded-2xl border border-dashed border-zinc-700 px-5 text-center">
        <PackageSearch className="mb-3 text-zinc-500" size={34} />
        <h2 className="font-semibold text-zinc-200">Không tìm thấy sản phẩm</h2>
        <p className="mt-1 text-sm text-zinc-500">Hãy thử từ khóa hoặc bộ lọc khác.</p>
      </div>
    );
  }

  const showSkeleton = loading && !products.length;
  return (
    <div aria-busy={loading || undefined}>
      <div className="grid grid-cols-2 gap-3 sm:hidden">
        {mobileProducts.map((product) => <ProductCard key={product.id} product={product} />)}
        {showSkeleton && <SkeletonCards count={4} />}
      </div>

      <div className={`hidden gap-5 sm:grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 ${loading && products.length ? "opacity-60 transition-opacity" : ""}`}>
        {showSkeleton ? <SkeletonCards count={8} /> : products.map((product) => <ProductCard key={product.id} product={product} />)}
      </div>

      <div ref={loadMoreRef} className="flex min-h-20 items-center justify-center sm:hidden" aria-live="polite">
        {loading && mobileProducts.length > 0 && (
          <span className="flex items-center gap-2 text-sm text-zinc-400">
            <LoaderCircle className="animate-spin" size={18} /> Đang tải thêm sản phẩm...
          </span>
        )}
        {!loading && !hasMore && mobileProducts.length > 0 && (
          <span className="text-sm text-zinc-500">Bạn đã xem hết {totalElements} sản phẩm</span>
        )}
        {error && mobileProducts.length > 0 && (
          <Button variant="unstyled" className="text-sm text-amber-400 underline" onClick={onRetry}>Tải lại</Button>
        )}
      </div>

      <div className="mt-8 hidden sm:block">
        <Pagination currentPage={page} totalPages={totalPages} onPageChange={onPageChange} />
      </div>
    </div>
  );
}
