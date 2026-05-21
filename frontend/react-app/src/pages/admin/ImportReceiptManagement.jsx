import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Eye,
  Settings,
  Ban,
  Trash2,
  RotateCcw,
  PackagePlus,
  Clock3,
  DollarSign,
  AlertTriangle,
  X,
  Plus,
} from "lucide-react";

import Pagination from "../../components/admin/Pagination";

const PAGE_SIZE = 4;

export default function ImportReceiptManagement() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [selectedReceipt, setSelectedReceipt] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [softDeletedIds, setSoftDeletedIds] = useState([2, 4]);

  const importReceiptsData = [
    {
      id: 1,
      code: "PN240521001",
      supplierCode: "NCC001",
      warehouse: {
        id: 1,
        name: "Kho Tổng HCM",
      },
      receiver: "Nguyễn Văn An",
      approver: "Trần Minh Quân",
      importDate: "2026-05-21",
      createdDate: "2026-05-20",
      status: "completed",
      totalQuantity: 125,
      totalAmount: 24500000,
    },

    {
      id: 2,
      code: "PN240521002",
      supplierCode: "NCC002",
      warehouse: {
        id: 2,
        name: "Kho Quận 7",
      },
      receiver: "Lê Thị Mai",
      approver: "Phạm Quốc Bảo",
      importDate: "2026-05-21",
      createdDate: "2026-05-21",
      status: "pending",
      totalQuantity: 48,
      totalAmount: 12800000,
    },

    {
      id: 3,
      code: "PN240520015",
      supplierCode: "NCC003",
      warehouse: {
        id: 3,
        name: "Kho Bình Tân",
      },
      receiver: "Trần Văn Long",
      approver: "Nguyễn Minh Đức",
      importDate: "2026-05-20",
      createdDate: "2026-05-19",
      status: "processing",
      totalQuantity: 92,
      totalAmount: 18600000,
    },

    {
      id: 4,
      code: "PN240520020",
      supplierCode: "NCC001",
      warehouse: {
        id: 4,
        name: "Kho Thủ Đức",
      },
      receiver: "Hoàng Quốc Anh",
      approver: "Phạm Minh Khôi",
      importDate: "2026-05-20",
      createdDate: "2026-05-20",
      status: "cancelled",
      totalQuantity: 35,
      totalAmount: 7200000,
    },

    {
      id: 5,
      code: "PN240519008",
      supplierCode: "NCC005",
      warehouse: {
        id: 1,
        name: "Kho Tổng HCM",
      },
      receiver: "Lê Quốc Huy",
      approver: "Nguyễn Minh Nhật",
      importDate: "2026-05-19",
      createdDate: "2026-05-18",
      status: "completed",
      totalQuantity: 210,
      totalAmount: 52800000,
    },
  ];

  const filteredReceipts = useMemo(() => {
    return importReceiptsData.filter((receipt) => {
      const matchSearch =
        receipt.code
          .toLowerCase()
          .includes(search.toLowerCase()) ||
        receipt.supplierCode
          .toLowerCase()
          .includes(search.toLowerCase()) ||
        receipt.receiver
          .toLowerCase()
          .includes(search.toLowerCase()) ||
        receipt.warehouse.name
          .toLowerCase()
          .includes(search.toLowerCase());

      const matchStatus =
        !statusFilter || receipt.status === statusFilter;

      return matchSearch && matchStatus;
    });
  }, [search, statusFilter]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredReceipts.length / PAGE_SIZE)
  );

  const pagedReceipts = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;

    return filteredReceipts.slice(
      start,
      start + PAGE_SIZE
    );
  }, [filteredReceipts, currentPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [search, statusFilter]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const resetFilters = () => {
    setSearch("");
    setStatusFilter("");
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case "pending":
        return (
          <span className="px-3 py-1 rounded-full text-sm font-medium bg-yellow-500/20 text-yellow-400">
            Chờ duyệt
          </span>
        );

      case "processing":
        return (
          <span className="px-3 py-1 rounded-full text-sm font-medium bg-blue-500/20 text-blue-400">
            Đang nhập kho
          </span>
        );

      case "completed":
        return (
          <span className="px-3 py-1 rounded-full text-sm font-medium bg-emerald-500/20 text-emerald-400">
            Hoàn thành
          </span>
        );

      default:
        return (
          <span className="px-3 py-1 rounded-full text-sm font-medium bg-red-500/20 text-red-400">
            Đã hủy
          </span>
        );
    }
  };

  const handleSoftDelete = (id) => {
    if (window.confirm("Xóa mềm phiếu nhập này?")) {
      setSoftDeletedIds((prev) =>
        prev.includes(id) ? prev : [...prev, id]
      );

      alert(`Đã xóa mềm phiếu #${id}`);
    }
  };

  const handleRestore = (id) => {
    setSoftDeletedIds((prev) =>
      prev.filter((item) => item !== id)
    );

    alert(`Đã khôi phục phiếu #${id}`);
  };

  return (
    <div className="text-zinc-200">

      {/* FILTER */}
      <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 mb-8">

        <div className="flex flex-wrap gap-4 items-center">

          <div className="flex-1 min-w-[280px] relative">

            <Search
              size={18}
              className="absolute left-4 top-3.5 text-zinc-500"
            />

            <input
              type="text"
              placeholder="Tìm mã phiếu, kho nhập, mã NCC..."
              value={search}
              onChange={(e) =>
                setSearch(e.target.value)
              }
              className="w-full pl-11 pr-4 py-3 bg-zinc-800 border border-zinc-700 rounded-2xl focus:outline-none focus:border-amber-400 text-white placeholder-zinc-500"
            />
          </div>

          <select
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value)
            }
            className="px-4 py-3 bg-zinc-800 border border-zinc-700 rounded-2xl text-white focus:outline-none focus:border-amber-400"
          >
            <option value="">
              Tất cả trạng thái
            </option>

            <option value="pending">
              Chờ duyệt
            </option>

            <option value="processing">
              Đang nhập kho
            </option>

            <option value="completed">
              Hoàn thành
            </option>

            <option value="cancelled">
              Đã hủy
            </option>
          </select>

          <button
            onClick={resetFilters}
            className="px-6 py-3 bg-blue-500 hover:bg-blue-600 rounded-2xl transition flex items-center gap-2 font-medium"
          >
            <RotateCcw size={16} />
            Reset
          </button>

          <button
            onClick={() =>
              alert("Mở form tạo phiếu nhập")
            }
            className="px-6 py-3 bg-amber-500 hover:bg-amber-600 rounded-2xl transition flex items-center gap-2 font-medium"
          >
            <Plus size={16} />
            Tạo phiếu nhập
          </button>

        </div>
      </div>

      {/* STATISTICS */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-6 mb-8">

        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800">

          <div className="flex items-center justify-between">

            <div>
              <p className="text-zinc-400 text-sm">
                Tổng phiếu nhập
              </p>

              <p className="text-3xl font-bold text-white mt-1">
                842
              </p>
            </div>

            <div className="w-12 h-12 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center">
              <PackagePlus size={24} />
            </div>

          </div>

          <p className="text-emerald-400 text-sm mt-3">
            ↑ 14% so với tháng trước
          </p>

        </div>

        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800">

          <div className="flex items-center justify-between">

            <div>
              <p className="text-zinc-400 text-sm">
                Phiếu hôm nay
              </p>

              <p className="text-3xl font-bold text-white mt-1">
                24
              </p>
            </div>

            <div className="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
              <Clock3 size={24} />
            </div>

          </div>

          <p className="text-emerald-400 text-sm mt-3">
            ↑ 5 phiếu so với hôm qua
          </p>

        </div>

        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800">

          <div className="flex items-center justify-between">

            <div>
              <p className="text-zinc-400 text-sm">
                Tổng tiền nhập
              </p>

              <p className="text-3xl font-bold text-white mt-1">
                528M
              </p>
            </div>

            <div className="w-12 h-12 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center">
              <DollarSign size={24} />
            </div>

          </div>

          <p className="text-emerald-400 text-sm mt-3">
            ↑ 21% so với tuần trước
          </p>

        </div>

        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800">

          <div className="flex items-center justify-between">

            <div>
              <p className="text-zinc-400 text-sm">
                Phiếu chờ duyệt
              </p>

              <p className="text-3xl font-bold text-red-400 mt-1">
                13
              </p>
            </div>

            <div className="w-12 h-12 rounded-xl bg-red-500/10 text-red-400 flex items-center justify-center">
              <AlertTriangle size={24} />
            </div>

          </div>

          <p className="text-red-400 text-sm mt-3">
            Cần xử lý gấp
          </p>

        </div>

      </div>

      {/* TABLE */}
      <div className="bg-zinc-900 rounded-3xl border border-zinc-800 overflow-hidden">

        <div className="overflow-x-auto">

          <table className="w-full">

            <thead className="bg-zinc-950 border-b border-zinc-800">

              <tr>

                <th className="px-6 py-4 text-left text-sm text-zinc-400">
                  Mã phiếu nhập
                </th>

                <th className="px-6 py-4 text-left text-sm text-zinc-400">
                  Mã NCC
                </th>

                <th className="px-6 py-4 text-left text-sm text-zinc-400">
                  Kho nhập
                </th>

                <th className="px-6 py-4 text-left text-sm text-zinc-400">
                  NV nhận hàng
                </th>

                <th className="px-6 py-4 text-left text-sm text-zinc-400">
                  NV duyệt
                </th>

                <th className="px-6 py-4 text-left text-sm text-zinc-400">
                  Ngày nhập kho
                </th>

                <th className="px-6 py-4 text-center text-sm text-zinc-400">
                  Trạng thái
                </th>

                <th className="px-6 py-4 text-center text-sm text-zinc-400">
                  SL nhập
                </th>

                <th className="px-6 py-4 text-right text-sm text-zinc-400">
                  Tổng tiền
                </th>

                <th className="px-6 py-4 text-center text-sm text-zinc-400">
                  Hành động
                </th>

              </tr>

            </thead>

            <tbody className="divide-y divide-zinc-800">

              {pagedReceipts.map((receipt) => (
                <tr
                  key={receipt.id}
                  className="hover:bg-zinc-800 transition"
                >

                  <td className="px-6 py-4 font-medium text-white">
                    {receipt.code}
                  </td>

                  <td className="px-6 py-4">
                    {receipt.supplierCode}
                  </td>

                  <td className="px-6 py-4">
                    <span className="px-3 py-1 rounded-xl bg-purple-500/20 text-purple-300 text-sm">
                      {receipt.warehouse.name}
                    </span>
                  </td>

                  <td className="px-6 py-4">
                    {receipt.receiver}
                  </td>

                  <td className="px-6 py-4">
                    {receipt.approver}
                  </td>

                  <td className="px-6 py-4">
                    {receipt.importDate}
                  </td>

                  <td className="px-6 py-4 text-center">
                    {getStatusBadge(receipt.status)}
                  </td>

                  <td className="px-6 py-4 text-center font-medium">
                    {receipt.totalQuantity}
                  </td>

                  <td className="px-6 py-4 text-right font-semibold text-white">
                    {receipt.totalAmount.toLocaleString(
                      "vi-VN"
                    )} đ
                  </td>

                  <td className="px-6 py-4">

                    <div className="flex items-center justify-center gap-3">

                      <button
                        onClick={() =>
                          setSelectedReceipt(receipt)
                        }
                        className="text-blue-400 hover:text-blue-300"
                      >
                        <Eye size={18} />
                      </button>

                      {!softDeletedIds.includes(receipt.id) && (
                        <>
                          <button
                            onClick={() =>
                              alert(
                                `Đang xử lý phiếu #${receipt.id}`
                              )
                            }
                            className="text-emerald-400 hover:text-emerald-300"
                          >
                            <Settings size={18} />
                          </button>

                          <button
                            onClick={() =>
                              window.confirm(
                                "Xác nhận hủy phiếu nhập?"
                              ) &&
                              alert(
                                `Đã hủy phiếu #${receipt.id}`
                              )
                            }
                            className="text-orange-400 hover:text-orange-300"
                          >
                            <Ban size={18} />
                          </button>
                        </>
                      )}

                      <button
                        onClick={() =>
                          softDeletedIds.includes(receipt.id)
                            ? handleRestore(receipt.id)
                            : handleSoftDelete(receipt.id)
                        }
                        className={
                          softDeletedIds.includes(receipt.id)
                            ? "text-emerald-400 hover:text-emerald-300"
                            : "text-red-400 hover:text-red-300"
                        }
                      >
                        {softDeletedIds.includes(receipt.id) ? (
                          <RotateCcw size={18} />
                        ) : (
                          <Trash2 size={18} />
                        )}
                      </button>

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

      {/* MODAL */}
      {selectedReceipt && (
        <div className="fixed inset-0 bg-black/80 z-50 flex items-start justify-center p-4 overflow-y-auto">

          <div className="w-full max-w-2xl mt-10 bg-zinc-900 rounded-2xl border border-zinc-700 shadow-2xl">

            <div className="p-6 border-b border-zinc-700 flex items-center justify-between">

              <h3 className="text-xl font-semibold text-white">
                Phiếu nhập #{selectedReceipt.code}
              </h3>

              <button
                onClick={() =>
                  setSelectedReceipt(null)
                }
                className="text-gray-400 hover:text-white"
              >
                <X size={24} />
              </button>

            </div>

            <div className="p-6">

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-gray-300">

                <p>
                  <strong>Mã NCC:</strong>{" "}
                  {selectedReceipt.supplierCode}
                </p>

                <p>
                  <strong>Kho nhập:</strong>{" "}
                  {selectedReceipt.warehouse.name}
                </p>

                <p>
                  <strong>NV nhận hàng:</strong>{" "}
                  {selectedReceipt.receiver}
                </p>

                <p>
                  <strong>NV duyệt:</strong>{" "}
                  {selectedReceipt.approver}
                </p>

                <p>
                  <strong>Ngày nhập kho:</strong>{" "}
                  {selectedReceipt.importDate}
                </p>

                <p>
                  <strong>Ngày tạo phiếu:</strong>{" "}
                  {selectedReceipt.createdDate}
                </p>

                <p>
                  <strong>Số lượng nhập:</strong>{" "}
                  {selectedReceipt.totalQuantity}
                </p>

                <div className="col-span-full">
                  <strong>Trạng thái:</strong>{" "}
                  {getStatusBadge(
                    selectedReceipt.status
                  )}
                </div>

                <div className="col-span-full pt-4">

                  <p className="text-2xl font-bold text-right text-white">
                    Tổng tiền:{" "}
                    {selectedReceipt.totalAmount.toLocaleString(
                      "vi-VN"
                    )} đ
                  </p>

                </div>

              </div>

            </div>

          </div>

        </div>
      )}

    </div>
  );
}