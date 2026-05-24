import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  PenSquare,
  RotateCcw,
  Trash2,
  ToggleLeft,
  ToggleRight,
} from "lucide-react";

import Pagination from "../../../components/common/Pagination";

const PAGE_SIZE = 6;

export default function VariantManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState(1);

  const variants = [
    {
      id: 1,
      productName: "Oversize Hoodie Black",
      image:
        "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?q=80&w=400",
      sku: "HD-BLK-XL",
      barcode: "893850123001",
      color: "Black",
      size: "XL",
      weight: "850g",
      price: 890000,
      salePrice: 790000,
      status: "ACTIVE",
    },

    {
      id: 2,
      productName: "Oversize Hoodie Black",
      image:
        "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?q=80&w=400",
      sku: "HD-BLK-L",
      barcode: "893850123002",
      color: "Black",
      size: "L",
      weight: "820g",
      price: 890000,
      salePrice: null,
      status: "ACTIVE",
    },

    {
      id: 3,
      productName: "Slim Fit Polo",
      image:
        "https://images.unsplash.com/photo-1576566588028-4147f3842f27?q=80&w=400",
      sku: "POLO-WHITE-M",
      barcode: "893850123003",
      color: "White",
      size: "M",
      weight: "350g",
      price: 490000,
      salePrice: 390000,
      status: "INACTIVE",
    },

    {
      id: 4,
      productName: "Cargo Pants Grey",
      image:
        "https://images.unsplash.com/photo-1506629905607-d9c297d75d72?q=80&w=400",
      sku: "CG-GREY-32",
      barcode: "893850123004",
      color: "Grey",
      size: "32",
      weight: "700g",
      price: 750000,
      salePrice: 650000,
      status: "ACTIVE",
    },

    {
      id: 5,
      productName: "Women Croptop",
      image:
        "https://images.unsplash.com/photo-1483985988355-763728e1935b?q=80&w=400",
      sku: "CR-PINK-S",
      barcode: "893850123005",
      color: "Pink",
      size: "S",
      weight: "250g",
      price: 390000,
      salePrice: null,
      status: "ACTIVE",
    },

    {
      id: 6,
      productName: "Varsity Jacket",
      image:
        "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?q=80&w=400",
      sku: "VJ-BLACK-L",
      barcode: "893850123006",
      color: "Black",
      size: "L",
      weight: "1200g",
      price: 1290000,
      salePrice: 1090000,
      status: "INACTIVE",
    },
  ];

  const formatPrice = (price) => {
    return new Intl.NumberFormat("vi-VN").format(price) + "đ";
  };

  const getStatusConfig = (status) => {
    switch (status) {
      case "ACTIVE":
        return {
          label: "Đang bán",
          className:
            "bg-emerald-500/15 text-emerald-400 border border-emerald-500/20",
        };

      case "INACTIVE":
        return {
          label: "Ngừng bán",
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

  const filteredVariants = useMemo(() => {
    return variants.filter((variant) => {
      const keyword = searchTerm.toLowerCase();

      const matchSearch =
        variant.productName
          .toLowerCase()
          .includes(keyword) ||
        variant.sku.toLowerCase().includes(keyword) ||
        variant.barcode.includes(keyword);

      const matchStatus =
        statusFilter === "ALL" ||
        variant.status === statusFilter;

      return matchSearch && matchStatus;
    });
  }, [searchTerm, statusFilter]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredVariants.length / PAGE_SIZE)
  );

  const pagedVariants = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;

    return filteredVariants.slice(
      start,
      start + PAGE_SIZE
    );
  }, [filteredVariants, currentPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, statusFilter]);

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
              placeholder="Tìm theo sản phẩm, SKU, barcode..."
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

            <option value="INACTIVE">
              Ngừng bán
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
              alert("Mở form thêm biến thể")
            }
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm biến thể</span>
          </button>
        </div>
      </div>

      {/* STATS */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Tổng biến thể
          </p>

          <p className="text-4xl font-bold mt-2">
            {variants.length}
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Đang bán
          </p>

          <p className="text-4xl font-bold mt-2 text-emerald-400">
            {
              variants.filter(
                (v) => v.status === "ACTIVE"
              ).length
            }
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Ngừng bán
          </p>

          <p className="text-4xl font-bold mt-2 text-red-400">
            {
              variants.filter(
                (v) => v.status === "INACTIVE"
              ).length
            }
          </p>
        </div>
      </div>

      {/* TABLE */}
      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">
            Danh sách biến thể
          </h3>

          <p className="text-sm text-zinc-400">
            Tìm thấy:
            <span className="font-medium text-white ml-1">
              {filteredVariants.length}
            </span>
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[1400px]">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-4 font-normal min-w-[300px]">
                  Sản phẩm
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  SKU
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Barcode
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Màu
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Size
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Trọng lượng
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Giá
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Giá giảm
                </th>

                <th className="text-left py-5 px-4 font-normal">
                  Trạng thái
                </th>

                <th className="text-center py-5 px-4 font-normal">
                  Action
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedVariants.map((variant) => {
                const status = getStatusConfig(
                  variant.status
                );

                return (
                  <tr
                    key={variant.id}
                    className="hover:bg-zinc-800 transition-colors"
                  >
                    {/* PRODUCT */}
                    <td className="px-4 py-5">
                      <div className="flex items-center gap-4">
                        <div className="w-16 h-16 rounded-2xl overflow-hidden bg-zinc-800 border border-zinc-700 shrink-0">
                          <img
                            src={variant.image}
                            alt={variant.productName}
                            className="w-full h-full object-cover"
                          />
                        </div>

                        <div>
                          <p className="font-semibold">
                            {variant.productName}
                          </p>

                          <div className="flex items-center gap-2 mt-2">
                            <span className="px-2 py-1 rounded-lg bg-zinc-800 border border-zinc-700 text-[10px]">
                              {variant.color}
                            </span>

                            <span className="px-2 py-1 rounded-lg bg-zinc-800 border border-zinc-700 text-[10px]">
                              Size {variant.size}
                            </span>
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* SKU */}
                    <td className="px-4 py-5 font-medium text-amber-400">
                      {variant.sku}
                    </td>

                    {/* BARCODE */}
                    <td className="px-4 py-5">
                      {variant.barcode}
                    </td>

                    {/* COLOR */}
                    <td className="px-4 py-5">
                      {variant.color}
                    </td>

                    {/* SIZE */}
                    <td className="px-4 py-5">
                      {variant.size}
                    </td>

                    {/* WEIGHT */}
                    <td className="px-4 py-5">
                      {variant.weight}
                    </td>

                    {/* PRICE */}
                    <td className="px-4 py-5 font-semibold">
                      {formatPrice(variant.price)}
                    </td>

                    {/* SALE PRICE */}
                    <td className="px-4 py-5">
                      {variant.salePrice ? (
                        <span className="text-red-400 font-semibold">
                          {formatPrice(
                            variant.salePrice
                          )}
                        </span>
                      ) : (
                        <span className="text-zinc-500">
                          Không có
                        </span>
                      )}
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
                              `Xem biến thể ID: ${variant.id}`
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
                              `Sửa biến thể ID: ${variant.id}`
                            )
                          }
                          className="text-amber-400 hover:text-amber-300 transition-colors"
                          title="Sửa"
                        >
                          <PenSquare size={18} />
                        </button>

                        {/* TOGGLE STATUS */}
                        <button
                          onClick={() =>
                            alert(
                              `Đổi trạng thái biến thể ID: ${variant.id}`
                            )
                          }
                          className={
                            variant.status === "ACTIVE"
                              ? "text-emerald-400 hover:text-emerald-300 transition-colors"
                              : "text-zinc-400 hover:text-white transition-colors"
                          }
                          title="Đổi trạng thái"
                        >
                          {variant.status ===
                          "ACTIVE" ? (
                            <ToggleRight size={22} />
                          ) : (
                            <ToggleLeft size={22} />
                          )}
                        </button>

                        {/* DELETE */}
                        <button
                          onClick={() =>
                            alert(
                              `Xóa biến thể ID: ${variant.id}`
                            )
                          }
                          className="text-red-400 hover:text-red-300 transition-colors"
                          title="Xóa"
                        >
                          <Trash2 size={18} />
                        </button>
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