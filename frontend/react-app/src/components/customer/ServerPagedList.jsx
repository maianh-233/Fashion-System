import { useEffect, useRef } from "react";
import { LoaderCircle, RefreshCw } from "lucide-react";
import Pagination from "../common/Pagination";
import Button from "../common/Button";

/**
 * Lưới dữ liệu phân trang phía server (dùng với {@link useServerPages}): máy tính phân trang, điện thoại cuộn tải
 * thêm. {@code renderItem} dựng một ô; {@code Empty} là nội dung khi không có kết quả.
 */
export default function ServerPagedList({
  list, renderItem, itemLabel, emptyIcon: EmptyIcon, emptyText,
  desktopClassName = "hidden gap-5 sm:grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4",
  mobileClassName = "grid grid-cols-1 gap-4 sm:hidden",
  skeleton,
  onPageChange,
}) {
  const loadMoreRef = useRef(null);
  const { page, totalPages, totalElements, items, mobileItems, loading, error, retry, loadMore, loaded } = list;
  const hasMore = page < totalPages;

  useEffect(() => {
    const target = loadMoreRef.current;
    if (!target || !hasMore || loading || !window.matchMedia("(max-width: 639px)").matches) return undefined;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) loadMore();
    }, { rootMargin: "0px 0px 600px", threshold: 0 });
    observer.observe(target);
    return () => observer.disconnect();
  }, [hasMore, loading, loadMore]);

  if (error && !items.length && !mobileItems.length) {
    return (
      <div className="flex min-h-72 flex-col items-center justify-center gap-3 text-center text-zinc-500">
        {EmptyIcon && <EmptyIcon size={34} />}
        <p className="text-sm">{error}</p>
        <Button variant="secondary" className="inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm" onClick={retry}>
          <RefreshCw size={15} /> Thử lại
        </Button>
      </div>
    );
  }
  if (loaded && !loading && totalElements === 0) {
    return (
      <div className="flex min-h-72 flex-col items-center justify-center text-center text-zinc-500">
        {EmptyIcon && <EmptyIcon className="mb-3" size={34} />}
        <p>{emptyText}</p>
      </div>
    );
  }

  const placeholders = (count) => Array.from({ length: count }, (_, index) => (
    <div key={`skeleton-${index}`} aria-hidden="true">{skeleton}</div>
  ));
  const firstLoad = loading && !items.length;
  return (
    <div aria-busy={loading || undefined}>
      <div className={mobileClassName}>
        {mobileItems.map(renderItem)}
        {firstLoad && !mobileItems.length && skeleton && placeholders(3)}
      </div>
      <div className={`${desktopClassName} ${loading && items.length ? "opacity-60 transition-opacity" : ""}`}>
        {firstLoad ? skeleton && placeholders(8) : items.map(renderItem)}
      </div>

      <div ref={loadMoreRef} className="flex min-h-20 items-center justify-center sm:hidden" aria-live="polite">
        {loading && mobileItems.length > 0 && (
          <span className="flex items-center gap-2 text-sm text-zinc-400">
            <LoaderCircle className="animate-spin" size={18} /> Đang tải thêm...
          </span>
        )}
        {!loading && !hasMore && mobileItems.length > 0 && (
          <span className="text-sm text-zinc-500">Bạn đã xem hết {totalElements} {itemLabel}</span>
        )}
        {error && mobileItems.length > 0 && (
          <Button variant="unstyled" className="text-sm text-amber-400 underline" onClick={retry}>Tải lại</Button>
        )}
      </div>

      <div className="mt-8 hidden sm:block">
        <Pagination currentPage={page} totalPages={totalPages}
          onPageChange={(next) => { list.setPage(next); onPageChange?.(next); }} />
      </div>
    </div>
  );
}
