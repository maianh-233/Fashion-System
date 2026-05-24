import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  PenSquare,
  ShieldCheck,
  CircleCheck,
  KeyRound,
  Layers3,
  RotateCcw,
} from "lucide-react";
import Pagination from "../common/Pagination";

const PAGE_SIZE = 5;

export default function RoleManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const roles = [
    {
      id: 1,
      code: "ROLE_ADMIN",
      name: "Quản trị viên",
      description: "Toàn quyền quản lý hệ thống",
    },
    {
      id: 2,
      code: "ROLE_MANAGER",
      name: "Quản lý",
      description: "Quản lý nhân viên và cửa hàng",
    },
    {
      id: 3,
      code: "ROLE_STAFF",
      name: "Nhân viên",
      description: "Thao tác bán hàng và xử lý đơn",
    },
    {
      id: 4,
      code: "ROLE_WAREHOUSE",
      name: "Nhân viên kho",
      description: "Quản lý nhập xuất kho",
    },
    {
      id: 5,
      code: "ROLE_ACCOUNTANT",
      name: "Kế toán",
      description: "Quản lý thanh toán và doanh thu",
    },
  ];

  const filteredRoles = useMemo(
    () =>
      roles.filter((role) => {
        return (
          role.code
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          role.name
            .toLowerCase()
            .includes(searchTerm.toLowerCase()) ||
          role.description
            .toLowerCase()
            .includes(searchTerm.toLowerCase())
        );
      }),
    [searchTerm]
  );

  const totalPages = Math.max(
    1,
    Math.ceil(filteredRoles.length / PAGE_SIZE)
  );

  const pagedRoles = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredRoles.slice(start, start + PAGE_SIZE);
  }, [filteredRoles, currentPage]);

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
              placeholder="Tìm theo code, tên quyền..."
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
            onClick={() => alert("Mở form thêm quyền")}
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm quyền</span>
          </button>
        </div>
      </div>

      {/* STATS */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Tổng quyền</p>
              <p className="text-4xl font-bold mt-2">12</p>
            </div>

            <ShieldCheck size={40} className="text-blue-400" />
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
              <p className="text-zinc-400">Phân quyền</p>
              <p className="text-4xl font-bold mt-2 text-amber-400">
                35
              </p>
            </div>

            <KeyRound size={40} className="text-amber-400" />
          </div>
        </div>

        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-zinc-400">Nhóm quyền</p>
              <p className="text-4xl font-bold mt-2 text-purple-400">
                6
              </p>
            </div>

            <Layers3 size={40} className="text-purple-400" />
          </div>
        </div>
      </div>

      {/* TABLE */}
      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">
            Danh sách quyền
          </h3>

          <p className="text-sm text-zinc-400">
            Tìm thấy:
            <span className="font-medium text-white ml-1">
              {filteredRoles.length}
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
                  Tên quyền
                </th>

                <th className="text-left py-5 px-6 font-normal">
                  Mô tả
                </th>

                <th className="text-center py-5 px-6 font-normal w-40">
                  Action
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedRoles.map((role) => (
                <tr
                  key={role.id}
                  className="hover:bg-zinc-800 transition-colors"
                >
                  <td className="px-6 py-5 font-medium text-amber-400">
                    {role.code}
                  </td>

                  <td className="px-6 py-5 font-medium">
                    {role.name}
                  </td>

                  <td className="px-6 py-5 text-zinc-300">
                    {role.description}
                  </td>

                  <td className="px-6 py-5">
                    <div className="flex items-center justify-center gap-4">
                      {/* VIEW */}
                      <button
                        onClick={() =>
                          alert(`Xem chi tiết quyền ID: ${role.id}`)
                        }
                        className="text-blue-400 hover:text-blue-300 transition-colors"
                        title="Xem"
                      >
                        <Eye size={18} />
                      </button>

                      {/* EDIT */}
                      <button
                        onClick={() =>
                          alert(`Sửa quyền ID: ${role.id}`)
                        }
                        className="text-amber-400 hover:text-amber-300 transition-colors"
                        title="Sửa"
                      >
                        <PenSquare size={18} />
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
    </div>
  );
}