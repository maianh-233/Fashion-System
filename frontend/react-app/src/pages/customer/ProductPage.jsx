import ProductLayout from "../../components/customer/Product/ProductLayout";
import ProductHeader from "../../components/customer/Product/ProductHeader";
import ProductSidebar from "../../components/customer/Product/ProductSidebar";
import ProductGrid from "../../components/customer/Product/ProductGrid";
import { useState } from "react";

const PRODUCT_NAMES = [
  "Áo thun Essential",
  "Sơ mi Linen thanh lịch",
  "Hoodie Premium",
  "Quần Cargo ống rộng",
  "Đầm Midi tối giản",
  "Áo khoác Denim",
  "Chân váy xếp ly",
  "Polo Classic Fit",
];

const MOCK_PRODUCTS = Array.from({ length: 40 }, (_, index) => ({
  id: index + 1,
  name: `${PRODUCT_NAMES[index % PRODUCT_NAMES.length]} #${index + 1}`,
  tags: index % 3 === 0 ? ["NEW"] : index % 5 === 0 ? ["SALE"] : [],
  thumbnail: `https://picsum.photos/seed/lunaria-product-${index + 1}/600/750`,
  min_price: 299000 + (index % 6) * 125000,
}));

export default function ProductPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [searchType, setSearchType] = useState("product");
  const [priceRange, setPriceRange] = useState([100000, 2000000]);

  const [openFilter, setOpenFilter] = useState(false);

  const filteredProducts = MOCK_PRODUCTS.filter((product) =>
    product.name.toLocaleLowerCase("vi").includes(search.trim().toLocaleLowerCase("vi")),
  );

  const handleSearch = (value) => {
    setSearch(value);
    setPage(1);
  };

  return (
    <ProductLayout
      header={
        <ProductHeader
          search={search}
          setSearch={handleSearch}
          searchType={searchType}
          setSearchType={setSearchType}
          setOpenFilter={setOpenFilter}
          productCount={filteredProducts.length}
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
          key={`${searchType}-${search}`}
          products={filteredProducts}
          page={page}
          setPage={setPage}
        />
      }
      openFilter={openFilter}
      setOpenFilter={setOpenFilter}
    />
  );
}
