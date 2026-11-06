import { useCallback, useEffect, useState } from "react";

const STORAGE_KEY = "lunaria.admin.sidebarCollapsed";

function readStored() {
  try {
    const value = window.localStorage.getItem(STORAGE_KEY);
    if (value !== null) return value === "1";
  } catch {
    // Storage bị chặn: dùng mặc định.
  }
  // Mặc định: điện thoại thu gọn (menu ngang chiếm chỗ), máy tính mở.
  return typeof window !== "undefined" && window.matchMedia?.("(max-width: 767px)").matches;
}

/** Trạng thái thu gọn sidebar quản trị: nhớ theo trình duyệt, phím tắt Ctrl/⌘ + B. */
export default function useSidebarState() {
  const [collapsed, setCollapsed] = useState(readStored);

  const toggle = useCallback(() => setCollapsed((value) => !value), []);

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, collapsed ? "1" : "0");
    } catch {
      // Không lưu được thì chỉ giữ trong phiên hiện tại.
    }
  }, [collapsed]);

  useEffect(() => {
    const onKey = (event) => {
      if (!(event.ctrlKey || event.metaKey) || event.shiftKey || event.altKey || event.key.toLowerCase() !== "b") return;
      if (event.target instanceof HTMLElement && event.target.isContentEditable) return;
      event.preventDefault();
      toggle();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [toggle]);

  return { collapsed, toggle };
}
