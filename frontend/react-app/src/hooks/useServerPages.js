import { useCallback, useEffect, useState } from "react";

/**
 * Danh sách phân trang phía server. Các trang đã tải được nhớ theo {@code filterKey} (đổi bộ lọc thì bỏ):
 * máy tính hiện trang {@code page}, điện thoại nối các trang liên tiếp từ trang 1.
 *
 * @param fetchPage (page 1-based, {signal}) => Promise<Spring Page>
 */
export function useServerPages(filterKey, fetchPage) {
  const [page, setPage] = useState(1);
  const [result, setResult] = useState({ key: null, pages: {}, totalElements: 0, totalPages: 1 });
  const [failure, setFailure] = useState({ key: "", message: "" });
  const [retry, setRetry] = useState(0);
  const [pageKey, setPageKey] = useState(filterKey);

  // Đổi bộ lọc thì về trang 1 (điều chỉnh state trong lúc render thay vì trong effect).
  if (pageKey !== filterKey) {
    setPageKey(filterKey);
    setPage(1);
  }
  const currentPage = pageKey === filterKey ? page : 1;

  const requestKey = `${filterKey}#${currentPage}#${retry}`;
  const cached = result.key === filterKey && Boolean(result.pages[currentPage]);
  const error = failure.key === requestKey ? failure.message : "";
  const loading = !cached && !error;

  useEffect(() => {
    if (cached) return undefined;
    const controller = new AbortController();
    fetchPage(currentPage, { signal: controller.signal })
      .then((data) => setResult((current) => ({
        key: filterKey,
        pages: { ...(current.key === filterKey ? current.pages : {}), [currentPage]: data.content || [] },
        totalElements: data.page?.totalElements ?? 0,
        totalPages: Math.max(1, data.page?.totalPages ?? 1),
      })))
      .catch((reason) => {
        if (!controller.signal.aborted) setFailure({ key: requestKey, message: reason.message });
      });
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestKey]);

  const current = result.key === filterKey ? result : { pages: {}, totalElements: 0, totalPages: 1 };
  const mobileItems = [];
  for (let p = 1; current.pages[p]; p += 1) mobileItems.push(...current.pages[p]);

  return {
    page: currentPage,
    setPage,
    items: current.pages[currentPage] || [],
    mobileItems,
    totalElements: current.totalElements,
    totalPages: current.totalPages,
    loaded: result.key === filterKey,
    loading,
    error,
    retry: useCallback(() => setRetry((value) => value + 1), []),
    loadMore: useCallback(() => setPage((value) => value + 1), []),
  };
}
