import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  PenSquare,
  Trash2,
  RotateCcw,
  Store,
  CircleCheck,
  MapPin,
  Users,
} from "lucide-react";
import Pagination from "../../components/admin/Pagination";

const PAGE_SIZE = 4;

export default function StoreManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const stores = [
    {
      id: 1,
      code: "CH001",
      name: "LUNARIA Quận 1",
      phone: "0912 345 678",
      address: "12 Nguyễn Huệ, Quận 1, TP.HCM",
      employees: 15,
      status: "active",
    },
    {
      id: 2,
      code: "CH002",
      name: "LUNARIA Gò Vấp",
      phone: "0987 654 321",
      address: "45 Phan Văn Trị, Gò Vấp, TP.HCM",
      employees: 10,
      status: "active",
    },
    {
      id: 3,
      code: "CH003",
      name: "LUNARIA Bình Thạnh",
      phone: "0934 567 890",
      address: "88 Xô Viết Nghệ Tĩnh, Bình Thạnh, TP.HCM",
      employees: 8,
      status: "deleted",
    },
    {
      id: 4,
      code: "CH004",
      name: "LUNARIA Thủ Đức",
      phone: "0901 234 567",
      address: "120 Võ Văn Ngân, Thủ Đức, TP.HCM",
      employees: 12,
      status: "active",
    },
    {
      id: 5,
      code: "CH005",
      name: "LUNARIA Tân Bình",
      phone: "0978 123 456",
      address: "66 Cộng Hòa, Tân Bình, TP.HCM",
      employees: 9,
      status: "inactive",
    },
  ];

  const filteredStores = useMemo(
    () =>
      stores.filter((store) => {
        const matchSearch =
          store.name
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          store.code
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          store.phone.includes(searchTerm) ||
          store.address
            .toLowerCase()
            .includes(searchTerm.toLowerCase());

        const matchStatus =
          !statusFilter || store.status === statusFilter;

        return matchSearch && matchStatus;
      }),
    [searchTerm, statusFilter]
  );

  const totalPages = Math.max(
    1,
    Math.ceil(filteredStores.length / PAGE_SIZE)
  );

  const pagedStores = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredStores.slice(start, start + PAGE_SIZE);
  }, [filteredStores, currentPage]);

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
              placeholder="Tìm theo mã cửa hàng, tên, địa chỉ..."
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
            <option value="active">Đang hoạt động</option>
            <option value="inactive">Tạm ngưng</option>
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
            onClick={() => alert("Mở form thêm cửa hàng")}
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm Cửa Hàng</span>
          </button>
        </div>
      </div>

      {/* STATS */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Tổng cửa hàng</p>
              <p className="text-4xl font-bold mt-2">12</p>
            </div>

            <Store size={40} className="text-blue-400" />
          </div>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Đang hoạt động</p>
              <p className="text-4xl font-bold mt-2 text-emerald-400">
                9
              </p>
            </div>

            <CircleCheck size={40} className="text-emerald-400" />
          </div>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Tạm ngưng</p>
              <p className="text-4xl font-bold mt-2 text-orange-400">
                2
              </p>
            </div>

            <MapPin size={40} className="text-orange-400" />
          </div>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Tổng nhân viên</p>
              <p className="text-4xl font-bold mt-2">54</p>
            </div>

            <Users size={40} className="text-amber-400" />
          </div>
        </div>
      </div>

      {/* TABLE */}
      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">
            Danh sách cửa hàng
          </h3>

          <p className="text-sm text-zinc-400">
            Tìm thấy:
            <span className="font-medium text-white ml-1">
              {filteredStores.length}
            </span>
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-6 font-normal">
                  Code cửa hàng
                </th>

                <th className="text-left py-5 px-6 font-normal">
                  Tên cửa hàng
                </th>

                <th className="text-left py-5 px-6 font-normal">
                  Phone
                </th>

                <th className="text-center py-5 px-6 font-normal">
                  Trạng thái
                </th>

                <th className="text-left py-5 px-6 font-normal">
                  Địa chỉ
                </th>

                <th className="text-center py-5 px-6 font-normal">
                  Số nhân viên
                </th>

                <th className="text-center py-5 px-6 font-normal w-52">
                  Action
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedStores.map((store) => (
                <tr
                  key={store.id}
                  className="hover:bg-zinc-800 transition-colors"
                >
                  <td className="px-6 py-5 font-medium text-amber-400">
                    {store.code}
                  </td>

                  <td className="px-6 py-5 font-medium">
                    {store.name}
                  </td>

                  <td className="px-6 py-5">
                    {store.phone}
                  </td>

                  <td className="px-6 py-5 text-center">
                    {store.status === "active" && (
                      <span className="bg-emerald-500/20 text-emerald-400 px-4 py-1 rounded-full text-xs">
                        Hoạt động
                      </span>
                    )}

                    {store.status === "inactive" && (
                      <span className="bg-orange-500/20 text-orange-400 px-4 py-1 rounded-full text-xs">
                        Tạm ngưng
                      </span>
                    )}

                    {store.status === "deleted" && (
                      <span className="bg-red-500/20 text-red-400 px-4 py-1 rounded-full text-xs">
                        Đã xóa mềm
                      </span>
                    )}
                  </td>

                  <td className="px-6 py-5 text-zinc-300">
                    {store.address}
                  </td>

                  <td className="px-6 py-5 text-center font-medium">
                    {store.employees}
                  </td>

                  <td className="px-6 py-5">
                    <div className="flex items-center justify-center gap-3">
                      {/* VIEW */}
                      <button
                        onClick={() =>
                          alert(
                            `Xem chi tiết cửa hàng ID: ${store.id}`
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
                            `Sửa cửa hàng ID: ${store.id}`
                          )
                        }
                        className="text-amber-400 hover:text-amber-300 transition-colors"
                        title="Sửa"
                      >
                        <PenSquare size={18} />
                      </button>

                      {/* DELETE / RESTORE */}
                      {store.status === "deleted" ? (
                        <button
                          onClick={() =>
                            alert(
                              `Khôi phục cửa hàng ID ${store.id}`
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
                            confirm("Xóa mềm cửa hàng này?") &&
                            alert(
                              `Đã xóa mềm cửa hàng ID ${store.id}`
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