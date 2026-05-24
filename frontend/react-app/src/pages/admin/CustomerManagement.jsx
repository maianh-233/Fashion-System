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

const PAGE_SIZE = 4;

export default function CustomerManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [rankFilter, setRankFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const customers = [
    { id: 1, name: "Nguyễn Văn An", avatar: "https://i.pravatar.cc/150?img=1", email: "an.nguyen@gmail.com", phone: "0912 345 678", status: "active", locked: false, rank: "VIP", totalOrders: 24, totalSpent: "18.500.000đ" },
    { id: 2, name: "Trần Thị Linh", avatar: "https://i.pravatar.cc/150?img=5", email: "linh.tran@gmail.com", phone: "0987 654 321", status: "active", locked: false, rank: "Gold", totalOrders: 12, totalSpent: "7.200.000đ" },
    { id: 3, name: "Lê Minh Quân", avatar: "https://i.pravatar.cc/150?img=8", email: "quan.le@gmail.com", phone: "0934 567 890", status: "deleted", locked: false, rank: "Silver", totalOrders: 2, totalSpent: "950.000đ" },
    { id: 4, name: "Phạm Ngọc Bích", avatar: "https://i.pravatar.cc/150?img=9", email: "bich.pham@gmail.com", phone: "0901 234 567", status: "active", locked: true, rank: "VIP", totalOrders: 41, totalSpent: "32.400.000đ" },
    { id: 5, name: "Hoàng Thị Mai", avatar: "https://i.pravatar.cc/150?img=15", email: "mai.hoang@gmail.com", phone: "0978 123 456", status: "active", locked: false, rank: "Gold", totalOrders: 8, totalSpent: "4.100.000đ" },
  ];

  const filteredCustomers = useMemo(
    () =>
      customers.filter((cus) => {
        const matchSearch =
          cus.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
          cus.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
          cus.phone.includes(searchTerm);
        const matchRank = !rankFilter || cus.rank === rankFilter;
        const matchStatus = !statusFilter || cus.status === statusFilter;
        return matchSearch && matchRank && matchStatus;
      }),
    [searchTerm, rankFilter, statusFilter]
  );

  const totalPages = Math.max(1, Math.ceil(filteredCustomers.length / PAGE_SIZE));
  const pagedCustomers = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredCustomers.slice(start, start + PAGE_SIZE);
  }, [filteredCustomers, currentPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, rankFilter, statusFilter]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const resetFilters = () => {
    setSearchTerm("");
    setRankFilter("");
    setStatusFilter("");
  };

  const getRankStyle = (rank) => {
    switch (rank) {
      case "VIP":
        return "bg-purple-500/20 text-purple-400";
      case "Gold":
        return "bg-amber-500/20 text-amber-400";
      case "Silver":
        return "bg-zinc-500/20 text-zinc-300";
      default:
        return "bg-zinc-700 text-white";
    }
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
            value={rankFilter}
            onChange={(e) => setRankFilter(e.target.value)}
            className="bg-zinc-800 border border-zinc-700 rounded-2xl py-3 px-5 focus:outline-none focus:border-amber-400"
          >
            <option value="">Tất cả hạng khách</option>
            <option value="VIP">VIP</option>
            <option value="Gold">Gold</option>
            <option value="Silver">Silver</option>
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
            onClick={() => alert("Mở form thêm khách hàng mới")}
            className="bg-amber-500 hover:bg-amber-600 px-6 py-3 rounded-2xl flex items-center gap-2 font-medium transition-colors"
          >
            <Plus size={18} />
            <span>Thêm khách hàng</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800"><div className="flex justify-between items-start"><div><p className="text-zinc-400">Tổng khách hàng</p><p className="text-4xl font-bold mt-2">128</p></div><Users size={40} className="text-blue-400" /></div></div>
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800"><div className="flex justify-between items-start"><div><p className="text-zinc-400">Khách VIP</p><p className="text-4xl font-bold mt-2 text-purple-400">24</p></div><CircleCheck size={40} className="text-purple-400" /></div></div>
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800"><div className="flex justify-between items-start"><div><p className="text-zinc-400">Bị khóa</p><p className="text-4xl font-bold mt-2 text-red-400">5</p></div><Lock size={40} className="text-red-400" /></div></div>
        <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800"><div className="flex justify-between items-start"><div><p className="text-zinc-400">Khách mới</p><p className="text-4xl font-bold mt-2">16</p></div><UserPlus size={40} className="text-amber-400" /></div></div>
      </div>

      <div className="bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800">
        <div className="p-6 border-b border-zinc-800 flex justify-between items-center bg-zinc-950">
          <h3 className="font-semibold text-lg">Danh sách khách hàng</h3>
          <p className="text-sm text-zinc-400">Tìm thấy:<span className="font-medium text-white ml-1">{filteredCustomers.length}</span></p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-sm">
                <th className="text-left py-5 px-6 font-normal">Khách hàng</th>
                <th className="text-left py-5 px-6 font-normal">Email</th>
                <th className="text-left py-5 px-6 font-normal">SĐT</th>
                <th className="text-center py-5 px-6 font-normal">Hạng khách</th>
                <th className="text-center py-5 px-6 font-normal">Đơn hàng</th>
                <th className="text-center py-5 px-6 font-normal">Tổng chi tiêu</th>
                <th className="text-center py-5 px-6 font-normal">Trạng thái</th>
                <th className="text-center py-5 px-6 font-normal">Khóa</th>
                <th className="text-center py-5 px-6 font-normal w-40">Hành động</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800 text-sm">
              {pagedCustomers.map((cus) => (
                <tr key={cus.id} className="hover:bg-zinc-800 transition-colors">
                  <td className="px-6 py-5">
                    <div className="flex items-center gap-4">
                      <img src={cus.avatar} alt={cus.name} className="w-12 h-12 rounded-full object-cover border border-zinc-700" />
                      <p className="font-medium text-white">{cus.name}</p>
                    </div>
                  </td>
                  <td className="px-6 py-5 text-zinc-300">{cus.email}</td>
                  <td className="px-6 py-5">{cus.phone}</td>
                  <td className="px-6 py-5 text-center"><span className={`px-4 py-1 rounded-full text-xs font-medium ${getRankStyle(cus.rank)}`}>{cus.rank}</span></td>
                  <td className="px-6 py-5 text-center">{cus.totalOrders}</td>
                  <td className="px-6 py-5 text-center text-amber-400 font-medium">{cus.totalSpent}</td>
                  <td className="px-6 py-5 text-center">
                    {cus.status === "active" ? <span className="bg-emerald-500/20 text-emerald-400 px-4 py-1 rounded-full text-xs">Hoạt động</span> : <span className="bg-red-500/20 text-red-400 px-4 py-1 rounded-full text-xs">Đã xóa</span>}
                  </td>
                  <td className="px-6 py-5 text-center">
                    {cus.locked ? <span className="bg-red-500/20 text-red-400 px-4 py-1 rounded-full text-xs">Đã khóa</span> : <span className="bg-emerald-500/20 text-emerald-400 px-4 py-1 rounded-full text-xs">Bình thường</span>}
                  </td>
                  <td className="px-6 py-5">
                    <div className="flex items-center justify-center gap-3">
                      <button onClick={() => alert(`Xem chi tiết khách hàng ID: ${cus.id}`)} className="text-blue-400 hover:text-blue-300 transition-colors"><Eye size={18} /></button>
                      <button onClick={() => alert(`Đang xử lý khách hàng ID: ${cus.id}`)} className="text-emerald-400 hover:text-emerald-300 transition-colors"><Settings size={18} /></button>
                      <button onClick={() => alert(`Cảnh báo tài khoản khách hàng ID: ${cus.id}`)} className="text-orange-400 hover:text-orange-300 transition-colors"><Ban size={18} /></button>
                      {cus.status === "active" ? (
                        <button onClick={() => confirm("Xóa mềm khách hàng này?") && alert(`Đã xóa mềm khách hàng ID ${cus.id}`)} className="text-red-400 hover:text-red-300 transition-colors"><Trash2 size={18} /></button>
                      ) : (
                        <button onClick={() => alert(`Đã khôi phục khách hàng ID ${cus.id}`)} className="text-emerald-400 hover:text-emerald-300 transition-colors"><RotateCcw size={18} /></button>
                      )}
                      {cus.locked ? (
                        <button onClick={() => alert(`Đã thay đổi trạng thái khóa khách hàng ID ${cus.id}`)} className="text-emerald-400 hover:text-emerald-300 transition-colors"><Unlock size={18} /></button>
                      ) : (
                        <button onClick={() => alert(`Đã thay đổi trạng thái khóa khách hàng ID ${cus.id}`)} className="text-red-400 hover:text-red-300 transition-colors"><Lock size={18} /></button>
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
    </div>
  );
}
