import { useCallback, useEffect, useMemo, useState } from "react";

const DEFAULT_INTERVAL_MS = 6000;

export function useHeroSlider(totalSlides, intervalMs = DEFAULT_INTERVAL_MS) {
  const safeTotal = useMemo(() => Math.max(totalSlides, 1), [totalSlides]);
  const [currentIndex, setCurrentIndex] = useState(0);

  const goTo = useCallback(
    (index) => {
      setCurrentIndex(((index % safeTotal) + safeTotal) % safeTotal);
    },
    [safeTotal]
  );

  const next = useCallback(() => {
    setCurrentIndex((prev) => (prev + 1) % safeTotal);
  }, [safeTotal]);

  const prev = useCallback(() => {
    setCurrentIndex((prev) => (prev - 1 + safeTotal) % safeTotal);
  }, [safeTotal]);

  useEffect(() => {
    const timer = window.setInterval(next, intervalMs);
    return () => window.clearInterval(timer);
  }, [next, intervalMs]);

  return {
    currentIndex,
    goTo,
    next,
    prev,
  };
}
