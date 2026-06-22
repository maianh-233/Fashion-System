import { ChevronLeft, ChevronRight } from "lucide-react";

export default function Pagination({
  currentPage,
  totalPages,
  onPageChange,
}) {
  if (totalPages <= 1) return null;

  return (
    <div className="hidden sm:flex w-full max-w-full px-4 sm:px-6 py-4 border-t border-zinc-700 bg-gradient-to-r from-zinc-900 via-zinc-900 to-zinc-800/90 items-center justify-between gap-4 flex-wrap">
      
      {/* LEFT */}
      <p className="text-sm text-zinc-300 whitespace-nowrap">
        Trang{" "}
        <span className="text-white font-medium">{currentPage}</span> /{" "}
        <span className="text-white font-medium">{totalPages}</span>
      </p>

      {/* RIGHT */}
      <div className="flex items-center gap-3 flex-wrap justify-end">
        
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          className="h-10 px-3 sm:px-4 rounded-xl bg-blue-500/90 border border-blue-400 text-white
          disabled:opacity-40 disabled:cursor-not-allowed hover:bg-blue-500 transition-colors
          flex items-center gap-1.5 whitespace-nowrap"
        >
          <ChevronLeft size={16} />
          Trước
        </button>

        <div className="h-10 min-w-10 px-3 rounded-xl bg-amber-400 text-zinc-900 font-semibold flex items-center justify-center">
          {currentPage}
        </div>

        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          className="h-10 px-3 sm:px-4 rounded-xl bg-emerald-500/90 border border-emerald-400 text-white
          disabled:opacity-40 disabled:cursor-not-allowed hover:bg-emerald-500 transition-colors
          flex items-center gap-1.5 whitespace-nowrap"
        >
          Sau
          <ChevronRight size={16} />
        </button>

      </div>
    </div>
  );
}