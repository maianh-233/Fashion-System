import ProductLayout from "../../components/customer/Product/ProductLayout";
import ProductHeader from "../../components/customer/Product/ProductHeader";
import ProductSidebar from "../../components/customer/Product/ProductSidebar";
import ProductGrid from "../../components/customer/Product/ProductGrid";
import { useState } from "react";

export default function ProductPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [searchType, setSearchType] = useState("product");
  const [priceRange, setPriceRange] = useState([100000, 2000000]);

  // ✅ PHẢI nằm trong component
  const [openFilter, setOpenFilter] = useState(false);

  const mockProducts = Array.from({ length: 10 }).map((_, i) => ({
    id: i + 1,
    name: `Sản phẩm #${i + 1}`,
    tags: i % 2 === 0 ? ["NEW"] : [],
    thumbnail:
      "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab",
    min_price: 299000,
  }));

  return (
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
          products={mockProducts}
          page={page}
          setPage={setPage}
        />
      }
      openFilter={openFilter}
      setOpenFilter={setOpenFilter}
    />
  );
}