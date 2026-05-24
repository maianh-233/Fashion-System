import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  PenSquare,
  Trash2,
  RotateCcw,
  TicketPercent,
  CircleCheck,
  Clock3,
  BadgePercent,
} from "lucide-react";
import Pagination from "../../components/common/Pagination";

const PAGE_SIZE = 5;

export default function PromotionManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const promotions = [
    {
      id: 1,
      code: "KM001",
      name: "Sale Hè 2026",
      type: "Giảm %",
      startDate: "01/05/2026",
      endDate: "31/05/2026",
      status: "active",
      promotionStatus: "running",
    },
    {
      id: 2,
      code: "KM002",
      name: "Flash Sale Cuối Tuần",
      type: "Voucher",
      startDate: "15/05/2026",
      endDate: "18/05/2026",
      status: "active",
      promotionStatus: "expired",
    },
    {
      id: 3,
      code: "KM003",
      name: "Giảm Giá Thành Viên VIP",
      type: "Giảm tiền",
      startDate: "20/05/2026",
      endDate: "25/05/2026",
      status: "deleted",
      promotionStatus: "upcoming",
    },
    {
      id: 4,
      code: "KM004",
      name: "Mừng Khai Trương",
      type: "Freeship",
      startDate: "22/05/2026",
      endDate: "30/05/2026",
      status: "active",
      promotionStatus: "upcoming",
    },
    {
      id: 5,
      code: "KM005",
      name: "Sale Sinh Nhật",
      type: "Giảm %",
      startDate: "10/05/2026",
      endDate: "28/05/2026",
      status: "active",
      promotionStatus: "running",
    },
  ];

  const filteredPromotions = useMemo(
    () =>
      promotions.filter((promotion) => {
        return (
          promotion.code
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          promotion.name
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          promotion.type
            .toLowerCase()
            .includes(searchTerm.toLowerCase())
        );
      }),
    [searchTerm]
  );

  const totalPages = Math.max(
    1,
    Math.ceil(filteredPromotions.length / PAGE_SIZE)
  );

  const pagedPromotions = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredPromotions.slice(start, start + PAGE_SIZE);
  }, [filteredPromotions, currentPage]);

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
          <div className="relative">
            <input
              type="text"
              placeholder="Tìm theo code, tên khuyến mãi..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="bg-zinc-800 border border-zinc-700 focus:border-amber-400 rounded-2xl py-3 pl-11 pr-4 w-96 max-w-full focus:outline-none text-sm"
            />

            <Search
              size={18}
              className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"
            />
          </div>

          <button
            onClick={resetFilters}
            className="bg-blue-500 hover:bg-blue-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <RotateCcw size={18} />
            <span>Reset</span>
          </button>

          <button
            onClick={() => alert("Mở form thêm khuyến mãi")}
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm khuyến mãi</span>
          </button>
        </div>
      </div>

      {/* STATS */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Tổng khuyến mãi</p>
              <p className="text-4xl font-bold mt-2">18</p>
            </div>

            <TicketPercent size={40} className="text-blue-400" />
          </div>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Đang hoạt động</p>
              <p className="text-4xl font-bold mt-2 text-emerald-400">
                10
              </p>
            </div>

            <CircleCheck size={40} className="text-emerald-400" />
          </div>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Đang diễn ra</p>
              <p className="text-4xl font-bold mt-2 text-amber-400">
                6
              </p>
            </div>

            <BadgePercent size={40} className="text-amber-400" />
          </div>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Sắp diễn ra</p>
              <p className="text-4xl font-bold mt-2 text-purple-400">
                2
              </p>
            </div>

            <Clock3 size={40} className="text-purple-400" />
          </div>
        </div>
      </div>

      {/* TABLE */}
      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">
            Danh sách khuyến mãi
          </h3>

          <p className="text-sm text-zinc-400">
            Tìm thấy:
            <span className="font-medium text-white ml-1">
              {filteredPromotions.length}
            </span>
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-6 font-normal">
                  Code
                </th>

                <th className="text-left py-5 px-6 font-normal">
                  Tên
                </th>

                <th className="text-left py-5 px-6 font-normal">
                  Loại
                </th>

                <th className="text-center py-5 px-6 font-normal">
                  Ngày bắt đầu
                </th>

                <th className="text-center py-5 px-6 font-normal">
                  Ngày kết thúc
                </th>

                <th className="text-center py-5 px-6 font-normal">
                  Trạng thái
                </th>

                <th className="text-center py-5 px-6 font-normal">
                  Tiến độ
                </th>

                <th className="text-center py-5 px-6 font-normal w-52">
                  Action
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedPromotions.map((promotion) => (
                <tr
                  key={promotion.id}
                  className="hover:bg-zinc-800 transition-colors"
                >
                  <td className="px-6 py-5 font-medium text-amber-400">
                    {promotion.code}
                  </td>

                  <td className="px-6 py-5 font-medium">
                    {promotion.name}
                  </td>

                  <td className="px-6 py-5">
                    {promotion.type}
                  </td>

                  <td className="px-6 py-5 text-center">
                    {promotion.startDate}
                  </td>

                  <td className="px-6 py-5 text-center">
                    {promotion.endDate}
                  </td>

                  {/* STATUS */}
                  <td className="px-6 py-5 text-center">
                    {promotion.status === "active" ? (
                      <span className="bg-emerald-500/20 text-emerald-400 px-4 py-1 rounded-full text-xs">
                        Hoạt động
                      </span>
                    ) : (
                      <span className="bg-red-500/20 text-red-400 px-4 py-1 rounded-full text-xs">
                        Đã xóa mềm
                      </span>
                    )}
                  </td>

                  {/* PROMOTION STATUS */}
                  <td className="px-6 py-5 text-center">
                    {promotion.promotionStatus === "running" && (
                      <span className="bg-emerald-500/20 text-emerald-400 px-4 py-1 rounded-full text-xs">
                        Đang diễn ra
                      </span>
                    )}

                    {promotion.promotionStatus === "upcoming" && (
                      <span className="bg-amber-500/20 text-amber-400 px-4 py-1 rounded-full text-xs">
                        Sắp diễn ra
                      </span>
                    )}

                    {promotion.promotionStatus === "expired" && (
                      <span className="bg-zinc-700 text-zinc-300 px-4 py-1 rounded-full text-xs">
                        Hết hạn
                      </span>
                    )}
                  </td>

                  {/* ACTION */}
                  <td className="px-6 py-5">
                    <div className="flex items-center justify-center gap-3">
                      {/* VIEW */}
                      <button
                        onClick={() =>
                          alert(
                            `Xem chi tiết khuyến mãi ID: ${promotion.id}`
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
                            `Sửa khuyến mãi ID: ${promotion.id}`
                          )
                        }
                        className="text-amber-400 hover:text-amber-300 transition-colors"
                        title="Sửa"
                      >
                        <PenSquare size={18} />
                      </button>

                      {/* DELETE / RESTORE */}
                      {promotion.status === "deleted" ? (
                        <button
                          onClick={() =>
                            alert(
                              `Khôi phục khuyến mãi ID ${promotion.id}`
                            )
                          }
                          className="text-emerald-400 hover:text-emerald-300 transition-colors"
                          title="Khôi phục"
                        >
                          <RotateCcw size={18} />
                        </button>
                      ) : (
                        <button
                          onClick={() =>
                            confirm("Xóa mềm khuyến mãi này?") &&
                            alert(
                              `Đã xóa mềm khuyến mãi ID ${promotion.id}`
                            )
                          }
                          className="text-red-400 hover:text-red-300 transition-colors"
                          title="Xóa mềm"
                        >
                          <Trash2 size={18} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
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