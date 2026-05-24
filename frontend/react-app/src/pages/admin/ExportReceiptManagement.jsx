import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Eye,
  Settings,
  Ban,
  Trash2,
  RotateCcw,
  PackageMinus,
  Clock3,
  DollarSign,
  AlertTriangle,
  X,
  Plus,
} from "lucide-react";

import Pagination from "../../components/common/Pagination";

const PAGE_SIZE = 4;

export default function ExportReceiptManagement() {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [exportTypeFilter, setExportTypeFilter] = useState("");
  const [selectedReceipt, setSelectedReceipt] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [softDeletedIds, setSoftDeletedIds] = useState([2, 5]);

  const exportReceiptsData = [
    {
      id: 1,
      code: "PX240521001",
      exportType: "Xuất bán hàng",
      receiver: "Nguyễn Văn An",
      approver: "Trần Minh Quân",
      exportDate: "2026-05-21",
      warehouseName: "Kho trung tâm Quận 1",
      status: "completed",
      totalQuantity: 85,
      totalAmount: 32500000,
    },
    {
      id: 2,
      code: "PX240521002",
      exportType: "Xuất chuyển kho",
      receiver: "Lê Thị Mai",
      approver: "Phạm Quốc Bảo",
      exportDate: "2026-05-21",
      warehouseName: "Kho chi nhánh Quận 7",
      status: "pending",
      totalQuantity: 48,
      totalAmount: 12800000,
    },
    {
      id: 3,
      code: "PX240520015",
      exportType: "Xuất trả NCC",
      receiver: "Trần Văn Long",
      approver: "Nguyễn Minh Đức",
      exportDate: "2026-05-20",
      warehouseName: "Kho online Thủ Đức",
      status: "processing",
      totalQuantity: 92,
      totalAmount: 18600000,
    },
    {
      id: 4,
      code: "PX240520020",
      exportType: "Xuất bán hàng",
      receiver: "Hoàng Quốc Anh",
      approver: "Phạm Minh Khôi",
      exportDate: "2026-05-20",
      warehouseName: "Kho outlet Tân Bình",
      status: "cancelled",
      totalQuantity: 35,
      totalAmount: 7200000,
    },
    {
      id: 5,
      code: "PX240519008",
      exportType: "Xuất hủy",
      receiver: "Lê Quốc Huy",
      approver: "Nguyễn Minh Nhật",
      exportDate: "2026-05-19",
      warehouseName: "Kho trung tâm Quận 1",
      status: "completed",
      totalQuantity: 210,
      totalAmount: 52800000,
    },
  ];

  const filteredReceipts = useMemo(() => {
    return exportReceiptsData.filter((receipt) => {
      const matchSearch =
        receipt.code
          .toLowerCase()
          .includes(search.toLowerCase()) ||
        receipt.receiver
          .toLowerCase()
          .includes(search.toLowerCase()) ||
        receipt.approver
          .toLowerCase()
          .includes(search.toLowerCase());

      const matchStatus =
        !statusFilter || receipt.status === statusFilter;

      const matchExportType =
        !exportTypeFilter ||
        receipt.exportType === exportTypeFilter;

      return (
        matchSearch &&
        matchStatus &&
        matchExportType
      );
    });
  }, [search, statusFilter, exportTypeFilter]);

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
  }, [search, statusFilter, exportTypeFilter]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const resetFilters = () => {
    setSearch("");
    setStatusFilter("");
    setExportTypeFilter("");
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
            Đang xuất kho
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

  const getExportTypeBadge = (type) => {
    switch (type) {
      case "Xuất bán hàng":
        return (
          <span className="px-3 py-1 rounded-full text-xs font-medium bg-emerald-500/20 text-emerald-400">
            Xuất bán
          </span>
        );

      case "Xuất chuyển kho":
        return (
          <span className="px-3 py-1 rounded-full text-xs font-medium bg-blue-500/20 text-blue-400">
            Chuyển kho
          </span>
        );

      case "Xuất trả NCC":
        return (
          <span className="px-3 py-1 rounded-full text-xs font-medium bg-orange-500/20 text-orange-400">
            Trả NCC
          </span>
        );

      default:
        return (
          <span className="px-3 py-1 rounded-full text-xs font-medium bg-red-500/20 text-red-400">
            Xuất hủy
          </span>
        );
    }
  };

  const handleSoftDelete = (id) => {
    if (window.confirm("Xóa mềm phiếu xuất này?")) {
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
              placeholder="Tìm mã phiếu, nhân viên..."
              value={search}
              onChange={(e) =>
                setSearch(e.target.value)
              }
              className="w-full pl-11 pr-4 py-3 bg-zinc-800 border border-zinc-700 rounded-2xl focus:outline-none focus:border-amber-400 text-white placeholder-zinc-500"
            />

          </div>

          <select
            value={exportTypeFilter}
            onChange={(e) =>
              setExportTypeFilter(e.target.value)
            }
            className="px-4 py-3 bg-zinc-800 border border-zinc-700 rounded-2xl text-white focus:outline-none focus:border-amber-400"
          >
            <option value="">
              Tất cả loại phiếu
            </option>

            <option value="Xuất bán hàng">
              Xuất bán hàng
            </option>

            <option value="Xuất chuyển kho">
              Xuất chuyển kho
            </option>

            <option value="Xuất trả NCC">
              Xuất trả NCC
            </option>

            <option value="Xuất hủy">
              Xuất hủy
            </option>

          </select>

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
              Đang xuất kho
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
              alert("Mở form tạo phiếu xuất")
            }
            className="px-6 py-3 bg-amber-500 hover:bg-amber-600 rounded-2xl transition flex items-center gap-2 font-medium"
          >
            <Plus size={16} />
            Tạo phiếu xuất
          </button>

        </div>
      </div>

      {/* STATISTICS */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-6 mb-8">

        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800">

          <div className="flex items-center justify-between">

            <div>
              <p className="text-zinc-400 text-sm">
                Tổng phiếu xuất
              </p>

              <p className="text-3xl font-bold text-white mt-1">
                625
              </p>
            </div>

            <div className="w-12 h-12 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center">
              <PackageMinus size={24} />
            </div>

          </div>

          <p className="text-emerald-400 text-sm mt-3">
            ↑ 11% so với tháng trước
          </p>

        </div>

        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800">

          <div className="flex items-center justify-between">

            <div>
              <p className="text-zinc-400 text-sm">
                Phiếu hôm nay
              </p>

              <p className="text-3xl font-bold text-white mt-1">
                18
              </p>
            </div>

            <div className="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
              <Clock3 size={24} />
            </div>

          </div>

          <p className="text-emerald-400 text-sm mt-3">
            ↑ 3 phiếu so với hôm qua
          </p>

        </div>

        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800">

          <div className="flex items-center justify-between">

            <div>
              <p className="text-zinc-400 text-sm">
                Tổng giá trị xuất
              </p>

              <p className="text-3xl font-bold text-white mt-1">
                412M
              </p>
            </div>

            <div className="w-12 h-12 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center">
              <DollarSign size={24} />
            </div>

          </div>

          <p className="text-emerald-400 text-sm mt-3">
            ↑ 16% so với tuần trước
          </p>

        </div>

        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800">

          <div className="flex items-center justify-between">

            <div>
              <p className="text-zinc-400 text-sm">
                Phiếu chờ duyệt
              </p>

              <p className="text-3xl font-bold text-red-400 mt-1">
                9
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
                  Mã phiếu xuất
                </th>

                <th className="px-6 py-4 text-left text-sm text-zinc-400">
                  Loại phiếu xuất
                </th>

                <th className="px-6 py-4 text-left text-sm text-zinc-400">
                  NV nhận hàng
                </th>

                <th className="px-6 py-4 text-left text-sm text-zinc-400">
                  NV duyệt
                </th>

                <th className="px-6 py-4 text-left text-sm text-zinc-400">
                  Ngày xuất kho
                </th>

                <th className="px-6 py-4 text-left text-sm text-zinc-400">
                  Tên kho xuất
                </th>

                <th className="px-6 py-4 text-center text-sm text-zinc-400">
                  Trạng thái
                </th>

                <th className="px-6 py-4 text-center text-sm text-zinc-400">
                  Xóa mềm
                </th>

                <th className="px-6 py-4 text-center text-sm text-zinc-400">
                  SL xuất
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
                    {getExportTypeBadge(receipt.exportType)}
                  </td>

                  <td className="px-6 py-4">
                    {receipt.receiver}
                  </td>

                  <td className="px-6 py-4">
                    {receipt.approver}
                  </td>

                  <td className="px-6 py-4">
                    {receipt.exportDate}
                  </td>

                  <td className="px-6 py-4">
                    {receipt.warehouseName}
                  </td>

                  <td className="px-6 py-4 text-center">
                    {getStatusBadge(receipt.status)}
                  </td>

                  <td className="px-6 py-4 text-center">

                    {softDeletedIds.includes(receipt.id) ? (
                      <span className="px-3 py-1 rounded-full text-xs font-medium bg-red-500/20 text-red-400">
                        Đã xóa mềm
                      </span>
                    ) : (
                      <span className="px-3 py-1 rounded-full text-xs font-medium bg-emerald-500/20 text-emerald-400">
                        Bình thường
                      </span>
                    )}

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
                                "Xác nhận hủy phiếu xuất?"
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
                Phiếu xuất #{selectedReceipt.code}
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
                  <strong>Loại phiếu:</strong>{" "}
                  {selectedReceipt.exportType}
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
                  <strong>Ngày xuất kho:</strong>{" "}
                  {selectedReceipt.exportDate}
                </p>

                <p>
                  <strong>Tên kho xuất:</strong>{" "}
                  {selectedReceipt.warehouseName}
                </p>

                <p>
                  <strong>Số lượng xuất:</strong>{" "}
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
