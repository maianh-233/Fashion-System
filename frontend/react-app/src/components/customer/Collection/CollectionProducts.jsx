// components/customer/Collection/CollectionProducts.jsx
import ProductLayout from "../Product/ProductLayout";
import ProductHeader from "../Product/ProductHeader";
import ProductSidebar from "../Product/ProductSidebar";
import ProductGrid from "../Product/ProductGrid";
import { useState } from "react";

export default function CollectionProducts({ products }) {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [searchType, setSearchType] = useState("product");
  const [priceRange, setPriceRange] = useState([100000, 2000000]);
  const [openFilter, setOpenFilter] = useState(false);

  return (
    <section className="w-full bg-[#0b0f14] py-16">
      <div className="mx-auto w-full px-4">
        <h2 className="text-2xl md:text-3xl font-semibold mb-6 text-white">
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
            />
          }
          sidebar={
            <ProductSidebar
              priceRange={priceRange}
              setPriceRange={setPriceRange}
            />
          }
          content={
            <ProductGrid
              products={products}
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