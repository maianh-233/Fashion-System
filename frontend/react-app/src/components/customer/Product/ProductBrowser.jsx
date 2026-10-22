import { useEffect, useMemo, useState } from "react";
import ProductLayout from "./ProductLayout";
import ProductHeader from "./ProductHeader";
import ProductSidebar from "./ProductSidebar";
import RemoteProductGrid from "./RemoteProductGrid";
import { storefrontApi } from "../../../api/storefrontApi";
import { useDebouncedValue } from "../../../hooks/useDebouncedValue";
import { useServerPages } from "../../../hooks/useServerPages";
import { PAGE_SIZE, SORT_OPTIONS, buildProductQuery, priceBoundsOf } from "../../../pages/customer/storefrontLogic";

/**
 * Tìm kiếm + lọc + phân trang sản phẩm từ API storefront. {@code scope} giới hạn trong một thương hiệu, bộ sưu tập
 * hoặc khuyến mãi ({brandId | collectionId | promotionId}); {@code intro} đổi tiêu đề phần đầu.
 */
export default function ProductBrowser({ scope = {}, intro, notice }) {
  const [filters, setFilters] = useState(null);
  const [searchInput, setSearchInput] = useState("");
  const search = useDebouncedValue(searchInput);
  const [searchType, setSearchType] = useState("product");
  const [genders, setGenders] = useState([]);
  const [categoryIds, setCategoryIds] = useState([]);
  const [priceRange, setPriceRange] = useState(null);
  const [inStockOnly, setInStockOnly] = useState(false);
  const [sort, setSort] = useState("NEWEST");
  const [openFilter, setOpenFilter] = useState(false);

  const priceBounds = useMemo(() => priceBoundsOf(filters), [filters]);
  const range = priceRange || priceBounds;

  useEffect(() => {
    const controller = new AbortController();
    storefrontApi.filters({ signal: controller.signal }).then(setFilters).catch(() => {});
    return () => controller.abort();
  }, []);

  const query = (page) => {
    const q = buildProductQuery({ search, searchType, genders, categoryIds, priceRange: range, priceBounds, inStockOnly,
      sort, page, size: PAGE_SIZE });
    Object.entries(scope).forEach(([key, value]) => { if (value) q.set(key, value); });
    return q;
  };
  const filterKey = query(1).toString();
  const list = useServerPages(filterKey, (page, options) => storefrontApi.products(query(page), options));

  const priceFilterActive = priceRange != null && (priceRange[0] !== priceBounds[0] || priceRange[1] !== priceBounds[1]);
  const activeFilterCount = genders.length + categoryIds.length + Number(priceFilterActive) + Number(inStockOnly);
  const resetFilters = () => {
    setGenders([]);
    setCategoryIds([]);
    setPriceRange(null);
    setInStockOnly(false);
  };

  return (
    <ProductLayout
      header={
        <>
          <ProductHeader
            {...intro}
            search={searchInput}
            setSearch={setSearchInput}
            searchType={searchType}
            setSearchType={setSearchType}
            setOpenFilter={setOpenFilter}
            productCount={list.totalElements}
            activeFilterCount={activeFilterCount}
          />
          {notice}
        </>
      }
      sidebar={
        <ProductSidebar
          sortOptions={SORT_OPTIONS}
          sort={sort}
          setSort={setSort}
          genderOptions={(filters?.genders || []).map((g) => ({ key: g.value, label: g.label, count: g.productCount }))}
          selectedCategories={genders}
          setSelectedCategories={setGenders}
          categoryTree={filters?.categories || []}
          selectedCategoryIds={categoryIds}
          setSelectedCategoryIds={setCategoryIds}
          priceRange={range}
          priceBounds={priceBounds}
          setPriceRange={setPriceRange}
          inStockOnly={inStockOnly}
          setInStockOnly={setInStockOnly}
          activeFilterCount={activeFilterCount}
          resultCount={list.totalElements}
          onReset={resetFilters}
          onClose={() => setOpenFilter(false)}
        />
      }
      content={
        <RemoteProductGrid
          products={list.items}
          mobileProducts={list.mobileItems}
          page={list.page}
          totalPages={list.totalPages}
          totalElements={list.loaded ? list.totalElements : null}
          loading={list.loading}
          error={list.error}
          onPageChange={(next) => {
            list.setPage(next);
            window.scrollTo({ top: 0, behavior: "smooth" });
          }}
          onLoadMore={list.loadMore}
          onRetry={list.retry}
        />
      }
      openFilter={openFilter}
      setOpenFilter={setOpenFilter}
    />
  );
}
