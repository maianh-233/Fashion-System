import Button from "./Button";
import { ChevronLeft, ChevronRight } from "lucide-react";

export default function Pagination({
  currentPage,
  totalPages,
  onPageChange,
}) {
  if (totalPages <= 1) return null;

  const goToPage = (page) => {
    onPageChange(Math.min(Math.max(page, 1), totalPages));
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
        <Button
          type="button"
          onClick={() => goToPage(currentPage - 1)}
          disabled={currentPage === 1}
          className="pagination__button"
          aria-label="Trang trước"
        >
          <ChevronLeft size={16} />
          <span className="hidden sm:inline">Trước</span>
        </Button>

        <div className="pagination__current" aria-current="page">
          {currentPage}
        </div>

        <Button
          type="button"
          onClick={() => goToPage(currentPage + 1)}
          disabled={currentPage === totalPages}
          className="pagination__button"
          aria-label="Trang sau"
        >
          <span className="hidden sm:inline">Sau</span>
          <ChevronRight size={16} />
        </Button>
      </div>
    </nav>
  );
}
