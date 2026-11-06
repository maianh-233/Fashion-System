import { useState } from "react";
import Button from "./Button";
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from "lucide-react";

/** Các số trang hiển thị quanh trang hiện tại, chèn "…" khi bỏ qua. */
function pageItems(current, total, radius) {
  const pages = new Set([1, total]);
  for (let p = current - radius; p <= current + radius; p += 1) if (p >= 1 && p <= total) pages.add(p);
  const sorted = [...pages].sort((a, b) => a - b);
  const items = [];
  sorted.forEach((p, i) => {
    if (i && p - sorted[i - 1] > 1) items.push(`gap-${p}`);
    items.push(p);
  });
  return items;
}

/**
 * Phân trang bằng nút (không cuộn vô tận): về đầu / trước / các số trang / sau / tới cuối, và ô nhảy tới trang khi
 * có nhiều trang. Trên điện thoại chỉ hiện trang lân cận cho gọn. {@code currentPage} bắt đầu từ 1.
 */
export default function Pagination({
  currentPage,
  totalPages,
  onPageChange,
}) {
  const [jump, setJump] = useState("");
  if (totalPages <= 1) return null;

  const goToPage = (page) => {
    const next = Math.min(Math.max(page, 1), totalPages);
    if (next !== currentPage) onPageChange(next);
  };
  const submitJump = (event) => {
    event.preventDefault();
    const value = Number(jump);
    if (Number.isFinite(value) && value >= 1) goToPage(Math.round(value));
    setJump("");
  };

  return (
    <nav className="pagination" aria-label="Phân trang">
      <p className="pagination__summary" aria-live="polite">
        Trang <strong>{currentPage}</strong>
        <span aria-hidden="true"> / </span>
        <span className="sr-only">trên </span>
        <strong>{totalPages}</strong>
      </p>

      <div className="pagination__controls">
        <Button type="button" onClick={() => goToPage(1)} disabled={currentPage === 1}
          className="pagination__button pagination__edge" aria-label="Trang đầu" title="Trang đầu">
          <ChevronsLeft size={16} />
        </Button>
        <Button type="button" onClick={() => goToPage(currentPage - 1)} disabled={currentPage === 1}
          className="pagination__button" aria-label="Trang trước">
          <ChevronLeft size={16} />
          <span className="hidden sm:inline">Trước</span>
        </Button>

        <span className="pagination__pages pagination__pages--wide">
          {pageItems(currentPage, totalPages, 2).map((item) => (typeof item === "number" ? (
            item === currentPage
              ? <span key={item} className="pagination__current" aria-current="page">{item}</span>
              : <Button key={item} type="button" className="pagination__button pagination__number" onClick={() => goToPage(item)} aria-label={`Trang ${item}`}>{item}</Button>
          ) : <span key={item} className="pagination__gap" aria-hidden="true">…</span>))}
        </span>
        <span className="pagination__pages pagination__pages--narrow">
          {pageItems(currentPage, totalPages, 0).map((item) => (typeof item === "number" ? (
            item === currentPage
              ? <span key={item} className="pagination__current" aria-current="page">{item}</span>
              : <Button key={item} type="button" className="pagination__button pagination__number" onClick={() => goToPage(item)} aria-label={`Trang ${item}`}>{item}</Button>
          ) : <span key={item} className="pagination__gap" aria-hidden="true">…</span>))}
        </span>

        <Button type="button" onClick={() => goToPage(currentPage + 1)} disabled={currentPage === totalPages}
          className="pagination__button" aria-label="Trang sau">
          <span className="hidden sm:inline">Sau</span>
          <ChevronRight size={16} />
        </Button>
        <Button type="button" onClick={() => goToPage(totalPages)} disabled={currentPage === totalPages}
          className="pagination__button pagination__edge" aria-label="Trang cuối" title="Trang cuối">
          <ChevronsRight size={16} />
        </Button>
      </div>

      {totalPages > 7 && (
        <form className="pagination__jump" onSubmit={submitJump}>
          <label>
            <span className="sr-only">Đến trang</span>
            <input type="number" inputMode="numeric" min="1" max={totalPages} value={jump} placeholder="Trang…"
              onChange={(e) => setJump(e.target.value)} aria-label={`Đến trang (1–${totalPages})`} />
          </label>
          <Button type="submit" className="pagination__button" disabled={!jump}>Đi</Button>
        </form>
      )}
    </nav>
  );
}
