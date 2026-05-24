import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  PenSquare,
  RotateCcw,
  Trash2,
  Undo2,
  Clock3,
} from "lucide-react";
import Pagination from "../../../components/common/Pagination";

const PAGE_SIZE = 5;

export default function CollectionManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState(1);

  const collections = [
    {
      id: 1,
      code: "SUMMER2026",
      name: "Summer Vibes 2026",
      brand: "Nike",
      status: "ACTIVE",
      releaseDate: "20/05/2026",
      createdAt: "10/05/2026",
    },
    {
      id: 2,
      code: "LUXURYWINTER",
      name: "Luxury Winter",
      brand: "Gucci",
      status: "COMING_SOON",
      releaseDate: "15/06/2026",
      createdAt: "02/05/2026",
    },
    {
      id: 3,
      code: "URBANSTREET",
      name: "Urban Street",
      brand: "Puma",
      status: "ACTIVE",
      releaseDate: "01/04/2026",
      createdAt: "20/03/2026",
    },
    {
      id: 4,
      code: "CLASSICMEN",
      name: "Classic Men",
      brand: "Louis Vuitton",
      status: "DELETED",
      releaseDate: "11/02/2026",
      createdAt: "01/02/2026",
    },
    {
      id: 5,
      code: "CHANELSPRING",
      name: "Chanel Spring",
      brand: "Chanel",
      status: "ACTIVE",
      releaseDate: "25/03/2026",
      createdAt: "15/03/2026",
    },
    {
      id: 6,
      code: "SPORTEDGE",
      name: "Sport Edge",
      brand: "Adidas",
      status: "COMING_SOON",
      releaseDate: "01/07/2026",
      createdAt: "18/05/2026",
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
          label: "Xóa mềm",
          className:
            "bg-red-500/15 text-red-400 border border-red-500/20",
        };

      case "COMING_SOON":
        return {
          label: "Sắp ra mắt",
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

  const filteredCollections = useMemo(() => {
    return collections.filter((collection) => {
      const matchSearch =
        collection.name
          .toLowerCase()
          .includes(searchTerm.toLowerCase()) ||
        collection.code
          .toLowerCase()
          .includes(searchTerm.toLowerCase()) ||
        collection.brand
          .toLowerCase()
          .includes(searchTerm.toLowerCase());

      const matchStatus =
        statusFilter === "ALL" ||
        collection.status === statusFilter;

      return matchSearch && matchStatus;
    });
  }, [searchTerm, statusFilter]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredCollections.length / PAGE_SIZE)
  );

  const pagedCollections = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredCollections.slice(
      start,
      start + PAGE_SIZE
    );
  }, [filteredCollections, currentPage]);

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
              placeholder="Tìm theo tên, code hoặc brand..."
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
            <option value="COMING_SOON">Sắp ra mắt</option>
            <option value="DELETED">Xóa mềm</option>
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
              alert("Mở form thêm bộ sưu tập")
            }
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm bộ sưu tập</span>
          </button>
        </div>
      </div>

      {/* STATS */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Tổng bộ sưu tập
          </p>

          <p className="text-4xl font-bold mt-2">
            {collections.length}
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">Đang hoạt động</p>

          <p className="text-4xl font-bold mt-2 text-emerald-400">
            {
              collections.filter(
                (item) => item.status === "ACTIVE"
              ).length
            }
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">Sắp ra mắt</p>

          <p className="text-4xl font-bold mt-2 text-amber-400">
            {
              collections.filter(
                (item) =>
                  item.status === "COMING_SOON"
              ).length
            }
          </p>
        </div>
      </div>

      {/* TABLE */}
      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">
            Danh sách bộ sưu tập
          </h3>

          <p className="text-sm text-zinc-400">
            Tìm thấy:
            <span className="font-medium text-white ml-1">
              {filteredCollections.length}
            </span>
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full table-fixed">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-4 font-normal w-[12%]">
                  Code
                </th>

                <th className="text-left py-5 px-4 font-normal w-[22%]">
                  Tên bộ sưu tập
                </th>

                <th className="text-left py-5 px-4 font-normal w-[15%]">
                  Brand
                </th>

                <th className="text-left py-5 px-4 font-normal w-[16%]">
                  Trạng thái
                </th>

                <th className="text-left py-5 px-4 font-normal w-[15%]">
                  Ngày ra mắt
                </th>

                <th className="text-left py-5 px-4 font-normal w-[12%]">
                  Ngày tạo
                </th>

                <th className="text-center py-5 px-4 font-normal w-[18%]">
                  Action
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedCollections.map((collection) => {
                const status = getStatusConfig(
                  collection.status
                );

                return (
                  <tr
                    key={collection.id}
                    className="hover:bg-zinc-800 transition-colors"
                  >
                    {/* CODE */}
                    <td className="px-4 py-5 font-medium text-amber-400">
                      {collection.code}
                    </td>

                    {/* NAME */}
                    <td className="px-4 py-5">
                      <div>
                        <p className="font-semibold">
                          {collection.name}
                        </p>

                        <p className="text-xs text-zinc-500 mt-1">
                          Fashion Collection
                        </p>
                      </div>
                    </td>

                    {/* BRAND */}
                    <td className="px-4 py-5 text-zinc-300">
                      {collection.brand}
                    </td>

                    {/* STATUS */}
                    <td className="px-4 py-5">
                      <span
                        className={`px-4 py-2 rounded-full text-xs font-medium ${status.className}`}
                      >
                        {status.label}
                      </span>
                    </td>

                    {/* RELEASE DATE */}
                    <td className="px-4 py-5 text-zinc-300">
                      {collection.releaseDate}
                    </td>

                    {/* CREATED DATE */}
                    <td className="px-4 py-5 text-zinc-300">
                      {collection.createdAt}
                    </td>

                    {/* ACTION */}
                    <td className="px-4 py-5">
                      <div className="flex items-center justify-center gap-4">
                        {/* VIEW */}
                        <button
                          onClick={() =>
                            alert(
                              `Xem collection ID: ${collection.id}`
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
                              `Sửa collection ID: ${collection.id}`
                            )
                          }
                          className="text-amber-400 hover:text-amber-300 transition-colors"
                          title="Sửa"
                        >
                          <PenSquare size={18} />
                        </button>

                        {/* SOFT DELETE */}
                        {collection.status !==
                          "DELETED" && (
                          <button
                            onClick={() =>
                              alert(
                                `Xóa mềm collection ID: ${collection.id}`
                              )
                            }
                            className="text-red-400 hover:text-red-300 transition-colors"
                            title="Xóa mềm"
                          >
                            <Trash2 size={18} />
                          </button>
                        )}

                        {/* RESTORE */}
                        {collection.status ===
                          "DELETED" && (
                          <button
                            onClick={() =>
                              alert(
                                `Khôi phục collection ID: ${collection.id}`
                              )
                            }
                            className="text-emerald-400 hover:text-emerald-300 transition-colors"
                            title="Khôi phục"
                          >
                            <Undo2 size={18} />
                          </button>
                        )}

                        {/* COMING SOON */}
                        {collection.status ===
                          "COMING_SOON" && (
                          <button
                            className="text-amber-400 hover:text-amber-300 transition-colors"
                            title="Sắp ra mắt"
                          >
                            <Clock3 size={18} />
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