import { useCallback, useEffect, useRef, useState } from "react";

export function useMobileInfiniteList(
  totalItems,
  { initialCount = 8, batchSize = 6 } = {},
) {
  const [visibleCount, setVisibleCount] = useState(initialCount);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const loadMoreRef = useRef(null);
  const loadingRef = useRef(false);
  const hasMore = visibleCount < totalItems;

  const reset = useCallback(() => {
    setVisibleCount(initialCount);
    setIsLoadingMore(false);
    loadingRef.current = false;
  }, [initialCount]);

  useEffect(() => {
    const target = loadMoreRef.current;
    const isMobile = window.matchMedia("(max-width: 639px)").matches;
    if (!target || !isMobile || !hasMore) return undefined;

    let loadingTimer;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting || loadingRef.current) return;

        loadingRef.current = true;
        setIsLoadingMore(true);
        loadingTimer = window.setTimeout(() => {
          setVisibleCount((current) =>
            Math.min(current + batchSize, totalItems),
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
  }, [batchSize, hasMore, totalItems, visibleCount]);

  return { visibleCount, isLoadingMore, hasMore, loadMoreRef, reset };
}
