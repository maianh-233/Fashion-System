// components/customer/Collection/CollectionProducts.jsx
import ProductLayout from "../Product/ProductLayout";
import ProductHeader from "../Product/ProductHeader";
import ProductSidebar from "../Product/ProductSidebar";
import ProductGrid from "../Product/ProductGrid";
import { useState } from "react";

const DEFAULT_PRICE_RANGE = [0, 5000000];

export default function CollectionProducts({ products }) {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [searchType, setSearchType] = useState("product");
  const [priceRange, setPriceRange] = useState(DEFAULT_PRICE_RANGE);
  const [openFilter, setOpenFilter] = useState(false);
  const priceFilterActive =
    priceRange[0] !== DEFAULT_PRICE_RANGE[0] ||
    priceRange[1] !== DEFAULT_PRICE_RANGE[1];
  const filteredProducts = products.filter((product) => {
    const price = product.min_price ?? product.price ?? 0;
    const name = product.name ?? "";
    const matchesSearch = name
      .toLocaleLowerCase("vi")
      .includes(search.trim().toLocaleLowerCase("vi"));
    return (
      matchesSearch && price >= priceRange[0] && price <= priceRange[1]
    );
  });

  return (
    <section className="customer-collection-products w-full py-10">
      <div className="customer-page__wide w-full px-4 sm:px-6">
        <h2 className="customer-page-title mb-6">
          Danh sách sản phẩm trong bộ sưu tập
        </h2>

        <ProductLayout
          header={
            <ProductHeader
              search={search}
              setSearch={setSearch}
              searchType={searchType}
              setSearchType={setSearchType}
              setOpenFilter={setOpenFilter}
              productCount={filteredProducts.length}
              activeFilterCount={Number(priceFilterActive)}
            />
          }
          sidebar={
            <ProductSidebar
              priceRange={priceRange}
              setPriceRange={(range) => {
                setPriceRange(range);
                setPage(1);
              }}
              activeFilterCount={Number(priceFilterActive)}
              resultCount={filteredProducts.length}
              onReset={() => {
                setPriceRange(DEFAULT_PRICE_RANGE);
                setPage(1);
              }}
              onClose={() => setOpenFilter(false)}
              showCategories={false}
            />
          }
          content={
            <ProductGrid
              products={filteredProducts}
              page={page}
              setPage={setPage}
            />
          }
          openFilter={openFilter}
          setOpenFilter={setOpenFilter}
        />
      </div>
    </section>
  );
}
