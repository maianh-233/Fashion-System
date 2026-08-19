import { useEffect, useRef, useState } from "react";
import { LoaderCircle, PackageSearch } from "lucide-react";
import ProductCard from "./ProductCard";
import Pagination from "../../common/Pagination";

const DESKTOP_PAGE_SIZE = 12;
const MOBILE_INITIAL_COUNT = 8;
const MOBILE_BATCH_SIZE = 6;

export default function ProductGrid({ products, page, setPage }) {
  const [mobileVisibleCount, setMobileVisibleCount] = useState(
    MOBILE_INITIAL_COUNT,
  );
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const loadMoreRef = useRef(null);
  const loadingRef = useRef(false);

  const totalPages = Math.max(
    1,
    Math.ceil(products.length / DESKTOP_PAGE_SIZE),
  );
  const safePage = Math.min(page, totalPages);
  const desktopProducts = products.slice(
    (safePage - 1) * DESKTOP_PAGE_SIZE,
    safePage * DESKTOP_PAGE_SIZE,
  );
  const mobileProducts = products.slice(0, mobileVisibleCount);
  const hasMoreMobile = mobileVisibleCount < products.length;

  useEffect(() => {
    const target = loadMoreRef.current;
    const isMobile = window.matchMedia("(max-width: 639px)").matches;
    if (!target || !isMobile || !hasMoreMobile) return undefined;

    let loadingTimer;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting || loadingRef.current) return;

        loadingRef.current = true;
        setIsLoadingMore(true);
        loadingTimer = window.setTimeout(() => {
          setMobileVisibleCount((current) =>
            Math.min(current + MOBILE_BATCH_SIZE, products.length),
          );
          setIsLoadingMore(false);
          loadingRef.current = false;
        }, 400);
      },
      { rootMargin: "0px 0px 200px", threshold: 0.1 },
    );

    observer.observe(target);
    return () => {
      observer.disconnect();
      window.clearTimeout(loadingTimer);
    };
  }, [hasMoreMobile, mobileVisibleCount, products.length]);

  if (products.length === 0) {
    return (
      <div className="flex min-h-72 flex-col items-center justify-center rounded-2xl border border-dashed border-zinc-700 px-5 text-center">
        <PackageSearch className="mb-3 text-zinc-500" size={34} />
        <h2 className="font-semibold text-zinc-200">Không tìm thấy sản phẩm</h2>
        <p className="mt-1 text-sm text-zinc-500">
          Hãy thử từ khóa hoặc bộ lọc khác.
        </p>
      </div>
    );
  }

  return (
    <>
      <div className="grid grid-cols-2 gap-3 sm:hidden">
        {mobileProducts.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>

      <div className="hidden gap-5 sm:grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {desktopProducts.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>

      <div
        ref={loadMoreRef}
        className="flex min-h-20 items-center justify-center sm:hidden"
        aria-live="polite"
      >
        {isLoadingMore && (
          <span className="flex items-center gap-2 text-sm text-zinc-400">
            <LoaderCircle className="animate-spin" size={18} />
            Đang tải thêm sản phẩm...
          </span>
        )}
        {!hasMoreMobile && (
          <span className="text-sm text-zinc-500">
            Bạn đã xem hết {products.length} sản phẩm
          </span>
        )}
      </div>

      <div className="mt-8 hidden sm:block">
        <Pagination
          currentPage={safePage}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      </div>
    </>
  );
}
