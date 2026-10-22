import { useEffect, useState } from "react";

/** Giá trị cập nhật sau khi người dùng ngừng gõ {@code delay} ms (ô tìm kiếm gọi API). */
export function useDebouncedValue(value, delay = 350) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay);
    return () => window.clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}
