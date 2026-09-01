import Button from "../../components/common/Button";
import { useMemo, useState } from "react";
import { LoaderCircle, SlidersHorizontal, X } from "lucide-react";
import PromotionCard from "../../components/customer/Promotion/PromotionCard";
import Pagination from "../../components/common/Pagination";
import { useMobileInfiniteList } from "../../hooks/useMobileInfiniteList";
import CustomerPageIntro from "../../components/customer/CustomerPageIntro";

/* ================= MOCK DATA ================= */
const MOCK_PROMOTIONS = Array.from({ length: 17 }).map((_, i) => ({
  id: `promotion-${i + 1}`,
  code: `SALE${1000 + i}`,
  name: `Chương trình khuyến mãi ${i + 1}`,
  discount_type: ["percent", "cash", "freeship"][i % 3],
  discount_value: i % 3 === 0 ? 10 : i % 3 === 1 ? 50000 : 0,
  start_date: "2026-09-01",
  end_date: "2026-10-15",
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

  /* ================= FILTER + SEARCH ================= */
  const filteredPromotions = useMemo(() => {
    return MOCK_PROMOTIONS.filter((p) => {
      const matchSearch =
        p.name.toLowerCase().includes(search.toLowerCase()) ||
        p.code.toLowerCase().includes(search.toLowerCase());

      const matchType = promoType === "all" || p.discount_type === promoType;

      // Keep promotions whose validity period overlaps the selected period.
      // Comparing ISO dates also avoids timezone shifts around midnight.
      const matchDate =
        (!dateFrom || p.end_date >= dateFrom) &&
        (!dateTo || p.start_date <= dateTo);

      return matchSearch && matchType && matchDate;
    });
  }, [search, promoType, dateFrom, dateTo]);

  const {
    visibleCount,
    isLoadingMore,
    hasMore,
    loadMoreRef,
    reset: resetMobileList,
  } = useMobileInfiniteList(filteredPromotions.length, {
    initialCount: PAGE_SIZE,
    batchSize: PAGE_SIZE,
  });

  const resetFilter = () => {
    setDateFrom("");
    setDateTo("");
    setPromoType("all");
    setPage(1);
    resetMobileList();
  };

  const activeFilterCount =
    Number(Boolean(dateFrom || dateTo)) + Number(promoType !== "all");

  /* ================= PAGINATION ================= */
  const totalPages = Math.max(
    1,
    Math.ceil(filteredPromotions.length / PAGE_SIZE),
  );
  const safePage = Math.min(page, totalPages);

  const desktopPromotions = useMemo(() => {
    const start = (safePage - 1) * PAGE_SIZE;
    return filteredPromotions.slice(start, start + PAGE_SIZE);
  }, [filteredPromotions, safePage]);
  const mobilePromotions = filteredPromotions.slice(0, visibleCount);

  return (
    <div className="customer-page customer-catalog-page w-full min-h-screen text-zinc-200 flex flex-col">
      {/* ================= HEADER ================= */}
      <div className="customer-page__wide">
      <CustomerPageIntro
        eyebrow="Private offers"
        title="Ưu đãi dành riêng"
        description="Khám phá đặc quyền hiện hành và chọn ưu đãi phù hợp với đơn hàng của bạn."
        meta={`${filteredPromotions.length} ưu đãi`}
      >
        <div className="flex w-full gap-3">
          <div className="customer-search-control flex-1 relative">
            <input
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(1);
                resetMobileList();
              }}
              placeholder="Tìm kiếm khuyến mãi..."
              className="w-full min-h-12 bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3 pl-11 focus:outline-none focus:border-amber-400"
            />
            <i className="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" />
          </div>

          <Button
            onClick={() => setFilterOpen(true)}
            variant="unstyled"
            className="promotion-filter-trigger promotion-filter-trigger--promotion"
            aria-label="Mở bộ lọc khuyến mãi"
            aria-expanded={filterOpen}
          >
            <SlidersHorizontal size={18} />
            {activeFilterCount > 0 && <span>{activeFilterCount}</span>}
          </Button>
        </div>
      </CustomerPageIntro>
      </div>

      {/* OVERLAY MOBILE */}
      {filterOpen && (
        <div
          onClick={() => setFilterOpen(false)}
          className="customer-filter-overlay fixed bg-black/60 z-40 sm:hidden"
        />
      )}

      <div className="customer-page__wide flex flex-1 items-start min-h-0">
        {/* ================= SIDEBAR ================= */}
        <aside
          id="promotion-filter-panel"
          className={`
    customer-filter-panel promotion-filter-panel
    ${filterOpen ? "translate-x-0" : "-translate-x-full"}
    sm:translate-x-0
  `}
        >
          <div className="customer-filter">
            <div className="customer-filter__heading promotion-filter-heading">
              <div>
                <span>Tinh chỉnh</span>
                <h2>Bộ lọc</h2>
              </div>
              <Button
                type="button"
                variant="unstyled"
                className="promotion-filter-close sm:hidden"
                onClick={() => setFilterOpen(false)}
                aria-label="Đóng bộ lọc khuyến mãi"
              >
                <X size={18} />
              </Button>
            </div>

            {/* DATE */}
            <div className="customer-filter__section mb-6">
              <h3>Thời gian áp dụng</h3>

              <div className="promotion-filter-dates">
                <label>
                  <span>Từ ngày</span>
                  <input
                    type="date"
                    value={dateFrom}
                    max={dateTo || undefined}
                    onChange={(e) => {
                      setDateFrom(e.target.value);
                      setPage(1);
                      resetMobileList();
                    }}
                  />
                </label>
                <label>
                  <span>Đến ngày</span>
                  <input
                    type="date"
                    value={dateTo}
                    min={dateFrom || undefined}
                    onChange={(e) => {
                      setDateTo(e.target.value);
                      setPage(1);
                      resetMobileList();
                    }}
                  />
                </label>
              </div>
            </div>

            {/* TYPE */}
            <div className="customer-filter__section mb-6">
              <h3>Loại khuyến mãi</h3>

              <div className="space-y-3 text-sm">
                {[
                  { label: "Tất cả", value: "all" },
                  { label: "Giảm %", value: "percent" },
                  { label: "Giảm tiền", value: "cash" },
                  { label: "Miễn phí vận chuyển", value: "freeship" },
                ].map((item) => (
                  <label key={item.value} className="customer-filter__option flex items-center gap-2">
                    <input
                      type="radio"
                      name="promotion-type"
                      checked={promoType === item.value}
                      onChange={() => {
                        setPromoType(item.value);
                        setPage(1);
                        resetMobileList();
                      }}
                    />
                    {item.label}
                  </label>
                ))}
              </div>
            </div>

            <div className="promotion-filter-actions">
              <Button
                onClick={resetFilter}
                variant="unstyled"
                className="customer-filter__clear w-full text-sm"
                disabled={activeFilterCount === 0}
              >
                Xóa bộ lọc
              </Button>
              <Button
                onClick={() => setFilterOpen(false)}
                variant="unstyled"
                className="customer-filter__apply w-full text-sm sm:hidden"
              >
                Xem {filteredPromotions.length} ưu đãi
              </Button>
            </div>
          </div>
        </aside>

        <main className="customer-catalog-content flex-1 p-4 sm:p-7 min-h-0 flex flex-col space-y-8">
          {/* GRID */}
          {filteredPromotions.length === 0 ? (
            <div className="text-center text-zinc-500 mt-20">
              Không có khuyến mãi phù hợp
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 gap-6 sm:hidden">
                {mobilePromotions.map((item) => (
                  <PromotionCard key={item.id} promotion={item} />
                ))}
              </div>
              <div className="hidden gap-6 sm:grid sm:grid-cols-2 lg:grid-cols-3">
                {desktopPromotions.map((item) => (
                  <PromotionCard key={item.id} promotion={item} />
                ))}
              </div>
              <MobileLoadStatus
                loadMoreRef={loadMoreRef}
                isLoadingMore={isLoadingMore}
                hasMore={hasMore}
                total={filteredPromotions.length}
              />
            </>
          )}

          {/* PAGINATION */}
          <div className="mt-auto hidden pt-8 sm:block">
            <Pagination
              currentPage={safePage}
              totalPages={totalPages}
              onPageChange={setPage}
            />
          </div>
        </main>
      </div>
    </div>
  );
}

function MobileLoadStatus({ loadMoreRef, isLoadingMore, hasMore, total }) {
  return (
    <div
      ref={loadMoreRef}
      className="flex min-h-20 items-center justify-center sm:hidden"
      aria-live="polite"
    >
      {isLoadingMore && (
        <span className="flex items-center gap-2 text-sm text-zinc-400">
          <LoaderCircle className="animate-spin" size={18} /> Đang tải thêm...
        </span>
      )}
      {!hasMore && (
        <span className="text-sm text-zinc-500">
          Bạn đã xem hết {total} khuyến mãi
        </span>
      )}
    </div>
  );
}
