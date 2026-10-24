import Button from "../../components/common/Button";
import { useState } from "react";
import { SlidersHorizontal, TicketPercent, X } from "lucide-react";
import PromotionCard from "../../components/customer/Promotion/PromotionCard";
import CustomerPageIntro from "../../components/customer/CustomerPageIntro";
import ServerPagedList from "../../components/customer/ServerPagedList";
import { storefrontApi } from "../../api/storefront/storefrontApi";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { useServerPages } from "../../hooks/useServerPages";

const PAGE_SIZE = 6;
const TYPE_OPTIONS = [
  { label: "Tất cả", value: "" },
  { label: "Giảm trên đơn hàng", value: "ORDER" },
  { label: "Giảm theo sản phẩm", value: "PRODUCT" },
  { label: "Đổi điểm lấy voucher", value: "POINT_REDEMPTION" },
];
const DISCOUNT_OPTIONS = [
  { label: "Tất cả", value: "" },
  { label: "Giảm %", value: "PERCENT" },
  { label: "Giảm tiền", value: "FIXED" },
];
const SORT_OPTIONS = [
  { label: "Sắp hết hạn", value: "ENDING_SOON" },
  { label: "Mới nhất", value: "NEWEST" },
];

/** Khuyến mãi đang áp dụng và sắp diễn ra (API storefront); lọc, sắp xếp, phân trang ở backend. */
export default function PromotionPage() {
  const [search, setSearch] = useState("");
  const keyword = useDebouncedValue(search.trim());
  const [filterOpen, setFilterOpen] = useState(false);
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [promoType, setPromoType] = useState("");
  const [discountType, setDiscountType] = useState("");
  const [sort, setSort] = useState("ENDING_SOON");

  const params = { keyword, type: promoType, discountType, from: dateFrom, to: dateTo, sort };
  const filterKey = JSON.stringify(params);
  const list = useServerPages(filterKey, (page, options) => {
    const query = new URLSearchParams({ page: String(page - 1), size: String(PAGE_SIZE) });
    Object.entries(params).forEach(([key, value]) => { if (value) query.set(key, value); });
    return storefrontApi.promotions(query, options);
  });

  const resetFilter = () => {
    setDateFrom("");
    setDateTo("");
    setPromoType("");
    setDiscountType("");
  };
  const activeFilterCount = Number(Boolean(dateFrom || dateTo)) + Number(Boolean(promoType)) + Number(Boolean(discountType));
  const total = list.loaded ? list.totalElements : 0;

  return (
    <div className="customer-page customer-catalog-page w-full min-h-screen text-zinc-200 flex flex-col">
      {/* ================= HEADER ================= */}
      <div className="customer-page__wide">
      <CustomerPageIntro
        eyebrow="Private offers"
        title="Ưu đãi dành riêng"
        description="Khám phá đặc quyền hiện hành và chọn ưu đãi phù hợp với đơn hàng của bạn."
        meta={list.loaded ? `${list.totalElements} ưu đãi` : "Đang tải..."}
      >
        <div className="flex w-full gap-3">
          <div className="customer-search-control flex-1 relative">
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm kiếm khuyến mãi..."
              aria-label="Tìm kiếm khuyến mãi"
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
                    onChange={(e) => setDateFrom(e.target.value)}
                  />
                </label>
                <label>
                  <span>Đến ngày</span>
                  <input
                    type="date"
                    value={dateTo}
                    min={dateFrom || undefined}
                    onChange={(e) => setDateTo(e.target.value)}
                  />
                </label>
              </div>
            </div>

            {/* SORT */}
            <div className="customer-filter__section mb-6">
              <h3>Sắp xếp</h3>
              <select className="customer-filter__select" value={sort} onChange={(e) => setSort(e.target.value)} aria-label="Sắp xếp khuyến mãi">
                {SORT_OPTIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
              </select>
            </div>

            {/* TYPE */}
            <RadioGroup title="Áp dụng cho" name="promotion-type" options={TYPE_OPTIONS} value={promoType} onChange={setPromoType} />
            <RadioGroup title="Kiểu giảm giá" name="promotion-discount" options={DISCOUNT_OPTIONS} value={discountType} onChange={setDiscountType} />

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
                Xem {total} ưu đãi
              </Button>
            </div>
          </div>
        </aside>

        <main className="customer-catalog-content flex-1 p-4 sm:p-7 min-h-0 flex flex-col space-y-8">
          <ServerPagedList
            list={list}
            itemLabel="khuyến mãi"
            emptyIcon={TicketPercent}
            emptyText="Không có khuyến mãi phù hợp."
            desktopClassName="hidden gap-6 sm:grid sm:grid-cols-2 lg:grid-cols-3"
            mobileClassName="grid grid-cols-1 gap-6 sm:hidden"
            skeleton={<div className="customer-card customer-card--skeleton" />}
            renderItem={(item) => <PromotionCard key={item.id} promotion={item} />}
            onPageChange={() => window.scrollTo({ top: 0, behavior: "smooth" })}
          />
        </main>
      </div>
    </div>
  );
}

function RadioGroup({ title, name, options, value, onChange }) {
  return (
    <div className="customer-filter__section mb-6">
      <h3>{title}</h3>
      <div className="space-y-3 text-sm">
        {options.map((item) => (
          <label key={item.value || "all"} className="customer-filter__option flex items-center gap-2">
            <input type="radio" name={name} checked={value === item.value} onChange={() => onChange(item.value)} />
            {item.label}
          </label>
        ))}
      </div>
    </div>
  );
}
