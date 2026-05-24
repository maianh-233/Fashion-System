import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  PenSquare,
  RotateCcw,
  FolderTree,
} from "lucide-react";
import Pagination from "../../../components/common/Pagination";

const PAGE_SIZE = 5;

export default function CategoryManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const categories = [
    {
      id: 1,
      code: "MEN",
      name: "Thời trang nam",
      parentCategory: "Không có",
      childrenCount: 5,
    },
    {
      id: 2,
      code: "WOMEN",
      name: "Thời trang nữ",
      parentCategory: "Không có",
      childrenCount: 8,
    },
    {
      id: 3,
      code: "SHIRT",
      name: "Áo sơ mi",
      parentCategory: "Thời trang nam",
      childrenCount: 2,
    },
    {
      id: 4,
      code: "TROUSER",
      name: "Quần tây",
      parentCategory: "Thời trang nam",
      childrenCount: 1,
    },
    {
      id: 5,
      code: "DRESS",
      name: "Đầm nữ",
      parentCategory: "Thời trang nữ",
      childrenCount: 4,
    },
    {
      id: 6,
      code: "BAG",
      name: "Túi xách",
      parentCategory: "Phụ kiện",
      childrenCount: 3,
    },
  ];

  const filteredCategories = useMemo(() => {
    return categories.filter((category) => {
      return (
        category.name
          .toLowerCase()
          .includes(searchTerm.toLowerCase()) ||
        category.code
          .toLowerCase()
          .includes(searchTerm.toLowerCase()) ||
        category.parentCategory
          .toLowerCase()
          .includes(searchTerm.toLowerCase())
      );
    });
  }, [searchTerm]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredCategories.length / PAGE_SIZE)
  );

  const pagedCategories = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;

    return filteredCategories.slice(
      start,
      start + PAGE_SIZE
    );
  }, [filteredCategories, currentPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const resetFilters = () => {
    setSearchTerm("");
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
              placeholder="Tìm theo tên, code hoặc danh mục cha..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="bg-zinc-800 border border-zinc-700 focus:border-amber-400 rounded-2xl py-3 pl-11 pr-4 w-96 max-w-full focus:outline-none text-sm"
            />

            <Search
              size={18}
              className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"
            />
          </div>

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
              alert("Mở form thêm danh mục")
            }
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm danh mục</span>
          </button>
        </div>
      </div>

      {/* STATS */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Tổng danh mục
          </p>

          <p className="text-4xl font-bold mt-2">
            {categories.length}
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Danh mục cha
          </p>

          <p className="text-4xl font-bold mt-2 text-amber-400">
            {
              categories.filter(
                (item) =>
                  item.parentCategory === "Không có"
              ).length
            }
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Tổng danh mục con
          </p>

          <p className="text-4xl font-bold mt-2 text-emerald-400">
            {categories.reduce(
              (total, item) =>
                total + item.childrenCount,
              0
            )}
          </p>
        </div>
      </div>

      {/* TABLE */}
      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">
            Danh sách danh mục
          </h3>

          <p className="text-sm text-zinc-400">
            Tìm thấy:
            <span className="font-medium text-white ml-1">
              {filteredCategories.length}
            </span>
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full table-fixed">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-4 font-normal w-[15%]">
                  Code
                </th>

                <th className="text-left py-5 px-4 font-normal w-[30%]">
                  Tên danh mục
                </th>

                <th className="text-left py-5 px-4 font-normal w-[25%]">
                  Danh mục cha
                </th>

                <th className="text-center py-5 px-4 font-normal w-[15%]">
                  SL danh mục con
                </th>

                <th className="text-center py-5 px-4 font-normal w-[15%]">
                  Action
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedCategories.map((category) => {
                return (
                  <tr
                    key={category.id}
                    className="hover:bg-zinc-800 transition-colors"
                  >
                    {/* CODE */}
                    <td className="px-4 py-5 font-medium text-amber-400">
                      {category.code}
                    </td>

                    {/* NAME */}
                    <td className="px-4 py-5">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-2xl bg-amber-500/15 flex items-center justify-center">
                          <FolderTree
                            size={22}
                            className="text-amber-400"
                          />
                        </div>

                        <div>
                          <p className="font-semibold">
                            {category.name}
                          </p>

                          <p className="text-xs text-zinc-500 mt-1">
                            Product Category
                          </p>
                        </div>
                      </div>
                    </td>

                    {/* PARENT */}
                    <td className="px-4 py-5 text-zinc-300">
                      {category.parentCategory}
                    </td>

                    {/* CHILD COUNT */}
                    <td className="px-4 py-5 text-center">
                      <span className="px-4 py-2 rounded-full bg-blue-500/15 text-blue-400 border border-blue-500/20 text-xs font-medium">
                        {category.childrenCount}
                      </span>
                    </td>

                    {/* ACTION */}
                    <td className="px-4 py-5">
                      <div className="flex items-center justify-center gap-4">
                        {/* VIEW */}
                        <button
                          onClick={() =>
                            alert(
                              `Xem danh mục ID: ${category.id}`
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
                              `Sửa danh mục ID: ${category.id}`
                            )
                          }
                          className="text-amber-400 hover:text-amber-300 transition-colors"
                          title="Sửa"
                        >
                          <PenSquare size={18} />
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