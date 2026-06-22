import { useMemo, useState } from "react";
import PromotionCard from "../../components/customer/Promotion/PromotionCard";
import Pagination from "../../components/common/Pagination";

/* ================= MOCK DATA ================= */
const MOCK_PROMOTIONS = Array.from({ length: 17 }).map((_, i) => ({
  id: crypto.randomUUID(),
  code: `SALE${1000 + i}`,
  name: `Chương trình khuyến mãi ${i + 1}`,
  discount_type: ["percent", "cash", "freeship"][i % 3],
  discount_value: i % 3 === 0 ? 10 : i % 3 === 1 ? 50000 : 0,
  start_date: "2025-06-01",
  end_date: "2025-06-30",
  min_order_value: 200000,
  active: i % 4 !== 0,
}));

export default function PromotionPage() {
  const [search, setSearch] = useState("");
  const [filterOpen, setFilterOpen] = useState(false);

  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [promoType, setPromoType] = useState("all");

  const [page, setPage] = useState(1);
  const PAGE_SIZE = 6;

  const resetFilter = () => {
    setDateFrom("");
    setDateTo("");
    setPromoType("all");
  };

  /* ================= FILTER + SEARCH ================= */
  const filteredPromotions = useMemo(() => {
    return MOCK_PROMOTIONS.filter((p) => {
      const matchSearch =
        p.name.toLowerCase().includes(search.toLowerCase()) ||
        p.code.toLowerCase().includes(search.toLowerCase());

      const matchType = promoType === "all" || p.discount_type === promoType;

      const start = dateFrom ? new Date(dateFrom) : null;
      const end = dateTo ? new Date(dateTo) : null;
      const promoStart = new Date(p.start_date);

      const matchDate =
        (!start || promoStart >= start) && (!end || promoStart <= end);

      return matchSearch && matchType && matchDate;
    });
  }, [search, promoType, dateFrom, dateTo]);

  /* ================= PAGINATION ================= */
  const totalPages = Math.ceil(filteredPromotions.length / PAGE_SIZE);

  const pagedPromotions = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredPromotions.slice(start, start + PAGE_SIZE);
  }, [filteredPromotions, page]);

  return (
    <div className="w-full h-full bg-zinc-950 text-zinc-200 flex flex-col">
      {/* ================= HEADER ================= */}
      <header className="border-b border-zinc-800 bg-zinc-900 sticky top-0 z-40">
        <div className="px-4 sm:px-8 py-4 flex gap-3">
          <div className="flex-1 relative">
            <input
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(1);
              }}
              placeholder="Tìm kiếm khuyến mãi..."
              className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3 pl-11 focus:outline-none focus:border-amber-400"
            />
            <i className="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" />
          </div>

          <button
            onClick={() => setFilterOpen(true)}
            className="sm:hidden px-4 py-3 rounded-xl bg-zinc-800 border border-zinc-700"
          >
            <i className="fas fa-filter" />
          </button>
        </div>
      </header>

      {/* OVERLAY MOBILE */}
      {filterOpen && (
        <div
          onClick={() => setFilterOpen(false)}
          className="fixed inset-0 bg-black/60 z-40 sm:hidden"
        />
      )}

      <div className="flex flex-1 overflow-hidden min-h-0">
        {/* ================= SIDEBAR ================= */}
        <aside
          className={`
    w-72 bg-zinc-900 border-r border-zinc-800
    p-6 flex flex-col overflow-y-auto
    fixed sm:sticky top-0
    h-screen sm:h-auto
    z-50 transition-transform duration-300
    ${filterOpen ? "translate-x-0" : "-translate-x-full"}
    sm:translate-x-0
  `}
        >
          <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
            <i className="fas fa-filter" />
            Bộ lọc
          </h2>

          {/* DATE */}
          <div className="mb-8">
            <h3 className="text-amber-400 font-medium mb-4">
              KHOẢNG THỜI GIAN
            </h3>

            <div className="space-y-4 text-sm">
              <input
                type="date"
                value={dateFrom}
                onChange={(e) => setDateFrom(e.target.value)}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-3 py-2"
              />
              <input
                type="date"
                value={dateTo}
                onChange={(e) => setDateTo(e.target.value)}
                className="w-full bg-zinc-800 border border-zinc-700 rounded-xl px-3 py-2"
              />
            </div>
          </div>

          {/* TYPE */}
          <div className="mb-8">
            <h3 className="text-amber-400 font-medium mb-4">LOẠI KHUYẾN MÃI</h3>

            <div className="space-y-3 text-sm">
              {[
                { label: "Tất cả", value: "all" },
                { label: "Giảm %", value: "percent" },
                { label: "Giảm tiền", value: "cash" },
                { label: "Freeship", value: "freeship" },
              ].map((item) => (
                <label key={item.value} className="flex items-center gap-2">
                  <input
                    type="radio"
                    checked={promoType === item.value}
                    onChange={() => {
                      setPromoType(item.value);
                      setPage(1);
                    }}
                    className="accent-amber-400"
                  />
                  {item.label}
                </label>
              ))}
            </div>
          </div>

          <div className="mt-auto pt-6 border-t border-zinc-800">
            <button
              onClick={resetFilter}
              className="w-full text-sm text-zinc-400"
            >
              Xóa bộ lọc
            </button>
          </div>
        </aside>

        <main className="flex-1 overflow-y-auto p-4 sm:p-8 min-h-0 flex flex-col space-y-10">
          {/* GRID */}
          {pagedPromotions.length === 0 ? (
            <div className="text-center text-zinc-500 mt-20">
              Không có khuyến mãi phù hợp
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {pagedPromotions.map((item) => (
                <PromotionCard key={item.id} promotion={item} />
              ))}
            </div>
          )}

          {/* PAGINATION */}
          <div className="mt-auto pt-8">
            <Pagination
              currentPage={page}
              totalPages={totalPages}
              onPageChange={setPage}
            />
          </div>
        </main>
      </div>
    </div>
  );
}
