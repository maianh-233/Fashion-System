import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  PenSquare,
  RotateCcw,
  Trash2,
  Undo2,
  Ban,
} from "lucide-react";
import Pagination from "../../../components/admin/Pagination";

const PAGE_SIZE = 5;

export default function BrandManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState(1);

  const brands = [
    {
      id: 1,
      code: "NIKE",
      name: "Nike",
      logo: "https://upload.wikimedia.org/wikipedia/commons/a/a6/Logo_NIKE.svg",
      status: "ACTIVE",
    },
    {
      id: 2,
      code: "ADIDAS",
      name: "Adidas",
      logo: "https://upload.wikimedia.org/wikipedia/commons/2/20/Adidas_Logo.svg",
      status: "ACTIVE",
    },
    {
      id: 3,
      code: "PUMA",
      name: "Puma",
      logo: "https://upload.wikimedia.org/wikipedia/commons/f/fd/Puma_logo.svg",
      status: "STOPPED",
    },
    {
      id: 4,
      code: "GUCCI",
      name: "Gucci",
      logo: "https://upload.wikimedia.org/wikipedia/commons/7/79/1960s_Gucci_Logo.svg",
      status: "DELETED",
    },
    {
      id: 5,
      code: "LV",
      name: "Louis Vuitton",
      logo: "https://upload.wikimedia.org/wikipedia/commons/4/46/Louis_Vuitton_logo_and_wordmark.svg",
      status: "ACTIVE",
    },
    {
      id: 6,
      code: "CHANEL",
      name: "Chanel",
      logo: "https://upload.wikimedia.org/wikipedia/commons/9/92/Chanel_logo_interlocking_cs.svg",
      status: "ACTIVE",
    },
  ];

  const getStatusConfig = (status) => {
    switch (status) {
      case "ACTIVE":
        return {
          label: "Hoạt động",
          className:
            "bg-emerald-500/15 text-emerald-400 border border-emerald-500/20",
        };

      case "DELETED":
        return {
          label: "Đã xóa",
          className:
            "bg-red-500/15 text-red-400 border border-red-500/20",
        };

      case "STOPPED":
        return {
          label: "Ngưng hợp tác",
          className:
            "bg-amber-500/15 text-amber-400 border border-amber-500/20",
        };

      default:
        return {
          label: "Không xác định",
          className:
            "bg-zinc-500/15 text-zinc-300 border border-zinc-500/20",
        };
    }
  };

  const filteredBrands = useMemo(() => {
    return brands.filter((brand) => {
      const matchSearch =
        brand.name
          .toLowerCase()
          .includes(searchTerm.toLowerCase()) ||
        brand.code
          .toLowerCase()
          .includes(searchTerm.toLowerCase());

      const matchStatus =
        statusFilter === "ALL" ||
        brand.status === statusFilter;

      return matchSearch && matchStatus;
    });
  }, [searchTerm, statusFilter]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredBrands.length / PAGE_SIZE)
  );

  const pagedBrands = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredBrands.slice(start, start + PAGE_SIZE);
  }, [filteredBrands, currentPage]);

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
              placeholder="Tìm theo tên hoặc code..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="bg-zinc-800 border border-zinc-700 focus:border-amber-400 rounded-2xl py-3 pl-11 pr-4 w-96 max-w-full focus:outline-none text-sm"
            />

            <Search
              size={18}
              className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"
            />
          </div>

          {/* STATUS FILTER */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-zinc-800 border border-zinc-700 rounded-2xl px-4 py-3 text-sm focus:outline-none focus:border-amber-400"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Hoạt động</option>
            <option value="DELETED">Đã xóa</option>
            <option value="STOPPED">Ngưng hợp tác</option>
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
            onClick={() => alert("Mở form thêm brand")}
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm brand</span>
          </button>
        </div>
      </div>

      {/* STATS */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">Tổng brand</p>

          <p className="text-4xl font-bold mt-2">
            {brands.length}
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">Đang hoạt động</p>

          <p className="text-4xl font-bold mt-2 text-emerald-400">
            {
              brands.filter(
                (brand) => brand.status === "ACTIVE"
              ).length
            }
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">Ngưng hợp tác</p>

          <p className="text-4xl font-bold mt-2 text-amber-400">
            {
              brands.filter(
                (brand) => brand.status === "STOPPED"
              ).length
            }
          </p>
        </div>
      </div>

      {/* TABLE */}
      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">
            Danh sách brand
          </h3>

          <p className="text-sm text-zinc-400">
            Tìm thấy:
            <span className="font-medium text-white ml-1">
              {filteredBrands.length}
            </span>
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full table-fixed">
            <thead>
            <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-4 font-normal w-[45%]">
                Tên brand
                </th>

                <th className="text-left py-5 px-4 font-normal w-[20%]">
                Code
                </th>

                <th className="text-left py-5 px-4 font-normal w-[20%]">
                Trạng thái
                </th>

                <th className="text-center py-5 px-4 font-normal w-[15%]">
                Action
                </th>
            </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedBrands.map((brand) => {
                const status = getStatusConfig(
                  brand.status
                );

                return (
                  <tr
                    key={brand.id}
                    className="hover:bg-zinc-800 transition-colors"
                  >
                    {/* BRAND */}
                    <td className="px-4 py-5">
                      <div className="flex items-center gap-4">
                        <div className="w-14 h-14 bg-white rounded-2xl p-2 flex items-center justify-center overflow-hidden">
                          <img
                            src={brand.logo}
                            alt={brand.name}
                            className="w-full h-full object-contain"
                          />
                        </div>

                        <div>
                          <p className="font-semibold">
                            {brand.name}
                          </p>

                          <p className="text-xs text-zinc-500 mt-1">
                            Brand thời trang
                          </p>
                        </div>
                      </div>
                    </td>

                    {/* CODE */}
                    <td className="px-4 py-5 font-medium text-amber-400">
                      {brand.code}
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
                              `Xem brand ID: ${brand.id}`
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
                              `Sửa brand ID: ${brand.id}`
                            )
                          }
                          className="text-amber-400 hover:text-amber-300 transition-colors"
                          title="Sửa"
                        >
                          <PenSquare size={18} />
                        </button>

                        {/* SOFT DELETE */}
                        {brand.status !== "DELETED" && (
                          <button
                            onClick={() =>
                              alert(
                                `Xóa mềm brand ID: ${brand.id}`
                              )
                            }
                            className="text-red-400 hover:text-red-300 transition-colors"
                            title="Xóa mềm"
                          >
                            <Trash2 size={18} />
                          </button>
                        )}

                        {/* RESTORE */}
                        {brand.status === "DELETED" && (
                          <button
                            onClick={() =>
                              alert(
                                `Khôi phục brand ID: ${brand.id}`
                              )
                            }
                            className="text-emerald-400 hover:text-emerald-300 transition-colors"
                            title="Khôi phục"
                          >
                            <Undo2 size={18} />
                          </button>
                        )}

                        {/* STOP COOPERATION */}
                        {brand.status === "ACTIVE" && (
                          <button
                            onClick={() =>
                              alert(
                                `Ngưng hợp tác brand ID: ${brand.id}`
                              )
                            }
                            className="text-orange-400 hover:text-orange-300 transition-colors"
                            title="Ngưng hợp tác"
                          >
                            <Ban size={18} />
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