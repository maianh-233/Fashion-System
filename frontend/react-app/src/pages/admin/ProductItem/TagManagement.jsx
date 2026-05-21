import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  PenSquare,
  RotateCcw,
  Tag,
} from "lucide-react";
import Pagination from "../../../components/admin/Pagination";

const PAGE_SIZE = 5;

export default function TagManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const tags = [
    {
      id: 1,
      name: "NEW",
      productCount: 24,
      color:
        "bg-emerald-500/15 text-emerald-400 border border-emerald-500/20",
    },
    {
      id: 2,
      name: "SALE",
      productCount: 18,
      color:
        "bg-red-500/15 text-red-400 border border-red-500/20",
    },
    {
      id: 3,
      name: "HOT",
      productCount: 12,
      color:
        "bg-orange-500/15 text-orange-400 border border-orange-500/20",
    },
    {
      id: 4,
      name: "BEST SELLER",
      productCount: 9,
      color:
        "bg-amber-500/15 text-amber-400 border border-amber-500/20",
    },
    {
      id: 5,
      name: "LIMITED",
      productCount: 6,
      color:
        "bg-violet-500/15 text-violet-400 border border-violet-500/20",
    },
    {
      id: 6,
      name: "TRENDING",
      productCount: 14,
      color:
        "bg-sky-500/15 text-sky-400 border border-sky-500/20",
    },
  ];

  const filteredTags = useMemo(() => {
    return tags.filter((tag) =>
      tag.name
        .toLowerCase()
        .includes(searchTerm.toLowerCase())
    );
  }, [searchTerm]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredTags.length / PAGE_SIZE)
  );

  const pagedTags = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;

    return filteredTags.slice(
      start,
      start + PAGE_SIZE
    );
  }, [filteredTags, currentPage]);

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
              placeholder="Tìm theo tên tag..."
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
              alert("Mở form thêm tag")
            }
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm tag</span>
          </button>
        </div>
      </div>

      {/* STATS */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">Tổng tag</p>

          <p className="text-4xl font-bold mt-2">
            {tags.length}
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Tổng sản phẩm gắn tag
          </p>

          <p className="text-4xl font-bold mt-2 text-emerald-400">
            {tags.reduce(
              (total, tag) =>
                total + tag.productCount,
              0
            )}
          </p>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <p className="text-zinc-400">
            Tag phổ biến nhất
          </p>

          <p className="text-4xl font-bold mt-2 text-amber-400">
            SALE
          </p>
        </div>
      </div>

      {/* TABLE */}
      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">
            Danh sách tag
          </h3>

          <p className="text-sm text-zinc-400">
            Tìm thấy:
            <span className="font-medium text-white ml-1">
              {filteredTags.length}
            </span>
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full table-fixed">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-4 font-normal w-[50%]">
                  Tên tag
                </th>

                <th className="text-center py-5 px-4 font-normal w-[25%]">
                  Số lượng sản phẩm
                </th>

                <th className="text-center py-5 px-4 font-normal w-[25%]">
                  Action
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedTags.map((tag) => {
                return (
                  <tr
                    key={tag.id}
                    className="hover:bg-zinc-800 transition-colors"
                  >
                    {/* TAG NAME */}
                    <td className="px-4 py-5">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-2xl bg-zinc-800 flex items-center justify-center">
                          <Tag
                            size={20}
                            className="text-amber-400"
                          />
                        </div>

                        <div>
                          <span
                            className={`px-4 py-2 rounded-full text-xs font-semibold ${tag.color}`}
                          >
                            {tag.name}
                          </span>

                          <p className="text-xs text-zinc-500 mt-2">
                            Product Tag
                          </p>
                        </div>
                      </div>
                    </td>

                    {/* PRODUCT COUNT */}
                    <td className="px-4 py-5 text-center">
                      <span className="px-4 py-2 rounded-full bg-blue-500/15 text-blue-400 border border-blue-500/20 text-xs font-medium">
                        {tag.productCount} sản phẩm
                      </span>
                    </td>

                    {/* ACTION */}
                    <td className="px-4 py-5">
                      <div className="flex items-center justify-center gap-4">
                        {/* VIEW */}
                        <button
                          onClick={() =>
                            alert(
                              `Xem tag ID: ${tag.id}`
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
                              `Sửa tag ID: ${tag.id}`
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