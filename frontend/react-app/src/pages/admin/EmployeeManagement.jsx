import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Plus,
  Eye,
  Settings,
  Ban,
  Trash2,
  RotateCcw,
  Lock,
  Unlock,
  Users,
  CircleCheck,
  UserPlus,
} from "lucide-react";
import Pagination from "../../components/common/Pagination";
import EmployeeDialog from "../../components/admin/Empolyee/EmployeeDialog";

const PAGE_SIZE = 4;

export default function EmployeeManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState("view"); // view | edit | create
  const [selectedEmployee, setSelectedEmployee] = useState(null);

  const openCreateDialog = () => {
  setSelectedEmployee(null);
  setDialogMode("create");
  setDialogOpen(true);
};

const openViewDialog = (employee) => {
  setSelectedEmployee(employee);
  setDialogMode("view");
  setDialogOpen(true);
};

  const employees = [
    { id: 1, name: "Nguyễn Văn An", email: "an.nguyen@lunaria.vn", phone: "0912 345 678", status: "active", locked: false, role: "Quản lý" },
    { id: 2, name: "Trần Thị Linh", email: "linh.tran@lunaria.vn", phone: "0987 654 321", status: "active", locked: false, role: "Nhân viên bán hàng" },
    { id: 3, name: "Lê Minh Quân", email: "quan.le@lunaria.vn", phone: "0934 567 890", status: "deleted", locked: false, role: "Nhân viên kho" },
    { id: 4, name: "Phạm Ngọc Bích", email: "bich.pham@lunaria.vn", phone: "0901 234 567", status: "active", locked: true, role: "Kế toán" },
    { id: 5, name: "Hoàng Thị Mai", email: "mai.hoang@lunaria.vn", phone: "0978 123 456", status: "active", locked: false, role: "Nhân viên bán hàng" },
  ];

  const filteredEmployees = useMemo(
    () =>
      employees.filter((emp) => {
        const matchSearch =
          emp.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
          emp.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
          emp.phone.includes(searchTerm);
        const matchRole = !roleFilter || emp.role === roleFilter;
        const matchStatus = !statusFilter || emp.status === statusFilter;
        return matchSearch && matchRole && matchStatus;
      }),
    [searchTerm, roleFilter, statusFilter]
  );

  const totalPages = Math.max(1, Math.ceil(filteredEmployees.length / PAGE_SIZE));
  const pagedEmployees = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredEmployees.slice(start, start + PAGE_SIZE);
  }, [filteredEmployees, currentPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, roleFilter, statusFilter]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const resetFilters = () => {
    setSearchTerm("");
    setRoleFilter("");
    setStatusFilter("");
  };

  return (
    <div>
      <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 mb-8">
        <div className="flex flex-wrap items-center gap-4">
          <div className="relative">
            <input
              type="text"
              placeholder="Tìm theo tên, email hoặc số điện thoại..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="bg-zinc-800 border border-zinc-700 focus:border-amber-400 rounded-2xl py-3 pl-11 pr-4 w-96 max-w-full focus:outline-none text-sm"
            />
            <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" />
          </div>

          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
            className="bg-zinc-800 border border-zinc-700 rounded-2xl py-3 px-5 focus:outline-none focus:border-amber-400"
          >
            <option value="">Tất cả vai trò</option>
            <option value="Quản lý">Quản lý</option>
            <option value="Nhân viên bán hàng">Nhân viên bán hàng</option>
            <option value="Nhân viên kho">Nhân viên kho</option>
            <option value="Kế toán">Kế toán</option>
          </select>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-zinc-800 border border-zinc-700 rounded-2xl py-3 px-5 focus:outline-none focus:border-amber-400"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="active">Hoạt động</option>
            <option value="deleted">Đã xóa</option>
          </select>

          <button
            onClick={resetFilters}
            className="bg-blue-500 hover:bg-blue-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <RotateCcw size={18} />
            <span>Reset</span>
          </button>

          <button
            onClick={openCreateDialog}
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm nhân viên</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800"><div className="flex justify-between items-start"><div><p className="text-zinc-400">Tổng nhân viên</p><p className="text-4xl font-bold mt-2">48</p></div><Users size={40} className="text-blue-400" /></div></div>
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800"><div className="flex justify-between items-start"><div><p className="text-zinc-400">Đang hoạt động</p><p className="text-4xl font-bold mt-2 text-emerald-400">42</p></div><CircleCheck size={40} className="text-emerald-400" /></div></div>
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800"><div className="flex justify-between items-start"><div><p className="text-zinc-400">Bị khóa</p><p className="text-4xl font-bold mt-2 text-red-400">3</p></div><Lock size={40} className="text-red-400" /></div></div>
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800"><div className="flex justify-between items-start"><div><p className="text-zinc-400">Nhân viên mới</p><p className="text-4xl font-bold mt-2">7</p></div><UserPlus size={40} className="text-amber-400" /></div></div>
      </div>

      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">Danh sách nhân viên</h3>
          <p className="text-sm text-zinc-400">Tìm thấy:<span className="font-medium text-white ml-1">{filteredEmployees.length}</span></p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-6 font-normal">Họ tên</th>
                <th className="text-left py-5 px-6 font-normal">Email</th>
                <th className="text-left py-5 px-6 font-normal">SĐT</th>
                <th className="text-center py-5 px-6 font-normal">Trạng thái</th>
                <th className="text-center py-5 px-6 font-normal">Khóa</th>
                <th className="text-center py-5 px-6 font-normal">Role</th>
                <th className="text-center py-5 px-6 font-normal w-40">Hành động</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedEmployees.map((emp) => (
                <tr key={emp.id} className="hover:bg-zinc-800 transition-colors">
                  <td className="px-6 py-5 font-medium">{emp.name}</td>
                  <td className="px-6 py-5 text-zinc-300">{emp.email}</td>
                  <td className="px-6 py-5">{emp.phone}</td>
                  <td className="px-6 py-5 text-center">
                    {emp.status === "active" ? <span className="bg-emerald-500/20 text-emerald-400 px-4 py-1 rounded-full text-xs">Hoạt động</span> : <span className="bg-red-500/20 text-red-400 px-4 py-1 rounded-full text-xs">Đã xóa</span>}
                  </td>
                  <td className="px-6 py-5 text-center">
                    {emp.locked ? <span className="bg-red-500/20 text-red-400 px-4 py-1 rounded-full text-xs">Đã khóa</span> : <span className="bg-emerald-500/20 text-emerald-400 px-4 py-1 rounded-full text-xs">Bình thường</span>}
                  </td>
                  <td className="px-6 py-5 text-center"><span className="bg-zinc-700 text-white px-4 py-1 rounded-full text-xs">{emp.role}</span></td>
                  <td className="px-6 py-5">
                    <div className="flex items-center justify-center gap-3">
                      <button onClick={() => alert(`Xem chi tiết nhân viên ID: ${emp.id}`)} className="text-blue-400 hover:text-blue-300 transition-colors" title="Xem"><Eye size={18} /></button>
                      <button onClick={() => alert(`Đang xử lý nhân viên ID: ${emp.id}`)} className="text-emerald-400 hover:text-emerald-300 transition-colors" title="Xử lý"><Settings size={18} /></button>
                      <button onClick={() => alert(`Tạm dừng tài khoản nhân viên ID: ${emp.id}`)} className="text-orange-400 hover:text-orange-300 transition-colors" title="Tạm dừng"><Ban size={18} /></button>
                      {emp.status === "active" ? (
                        <button onClick={() => confirm("Xóa mềm nhân viên này?") && alert(`Đã xóa mềm nhân viên ID ${emp.id}`)} className="text-red-400 hover:text-red-300 transition-colors" title="Xóa mềm"><Trash2 size={18} /></button>
                      ) : (
                        <button onClick={() => alert(`Đã khôi phục nhân viên ID ${emp.id}`)} className="text-emerald-400 hover:text-emerald-300 transition-colors" title="Khôi phục"><RotateCcw size={18} /></button>
                      )}
                      {emp.locked ? (
                        <button onClick={() => alert(`Đã thay đổi trạng thái khóa của nhân viên ID ${emp.id}`)} className="text-emerald-400 hover:text-emerald-300 transition-colors" title="Mở khóa"><Unlock size={18} /></button>
                      ) : (
                        <button onClick={() => alert(`Đã thay đổi trạng thái khóa của nhân viên ID ${emp.id}`)} className="text-red-400 hover:text-red-300 transition-colors" title="Khóa"><Lock size={18} /></button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} />
      </div>

      {dialogOpen && (
        <EmployeeDialog
          mode={dialogMode}
          employee={selectedEmployee}
          onClose={() => setDialogOpen(false)}
          onSave={(data) => {
            console.log("SAVE:", data);
            setDialogOpen(false);
          }}
          onDelete={() => {
            if (confirm("Xóa nhân viên này?")) {
              console.log("DELETE:", selectedEmployee);
              setDialogOpen(false);
            }
          }}
        />
      )}
    </div>
  );
}
