import { useEffect } from "react";

const TITLE_HEADER = /^(tên|họ tên|sản phẩm|nhân viên|khách hàng|ứng viên|phiếu|chiến dịch|vị trí|phòng ban|nhà cung cấp|cửa hàng|đơn hàng|mã đơn|chương trình|vai trò|danh mục|thương hiệu|bộ sưu tập)/i;

/** Cột dùng làm tiêu đề thẻ trong 3 cột đầu, -1 nếu không có cột tên. */
function titleColumn(labels) {
  return labels.slice(0, 3).findIndex((text) => TITLE_HEADER.test(text));
}

function cellAt(cells, column) {
  let col = 0;
  for (const cell of cells) {
    if (col === column) return cell;
    col += cell.colSpan || 1;
  }
  return null;
}

/**
 * Trên điện thoại, bảng dữ liệu trong khu quản trị hiển thị thành thẻ: mỗi dòng một thẻ, mỗi ô kèm nhãn cột lấy từ
 * <thead>. Hook này chỉ gắn nhãn (data-label) và lớp `admin-rtable`; bố cục thẻ nằm trong CSS (max-width: 767px)
 * nên máy tính không đổi. Bảng dạng ma trận giữ cuộn ngang bằng `data-table-mode="scroll"` trên <table>.
 */
export default function useResponsiveTables(rootRef) {
  useEffect(() => {
    const root = rootRef.current;
    if (!root) return undefined;
    let frame = 0;

    const label = (table) => {
      if (table.dataset.tableMode === "scroll") return;
      const headRow = table.tHead?.rows[table.tHead.rows.length - 1];
      if (!headRow) return;
      const labels = [];
      [...headRow.cells].forEach((cell) => {
        const text = cell.innerText.replace(/\s+/g, " ").trim();
        for (let i = 0; i < (cell.colSpan || 1); i += 1) labels.push(text);
      });
      if (labels.length < 2) return;
      table.classList.add("admin-rtable");
      const title = titleColumn(labels);
      [...table.tBodies].forEach((body) => [...body.rows].forEach((row) => {
        let col = 0;
        const cells = [...row.cells];
        // Tiêu đề thẻ: cột tên (nếu có), không thì ô đầu tiên có chữ (bỏ qua ô ảnh / ô chọn).
        const titleCell = (title >= 0 && cellAt(cells, title)) || cells.find((c, i) => i < 3 && c.innerText.trim().length > 1) || cells[0];
        cells.forEach((cell) => cell.classList.toggle("admin-rtable__title", cell === titleCell && cells.length > 1));
        cells.forEach((cell) => {
          const text = labels[col] || "";
          if (cell.dataset.label !== text) cell.dataset.label = text;
          // Ô gộp nhiều cột (dòng trống / dòng tổng) chiếm cả thẻ.
          cell.classList.toggle("admin-rtable__wide", (cell.colSpan || 1) > 1);
          col += cell.colSpan || 1;
        });
      }));
    };

    const scan = () => {
      frame = 0;
      root.querySelectorAll("table").forEach(label);
    };
    const schedule = () => { if (!frame) frame = requestAnimationFrame(scan); };

    scan();
    const observer = new MutationObserver(schedule);
    observer.observe(root, { childList: true, subtree: true });
    return () => {
      observer.disconnect();
      if (frame) cancelAnimationFrame(frame);
    };
  }, [rootRef]);
}
