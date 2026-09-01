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
  category: ["women", "men", "unisex"][index % 3],
}));

const DEFAULT_PRICE_RANGE = [0, 5000000];

export default function ProductPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [searchType, setSearchType] = useState("product");
  const [priceRange, setPriceRange] = useState(DEFAULT_PRICE_RANGE);
  const [selectedCategories, setSelectedCategories] = useState([]);

  const [openFilter, setOpenFilter] = useState(false);

  const filteredProducts = MOCK_PRODUCTS.filter((product) => {
    const matchesSearch = product.name
      .toLocaleLowerCase("vi")
      .includes(search.trim().toLocaleLowerCase("vi"));
    const matchesCategory =
      selectedCategories.length === 0 ||
      selectedCategories.includes(product.category);
    const matchesPrice =
      product.min_price >= priceRange[0] &&
      product.min_price <= priceRange[1];

    return matchesSearch && matchesCategory && matchesPrice;
  });

  const priceFilterActive =
    priceRange[0] !== DEFAULT_PRICE_RANGE[0] ||
    priceRange[1] !== DEFAULT_PRICE_RANGE[1];
  const activeFilterCount =
    selectedCategories.length + Number(priceFilterActive);

  const handleSearch = (value) => {
    setSearch(value);
    setPage(1);
  };

  const resetFilters = () => {
    setPriceRange(DEFAULT_PRICE_RANGE);
    setSelectedCategories([]);
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
          activeFilterCount={activeFilterCount}
        />
      }
      sidebar={
        <ProductSidebar
          priceRange={priceRange}
          setPriceRange={(range) => {
            setPriceRange(range);
            setPage(1);
          }}
          selectedCategories={selectedCategories}
          setSelectedCategories={(categories) => {
            setSelectedCategories(categories);
            setPage(1);
          }}
          activeFilterCount={activeFilterCount}
          resultCount={filteredProducts.length}
          onReset={resetFilters}
          onClose={() => setOpenFilter(false)}
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
