import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  PenSquare,
  Ban,
  Trash2,
  RotateCcw,
  Building2,
  CircleCheck,
  Truck,
} from "lucide-react";
import Pagination from "../../components/admin/Pagination";

const PAGE_SIZE = 4;

export default function SupplierManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const suppliers = [
    {
      id: 1,
      code: "NCC001",
      name: "Công ty TNHH Fashion Star",
      contact: "Nguyễn Minh Khoa",
      email: "fashionstar@gmail.com",
      phone: "0912 345 678",
      status: "active",
    },
    {
      id: 2,
      code: "NCC002",
      name: "Luxury Textile",
      contact: "Trần Hoài Nam",
      email: "luxurytextile@gmail.com",
      phone: "0987 654 321",
      status: "inactive",
    },
    {
      id: 3,
      code: "NCC003",
      name: "Moonlight Garment",
      contact: "Lê Thanh Tùng",
      email: "moonlight@gmail.com",
      phone: "0934 567 890",
      status: "deleted",
    },
    {
      id: 4,
      code: "NCC004",
      name: "Elegant Fabric",
      contact: "Phạm Thị Ly",
      email: "elegantfabric@gmail.com",
      phone: "0901 234 567",
      status: "active",
    },
    {
      id: 5,
      code: "NCC005",
      name: "Aurora Clothing",
      contact: "Hoàng Nhật Anh",
      email: "aurora@gmail.com",
      phone: "0978 123 456",
      status: "active",
    },
  ];

  const filteredSuppliers = useMemo(
    () =>
      suppliers.filter((supplier) => {
        const matchSearch =
          supplier.name
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          supplier.code
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          supplier.contact
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          supplier.email
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          supplier.phone.includes(searchTerm);

        const matchStatus =
          !statusFilter || supplier.status === statusFilter;

        return matchSearch && matchStatus;
      }),
    [searchTerm, statusFilter]
  );

  const totalPages = Math.max(
    1,
    Math.ceil(filteredSuppliers.length / PAGE_SIZE)
  );

  const pagedSuppliers = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredSuppliers.slice(start, start + PAGE_SIZE);
  }, [filteredSuppliers, currentPage]);

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
    setStatusFilter("");
  };

  return (
    <div>
      {/* FILTER */}
      <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 mb-8">
        <div className="flex flex-wrap items-center gap-4">
          <div className="relative">
            <input
              type="text"
              placeholder="Tìm theo mã NCC, tên, người liên hệ..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="bg-zinc-800 border border-zinc-700 focus:border-amber-400 rounded-2xl py-3 pl-11 pr-4 w-96 max-w-full focus:outline-none text-sm"
            />

            <Search
              size={18}
              className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"
            />
          </div>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-zinc-800 border border-zinc-700 rounded-2xl py-3 px-5 focus:outline-none focus:border-amber-400"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="active">Đang hợp tác</option>
            <option value="inactive">Ngừng hợp tác</option>
            <option value="deleted">Đã xóa mềm</option>
          </select>

          <button
            onClick={resetFilters}
            className="bg-blue-500 hover:bg-blue-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <RotateCcw size={18} />
            <span>Reset</span>
          </button>

          <button
            onClick={() => alert("Mở form thêm nhà cung cấp")}
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm Nhà Cung Cấp</span>
          </button>
        </div>
      </div>

      {/* STATS */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Tổng NCC</p>
              <p className="text-4xl font-bold mt-2">25</p>
            </div>

            <Building2 size={40} className="text-blue-400" />
          </div>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Đang hợp tác</p>
              <p className="text-4xl font-bold mt-2 text-emerald-400">
                20
              </p>
            </div>

            <CircleCheck size={40} className="text-emerald-400" />
          </div>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Ngừng hợp tác</p>
              <p className="text-4xl font-bold mt-2 text-orange-400">
                3
              </p>
            </div>

            <Ban size={40} className="text-orange-400" />
          </div>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">NCC mới</p>
              <p className="text-4xl font-bold mt-2">5</p>
            </div>

            <Truck size={40} className="text-amber-400" />
          </div>
        </div>
      </div>

      {/* TABLE */}
      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">
            Danh sách nhà cung cấp
          </h3>

          <p className="text-sm text-zinc-400">
            Tìm thấy:
            <span className="font-medium text-white ml-1">
              {filteredSuppliers.length}
            </span>
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-6 font-normal">
                  Mã NCC
                </th>

                <th className="text-left py-5 px-6 font-normal">
                  Tên NCC
                </th>

                <th className="text-left py-5 px-6 font-normal">
                  Người liên hệ
                </th>

                <th className="text-left py-5 px-6 font-normal">
                  Email
                </th>

                <th className="text-left py-5 px-6 font-normal">
                  SĐT
                </th>

                <th className="text-center py-5 px-6 font-normal">
                  Trạng thái
                </th>

                <th className="text-center py-5 px-6 font-normal w-52">
                  Action
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedSuppliers.map((supplier) => (
                <tr
                  key={supplier.id}
                  className="hover:bg-zinc-800 transition-colors"
                >
                  <td className="px-6 py-5 font-medium text-amber-400">
                    {supplier.code}
                  </td>

                  <td className="px-6 py-5 font-medium">
                    {supplier.name}
                  </td>

                  <td className="px-6 py-5">
                    {supplier.contact}
                  </td>

                  <td className="px-6 py-5 text-zinc-300">
                    {supplier.email}
                  </td>

                  <td className="px-6 py-5">
                    {supplier.phone}
                  </td>

                  <td className="px-6 py-5 text-center">
                    {supplier.status === "active" && (
                      <span className="bg-emerald-500/20 text-emerald-400 px-4 py-1 rounded-full text-xs">
                        Đang hợp tác
                      </span>
                    )}

                    {supplier.status === "inactive" && (
                      <span className="bg-orange-500/20 text-orange-400 px-4 py-1 rounded-full text-xs">
                        Ngừng hợp tác
                      </span>
                    )}

                    {supplier.status === "deleted" && (
                      <span className="bg-red-500/20 text-red-400 px-4 py-1 rounded-full text-xs">
                        Đã xóa mềm
                      </span>
                    )}
                  </td>

                  <td className="px-6 py-5">
                    <div className="flex items-center justify-center gap-3">
                      {/* VIEW */}
                      <button
                        onClick={() =>
                          alert(
                            `Xem chi tiết nhà cung cấp ID: ${supplier.id}`
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
                            `Sửa nhà cung cấp ID: ${supplier.id}`
                          )
                        }
                        className="text-amber-400 hover:text-amber-300 transition-colors"
                        title="Sửa"
                      >
                        <PenSquare size={18} />
                      </button>

                      {/* SOFT DELETE / RESTORE */}
                      {supplier.status === "deleted" ? (
                        <button
                          onClick={() =>
                            alert(
                              `Khôi phục nhà cung cấp ID ${supplier.id}`
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
                            confirm("Xóa mềm nhà cung cấp này?") &&
                            alert(
                              `Đã xóa mềm nhà cung cấp ID ${supplier.id}`
                            )
                          }
                          className="text-red-400 hover:text-red-300 transition-colors"
                          title="Xóa mềm"
                        >
                          <Trash2 size={18} />
                        </button>
                      )}

                      {/* STOP COOPERATION */}
                      {supplier.status !== "deleted" && (
                        <button
                          onClick={() =>
                            alert(
                              `Đã cập nhật trạng thái hợp tác NCC ID ${supplier.id}`
                            )
                          }
                          className="text-orange-400 hover:text-orange-300 transition-colors"
                          title="Không hợp tác nữa"
                        >
                          <Ban size={18} />
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