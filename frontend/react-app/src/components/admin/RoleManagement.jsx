import Button from "../common/Button";
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
import RoleDialog from "./Role/RoleDialog";

const PAGE_SIZE = 5;

const permissionGroups = [
  {
    id: "pg-users", code: "USER_MANAGEMENT", name: "Quản lý người dùng", description: "Tài khoản, trạng thái và xác thực người dùng.",
    permissions: [
      { id: "p-user-view", code: "USER_VIEW", name: "Xem người dùng", description: "Xem danh sách và chi tiết người dùng." },
      { id: "p-user-create", code: "USER_CREATE", name: "Tạo người dùng", description: "Tạo tài khoản người dùng mới." },
      { id: "p-user-update", code: "USER_UPDATE", name: "Cập nhật người dùng", description: "Sửa hồ sơ và trạng thái tài khoản." },
      { id: "p-user-lock", code: "USER_LOCK", name: "Khóa tài khoản", description: "Khóa hoặc mở khóa tài khoản." },
    ],
  },
  {
    id: "pg-orders", code: "ORDER_MANAGEMENT", name: "Quản lý đơn hàng", description: "Xử lý vòng đời đơn hàng.",
    permissions: [
      { id: "p-order-view", code: "ORDER_VIEW", name: "Xem đơn hàng" },
      { id: "p-order-create", code: "ORDER_CREATE", name: "Tạo đơn hàng" },
      { id: "p-order-update", code: "ORDER_UPDATE", name: "Cập nhật đơn hàng" },
      { id: "p-order-cancel", code: "ORDER_CANCEL", name: "Hủy đơn hàng" },
    ],
  },
  {
    id: "pg-inventory", code: "INVENTORY", name: "Kho vận", description: "Nhập, xuất và kiểm soát tồn kho.",
    permissions: [
      { id: "p-stock-view", code: "INVENTORY_VIEW", name: "Xem tồn kho" },
      { id: "p-stock-import", code: "INVENTORY_IMPORT", name: "Nhập kho" },
      { id: "p-stock-export", code: "INVENTORY_EXPORT", name: "Xuất kho" },
      { id: "p-stock-adjust", code: "INVENTORY_ADJUST", name: "Điều chỉnh tồn kho" },
    ],
  },
  {
    id: "pg-rbac", code: "AUTHORIZATION", name: "Phân quyền", description: "Vai trò, quyền và lịch sử thay đổi.",
    permissions: [
      { id: "p-role-view", code: "ROLE_VIEW", name: "Xem vai trò" },
      { id: "p-role-manage", code: "ROLE_MANAGE", name: "Quản lý vai trò" },
      { id: "p-permission-assign", code: "PERMISSION_ASSIGN", name: "Gán quyền" },
      { id: "p-audit-view", code: "AUTH_AUDIT_VIEW", name: "Xem log phân quyền" },
    ],
  },
];

const authUsers = [
  { id: "u-1", name: "Nguyễn Văn Admin", email: "admin@lunaria.vn", phone: "0901000001", active: true, locked: false },
  { id: "u-2", name: "Trần Minh Anh", email: "manager@lunaria.vn", phone: "0901000002", active: true, locked: false },
  { id: "u-3", name: "Lê Hoàng Nam", email: "warehouse@lunaria.vn", phone: "0901000003", active: true, locked: false },
  { id: "u-4", name: "Phạm Thu Hà", email: "staff@lunaria.vn", phone: "0901000004", active: true, locked: false },
  { id: "u-5", name: "Vũ Minh Khoa", email: "locked@lunaria.vn", phone: "0901000005", active: false, locked: true },
];

const initialRoles = [
  { id: 1, code: "ADMIN", name: "Quản trị viên", description: "Toàn quyền quản lý hệ thống", created_at: "01/06/2026 08:00", user_ids: ["u-1"], permission_ids: permissionGroups.flatMap((group) => group.permissions.map((item) => item.id)), direct_user_permissions: {}, audit_entries: [{ id: "a-1", permission_id: "p-permission-assign", action: "ADD", changed_by: "admin@lunaria.vn", changed_at: "12/08/2026 09:30" }] },
  { id: 2, code: "MANAGER", name: "Quản lý", description: "Quản lý nhân viên và cửa hàng", created_at: "02/06/2026 09:00", user_ids: ["u-2"], permission_ids: ["p-user-view", "p-order-view", "p-order-update", "p-stock-view"], direct_user_permissions: {} },
  { id: 3, code: "STAFF", name: "Nhân viên", description: "Thao tác bán hàng và xử lý đơn", created_at: "03/06/2026 10:00", user_ids: ["u-4"], permission_ids: ["p-order-view", "p-order-create", "p-order-update"], direct_user_permissions: {} },
  { id: 4, code: "WAREHOUSE", name: "Nhân viên kho", description: "Quản lý nhập xuất kho", created_at: "04/06/2026 10:00", user_ids: ["u-3"], permission_ids: ["p-stock-view", "p-stock-import", "p-stock-export"], direct_user_permissions: {} },
  { id: 5, code: "ACCOUNTANT", name: "Kế toán", description: "Quản lý thanh toán và doanh thu", created_at: "05/06/2026 11:00", user_ids: [], permission_ids: ["p-order-view", "p-audit-view"], direct_user_permissions: {} },
];

export default function RoleManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [roles, setRoles] = useState(initialRoles);
  const [dialog, setDialog] = useState({ open: false, mode: "view", role: null });

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
    [roles, searchTerm]
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

  const openDialog = (mode, role = null) => setDialog({ open: true, mode, role });
  const closeDialog = () => setDialog((current) => ({ ...current, open: false }));
  const saveRole = (payload) => {
    if (dialog.mode === "create") {
      setRoles((current) => [...current, { ...payload, id: crypto.randomUUID(), created_at: new Date().toLocaleString("vi-VN"), audit_entries: [] }]);
    } else {
      setRoles((current) => current.map((role) => role.id === payload.id ? { ...role, ...payload } : role));
    }
    closeDialog();
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

          <Button
            onClick={resetFilters}
            className="bg-blue-500 hover:bg-blue-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <RotateCcw size={18} />
            <span>Reset</span>
          </Button>

          <Button
            onClick={() => openDialog("create")}
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm vai trò</span>
          </Button>
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
            Danh sách vai trò
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
                      <Button
                        onClick={() =>
                          openDialog("view", role)
                        }
                        className="text-blue-400 hover:text-blue-300 transition-colors"
                        title="Xem"
                      >
                        <Eye size={18} />
                      </Button>

                      {/* EDIT */}
                      <Button
                        onClick={() =>
                          openDialog("edit", role)
                        }
                        className="text-amber-400 hover:text-amber-300 transition-colors"
                        title="Sửa"
                      >
                        <PenSquare size={18} />
                      </Button>
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

      <RoleDialog
        open={dialog.open}
        mode={dialog.mode}
        role={dialog.role}
        users={authUsers}
        permissionGroups={permissionGroups}
        auditEntries={dialog.role?.audit_entries || []}
        onClose={closeDialog}
        onSave={saveRole}
      />
    </div>
  );
}
