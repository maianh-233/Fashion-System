import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  PenSquare,
  RotateCcw,
  Trash2,
  Undo2,
  Archive,
} from "lucide-react";

import Pagination from "../../../components/common/Pagination";

const PAGE_SIZE = 5;

export default function ProductManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState(1);

  const products = [
    {
      id: 1,
      name: "Oversize Hoodie Black",
      slug: "oversize-hoodie-black",
      material: "Cotton 100%",
      fit: "Oversize",
      gender: "UNISEX",
      status: "ACTIVE",
      variantCount: 8,
      tagCount: 5,
      category: "Hoodie",
      brand: "Nike",
      collection: "Winter 2026",
      image:
        "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?q=80&w=400",
    },

    {
      id: 2,
      name: "Slim Fit Polo",
      slug: "slim-fit-polo",
      material: "Cotton Poly",
      fit: "Slim Fit",
      gender: "MALE",
      status: "DRAFT",
      variantCount: 4,
      tagCount: 2,
      category: "Polo",
      brand: "Adidas",
      collection: "Summer 2026",
      image:
        "https://images.unsplash.com/photo-1576566588028-4147f3842f27?q=80&w=400",
    },

    {
      id: 3,
      name: "Basic Tee White",
      slug: "basic-tee-white",
      material: "Cotton",
      fit: "Regular",
      gender: "FEMALE",
      status: "ARCHIVE",
      variantCount: 3,
      tagCount: 1,
      category: "T-Shirt",
      brand: "Puma",
      collection: "Classic",
      image:
        "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?q=80&w=400",
    },

    {
      id: 4,
      name: "Cargo Pants Grey",
      slug: "cargo-pants-grey",
      material: "Kaki",
      fit: "Relaxed",
      gender: "UNISEX",
      status: "ACTIVE",
      variantCount: 6,
      tagCount: 4,
      category: "Pants",
      brand: "Gucci",
      collection: "Urban Street",
      image:
        "https://images.unsplash.com/photo-1506629905607-d9c297d75d72?q=80&w=400",
    },

    {
      id: 5,
      name: "Varsity Jacket",
      slug: "varsity-jacket",
      material: "Wool Blend",
      fit: "Regular",
      gender: "MALE",
      status: "ACTIVE",
      variantCount: 5,
      tagCount: 3,
      category: "Jacket",
      brand: "Louis Vuitton",
      collection: "Luxury Sport",
      image:
        "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?q=80&w=400",
    },

    {
      id: 6,
      name: "Women Croptop",
      slug: "women-croptop",
      material: "Cotton Spandex",
      fit: "Slim",
      gender: "FEMALE",
      status: "DRAFT",
      variantCount: 2,
      tagCount: 2,
      category: "Croptop",
      brand: "Chanel",
      collection: "Pink Collection",
      image:
        "https://images.unsplash.com/photo-1483985988355-763728e1935b?q=80&w=400",
    },
  ];

  const getStatusConfig = (status) => {
    switch (status) {
      case "ACTIVE":
        return {
          label: "Đang bán",
          className:
            "bg-emerald-500/15 text-emerald-400 border border-emerald-500/20",
        };

      case "DRAFT":
        return {
          label: "Nháp",
          className:
            "bg-amber-500/15 text-amber-400 border border-amber-500/20",
        };

      case "ARCHIVE":
        return {
          label: "Lưu trữ",
          className:
            "bg-red-500/15 text-red-400 border border-red-500/20",
        };

      default:
        return {
          label: "Không xác định",
          className:
            "bg-zinc-500/15 text-zinc-300 border border-zinc-500/20",
        };
    }
  };

  const filteredProducts = useMemo(() => {
    return products.filter((product) => {
      const keyword = searchTerm.toLowerCase();

      const matchSearch =
        product.name.toLowerCase().includes(keyword) ||
        product.slug.toLowerCase().includes(keyword) ||
        product.brand.toLowerCase().includes(keyword);

      const matchStatus =
        statusFilter === "ALL" ||
        product.status === statusFilter;

      return matchSearch && matchStatus;
    });
  }, [searchTerm, statusFilter]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredProducts.length / PAGE_SIZE)
  );

  const pagedProducts = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;

    return filteredProducts.slice(
      start,
      start + PAGE_SIZE
    );
  }, [filteredProducts, currentPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, statusFilter]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const resetFilters = () => {
    setSearchTerm("");
    setStatusFilter("ALL");
  };

  return (
    <div>
      {/* FILTER */}
      <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 mb-8">
        <div className="flex flex-wrap items-center gap-4">
          {/* SEARCH */}
          <div className="relative">
            <input
              type="text"
              placeholder="Tìm theo tên, slug, brand..."
              value={searchTerm}
              onChange={(e) =>
                setSearchTerm(e.target.value)
              }
              className="bg-zinc-800 border border-zinc-700 focus:border-amber-400 rounded-2xl py-3 pl-11 pr-4 w-96 max-w-full focus:outline-none text-sm"
            />

            <Search
              size={18}
              className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"
            />
          </div>

          {/* STATUS */}
          <select
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value)
            }
            className="bg-zinc-800 border border-zinc-700 rounded-2xl px-4 py-3 text-sm focus:outline-none focus:border-amber-400"
          >
            <option value="ALL">
              Tất cả trạng thái
            </option>

            <option value="ACTIVE">
              Đang bán
            </option>

            <option value="DRAFT">Nháp</option>

            <option value="ARCHIVE">
              Lưu trữ
            </option>
          </select>

          {/* RESET */}
          <button
            onClick={resetFilters}
            className="bg-blue-500 hover:bg-blue-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <RotateCcw size={18} />
            <span>Reset</span>
          </button>

          {/* ADD */}
          <button
            onClick={() =>
              alert("Mở form thêm sản phẩm")
            }
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm sản phẩm</span>
          </button>
        </div>
      </div>

      {/* STATS */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Tổng sản phẩm
          </p>

          <p className="text-4xl font-bold mt-2">
            {products.length}
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Đang bán
          </p>

          <p className="text-4xl font-bold mt-2 text-emerald-400">
            {
              products.filter(
                (p) => p.status === "ACTIVE"
              ).length
            }
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">Nháp</p>

          <p className="text-4xl font-bold mt-2 text-amber-400">
            {
              products.filter(
                (p) => p.status === "DRAFT"
              ).length
            }
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Lưu trữ
          </p>

          <p className="text-4xl font-bold mt-2 text-red-400">
            {
              products.filter(
                (p) => p.status === "ARCHIVE"
              ).length
            }
          </p>
        </div>
      </div>

      {/* TABLE */}
      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">
            Danh sách sản phẩm
          </h3>

          <p className="text-sm text-zinc-400">
            Tìm thấy:
            <span className="font-medium text-white ml-1">
              {filteredProducts.length}
            </span>
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[1400px]">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-4 font-normal">
                  Sản phẩm
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Chất liệu
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Fit
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Gender
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Danh mục
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Brand
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Bộ sưu tập
                </th>

                <th className="text-center py-5 px-4 font-normal">
                  Variant
                </th>

                <th className="text-center py-5 px-4 font-normal">
                  Tag
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Status
                </th>

                <th className="text-center py-5 px-4 font-normal">
                  Action
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedProducts.map((product) => {
                const status = getStatusConfig(
                  product.status
                );

                return (
                  <tr
                    key={product.id}
                    className="hover:bg-zinc-800 transition-colors"
                  >
                    {/* PRODUCT */}
                    <td className="px-4 py-5">
                      <div className="flex items-center gap-4">
                        <div className="w-16 h-16 rounded-2xl overflow-hidden bg-zinc-800">
                          <img
                            src={product.image}
                            alt={product.name}
                            className="w-full h-full object-cover"
                          />
                        </div>

                        <div>
                          <p className="font-semibold">
                            {product.name}
                          </p>

                          <p className="text-xs text-zinc-500 mt-1">
                            {product.slug}
                          </p>
                        </div>
                      </div>
                    </td>

                    {/* MATERIAL */}
                    <td className="px-4 py-5">
                      {product.material}
                    </td>

                    {/* FIT */}
                    <td className="px-4 py-5">
                      {product.fit}
                    </td>

                    {/* GENDER */}
                    <td className="px-4 py-5">
                      {product.gender}
                    </td>

                    {/* CATEGORY */}
                    <td className="px-4 py-5">
                      {product.category}
                    </td>

                    {/* BRAND */}
                    <td className="px-4 py-5">
                      {product.brand}
                    </td>

                    {/* COLLECTION */}
                    <td className="px-4 py-5">
                      {product.collection}
                    </td>

                    {/* VARIANT */}
                    <td className="px-4 py-5 text-center font-semibold text-blue-400">
                      {product.variantCount}
                    </td>

                    {/* TAG */}
                    <td className="px-4 py-5 text-center font-semibold text-purple-400">
                      {product.tagCount}
                    </td>

                    {/* STATUS */}
                    <td className="px-4 py-5">
                      <span
                        className={`px-4 py-2 rounded-full text-xs font-medium ${status.className}`}
                      >
                        {status.label}
                      </span>
                    </td>

                    {/* ACTION */}
                    <td className="px-4 py-5">
                      <div className="flex items-center justify-center gap-4">
                        {/* VIEW */}
                        <button
                          onClick={() =>
                            alert(
                              `Xem sản phẩm ID: ${product.id}`
                            )
                          }
                          className="text-blue-400 hover:text-blue-300 transition-colors"
                          title="Xem"
                        >
                          <Eye size={18} />
                        </button>

                        {/* EDIT */}
                        <button
                          onClick={() =>
                            alert(
                              `Sửa sản phẩm ID: ${product.id}`
                            )
                          }
                          className="text-amber-400 hover:text-amber-300 transition-colors"
                          title="Sửa"
                        >
                          <PenSquare size={18} />
                        </button>

                        {/* DELETE */}
                        {product.status !==
                          "ARCHIVE" && (
                          <button
                            onClick={() =>
                              alert(
                                `Archive sản phẩm ID: ${product.id}`
                              )
                            }
                            className="text-red-400 hover:text-red-300 transition-colors"
                            title="Archive"
                          >
                            <Trash2 size={18} />
                          </button>
                        )}

                        {/* RESTORE */}
                        {product.status ===
                          "ARCHIVE" && (
                          <button
                            onClick={() =>
                              alert(
                                `Khôi phục sản phẩm ID: ${product.id}`
                              )
                            }
                            className="text-emerald-400 hover:text-emerald-300 transition-colors"
                            title="Khôi phục"
                          >
                            <Undo2 size={18} />
                          </button>
                        )}

                        {/* ARCHIVE */}
                        {product.status ===
                          "ACTIVE" && (
                          <button
                            onClick={() =>
                              alert(
                                `Lưu trữ sản phẩm ID: ${product.id}`
                              )
                            }
                            className="text-orange-400 hover:text-orange-300 transition-colors"
                            title="Lưu trữ"
                          >
                            <Archive size={18} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      </div>
    </div>
  );
}