import { useState } from "react";
import ProductCard from "../../components/customer/Product/ProductCard";
import SearchTypeDropdown from "../../components/customer/Product/SearchTypeDropdown";
import Pagination from "../../components/common/Pagination";


export default function ProductPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState("");
  const [searchType, setSearchType] = useState("product");
  const [priceRange, setPriceRange] = useState([100000, 2000000]);

  // Dữ liệu giả
  const mockProducts = Array.from({ length: 10 }).map((_, i) => ({
    id: i + 1,
    name: `Sản phẩm #${i + 1}`,
    tags: i % 2 === 0 ? ["NEW"] : [],
    thumbnail: "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab",
    min_price: 299000,
  }));

  //   Format tiền
  const formatPrice = (value) => value.toLocaleString("vi-VN") + "đ";

  return (
    <div className="flex flex-col gap-6 px-4 sm:px-8 py-6">
      {/* ================= HEADER ================= */}
      <header className="border-b border-zinc-800 bg-zinc-900">
        <div className="px-8 py-5">
          <div className="flex flex-col xl:flex-row xl:items-center gap-4">
            {/* Search */}
            <div className="flex-1 relative">
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm kiếm tên sản phẩm, mã SKU..."
                className="w-full bg-zinc-800 border border-zinc-700 rounded-2xl px-5 py-3 pl-12 focus:outline-none focus:border-amber-400"
              />
              <i className="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" />
            </div>

            {/* Search type + sort */}
            <div className="flex gap-3">
                <SearchTypeDropdown
                    value={searchType}
                    onChange={setSearchType}
                />
            </div>
          </div>
        </div>
      </header>

      {/* ================= BODY ================= */}
      <div className="flex flex-1 overflow-hidden">
        {/* SIDEBAR */}
        <aside className="w-72 bg-zinc-900 border-r border-zinc-800 p-6 overflow-y-auto flex flex-col">
          <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
            <i className="fas fa-filter" />
            Bộ lọc
          </h2>

          {/* ===== DANH MỤC ===== */}
          <div className="mb-10">
            <h3 className="text-amber-400 font-medium mb-4">DANH MỤC</h3>

            <div className="space-y-3 text-sm pl-4 border-l border-zinc-700">
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" className="accent-amber-400" />
                Quần áo
              </label>

              <div className="pl-6 space-y-3">
                {["Nam", "Nữ", "Unisex"].map((item) => (
                  <label
                    key={item}
                    className="flex items-center gap-2 cursor-pointer"
                  >
                    <input type="checkbox" className="accent-amber-400" />
                    {item}
                  </label>
                ))}
              </div>
            </div>
          </div>

          {/* ===== KHOẢNG GIÁ (SLIDER) ===== */}
          <div className="mb-10">
            <h3 className="text-amber-400 font-medium mb-4">KHOẢNG GIÁ</h3>

            <div className="space-y-4">
              {/* Hiển thị giá */}
              <div className="flex justify-between text-sm text-zinc-300">
                <span>{formatPrice(priceRange[0])}</span>
                <span>{formatPrice(priceRange[1])}</span>
              </div>

              {/* Slider min */}
              <input
                type="range"
                min={0}
                max={5000000}
                step={50000}
                value={priceRange[0]}
                onChange={(e) =>
                  setPriceRange([Number(e.target.value), priceRange[1]])
                }
                className="w-full accent-amber-400"
              />

              {/* Slider max */}
              <input
                type="range"
                min={0}
                max={5000000}
                step={50000}
                value={priceRange[1]}
                onChange={(e) =>
                  setPriceRange([priceRange[0], Number(e.target.value)])
                }
                className="w-full accent-amber-400"
              />
            </div>
          </div>

          {/* ===== ACTION BUTTON ===== */}
          <div className="mt-auto pt-6 border-t border-zinc-800">
            <button
              className="w-full bg-amber-400 text-zinc-900 font-medium py-3 rounded-xl hover:bg-amber-300 transition"
              onClick={() => {
                console.log("Apply filter:", { priceRange });
              }}
            >
              Áp dụng bộ lọc
            </button>

            <button className="w-full mt-3 text-sm text-zinc-400 hover:text-zinc-200">
              Xóa bộ lọc
            </button>
          </div>
        </aside>

        {/* MAIN CONTENT */}
        <main className="flex-1 p-8 overflow-y-auto flex flex-col">
          {/* GRID */}
          <div className="grid grid-cols-[repeat(auto-fill,minmax(280px,1fr))] gap-6">
            {mockProducts.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>

          {/* PAGINATION */}
          <div className="mt-12">
            {" "}
            {/* 👈 TẠO KHOẢNG CÁCH */}
            <Pagination
              currentPage={page}
              totalPages={5}
              onPageChange={setPage}
            />
          </div>
        </main>
      </div>
    </div>
  );
}
