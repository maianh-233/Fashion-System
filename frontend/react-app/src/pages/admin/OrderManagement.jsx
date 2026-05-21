import { useEffect, useMemo, useState } from "react";
import {
  Search,
  Eye,
  Settings,
  Ban,
  Trash2,
  RotateCcw,
  ShoppingBag,
  Clock3,
  DollarSign,
  AlertTriangle,
  X,
  Plus,
  MessageCircle,
} from "lucide-react";
import Pagination from "../../components/admin/Pagination";
import OrderChatDialog from "../../components/admin/OrderChatDialog";

const PAGE_SIZE = 4;

export default function OrderManagement() {
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [paymentFilter, setPaymentFilter] = useState("");
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [chatOrder, setChatOrder] = useState(null);
  const [chatMessage, setChatMessage] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [softDeletedIds, setSoftDeletedIds] = useState([2, 5]);

  const ordersData = [
    { id: 1, date: "2026-05-21 14:32", code: "DH240521001", customer: "Nguyễn Văn An", type: "ONLINE", status: "processing", payment: "paid", amount: 1250000, hasCustomerChat: true, unreadMessages: 2 },
    { id: 2, date: "2026-05-21 11:15", code: "DH240521002", customer: "Trần Thị Bích", type: "PICKUP", status: "pending", payment: "unpaid", amount: 850000, hasCustomerChat: false, unreadMessages: 0 },
    { id: 3, date: "2026-05-20 17:45", code: "DH240520015", customer: "Lê Hoàng Nam", type: "OFFLINE", status: "completed", payment: "paid", amount: 2450000, hasCustomerChat: false, unreadMessages: 0 },
    { id: 4, date: "2026-05-20 09:20", code: "DH240520014", customer: "Phạm Minh Quân", type: "ONLINE", status: "shipped", payment: "partial", amount: 980000, hasCustomerChat: true, unreadMessages: 1 },
    { id: 5, date: "2026-05-19 20:10", code: "DH240519028", customer: "Hoàng Thị Lan", type: "PICKUP", status: "cancelled", payment: "unpaid", amount: 670000, hasCustomerChat: false, unreadMessages: 0 },
  ];

  const filteredOrders = useMemo(
    () =>
      ordersData.filter((order) => {
        const matchSearch =
          order.code.toLowerCase().includes(search.toLowerCase()) ||
          order.customer.toLowerCase().includes(search.toLowerCase());
        const matchType = !typeFilter || order.type === typeFilter;
        const matchStatus = !statusFilter || order.status === statusFilter;
        const matchPayment = !paymentFilter || order.payment === paymentFilter;
        return matchSearch && matchType && matchStatus && matchPayment;
      }),
    [search, typeFilter, statusFilter, paymentFilter]
  );

  const totalPages = Math.max(1, Math.ceil(filteredOrders.length / PAGE_SIZE));
  const pagedOrders = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredOrders.slice(start, start + PAGE_SIZE);
  }, [filteredOrders, currentPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [search, typeFilter, statusFilter, paymentFilter]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const resetFilters = () => {
    setSearch("");
    setTypeFilter("");
    setStatusFilter("");
    setPaymentFilter("");
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case "pending":
        return <span className="px-3 py-1 rounded-full text-sm font-medium bg-yellow-500/20 text-yellow-400">Chờ xử lý</span>;
      case "processing":
        return <span className="px-3 py-1 rounded-full text-sm font-medium bg-blue-500/20 text-blue-400">Đang xử lý</span>;
      case "shipped":
        return <span className="px-3 py-1 rounded-full text-sm font-medium bg-purple-500/20 text-purple-400">Đã giao</span>;
      case "completed":
        return <span className="px-3 py-1 rounded-full text-sm font-medium bg-emerald-500/20 text-emerald-400">Hoàn thành</span>;
      default:
        return <span className="px-3 py-1 rounded-full text-sm font-medium bg-red-500/20 text-red-400">Đã hủy</span>;
    }
  };

  const getPaymentBadge = (payment) => {
    switch (payment) {
      case "paid":
        return <span className="px-3 py-1 rounded-full text-sm font-medium bg-emerald-500/20 text-emerald-400">Đã TT</span>;
      case "unpaid":
        return <span className="px-3 py-1 rounded-full text-sm font-medium bg-red-500/20 text-red-400">Chưa TT</span>;
      default:
        return <span className="px-3 py-1 rounded-full text-sm font-medium bg-orange-500/20 text-orange-400">TT một phần</span>;
    }
  };

  const handleSoftDelete = (id) => {
    if (window.confirm("Xóa mềm đơn hàng này?")) {
      setSoftDeletedIds((prev) =>
        prev.includes(id) ? prev : [...prev, id]
      );
      alert(`Đã xóa mềm đơn #${id}`);
    }
  };

  const handleRestore = (id) => {
    setSoftDeletedIds((prev) => prev.filter((item) => item !== id));
    alert(`Đã khôi phục đơn #${id}`);
  };

  const chatHistory = useMemo(
    () => ({
      DH240521001: [
        { id: "m1", sender: "customer", content: "Shop ơi đơn em đang giao đến đâu rồi ạ?" },
        { id: "m2", sender: "staff", content: "Shop đã bàn giao cho đơn vị vận chuyển, dự kiến chiều nay giao ạ." },
      ],
      DH240520014: [
        { id: "m1", sender: "customer", content: "Mình muốn đổi sang size L được không?" },
      ],
    }),
    []
  );

  const handleSendChat = () => {
    if (!chatMessage.trim()) {
      return;
    }
    alert(`Đã gửi tin nhắn cho đơn ${chatOrder.code}: ${chatMessage}`);
    setChatMessage("");
  };

  return (
    <div className="text-zinc-200">
      <div className="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 mb-8">
        <div className="flex flex-wrap gap-4 items-center">
          <div className="flex-1 min-w-[280px] relative">
            <Search size={18} className="absolute left-4 top-3.5 text-zinc-500" />
            <input
              type="text"
              placeholder="Tìm theo mã đơn, tên khách hàng..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-11 pr-4 py-3 bg-zinc-800 border border-zinc-700 rounded-2xl focus:outline-none focus:border-amber-400 text-white placeholder-zinc-500"
            />
          </div>

          <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} className="px-4 py-3 bg-zinc-800 border border-zinc-700 rounded-2xl text-white focus:outline-none focus:border-amber-400">
            <option value="">Tất cả loại đơn</option>
            <option value="ONLINE">ONLINE</option>
            <option value="OFFLINE">OFFLINE</option>
            <option value="PICKUP">PICKUP</option>
          </select>

          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="px-4 py-3 bg-zinc-800 border border-zinc-700 rounded-2xl text-white focus:outline-none focus:border-amber-400">
            <option value="">Tất cả trạng thái</option>
            <option value="pending">Chờ xử lý</option>
            <option value="processing">Đang xử lý</option>
            <option value="shipped">Đã giao</option>
            <option value="completed">Hoàn thành</option>
            <option value="cancelled">Đã hủy</option>
          </select>

          <select value={paymentFilter} onChange={(e) => setPaymentFilter(e.target.value)} className="px-4 py-3 bg-zinc-800 border border-zinc-700 rounded-2xl text-white focus:outline-none focus:border-amber-400">
            <option value="">Tất cả thanh toán</option>
            <option value="paid">Đã thanh toán</option>
            <option value="unpaid">Chưa thanh toán</option>
            <option value="partial">Thanh toán một phần</option>
          </select>

          <button onClick={resetFilters} className="px-6 py-3 bg-blue-500 hover:bg-blue-600 rounded-2xl transition flex items-center gap-2 font-medium">
            <RotateCcw size={16} />
            Reset
          </button>

          <button onClick={() => alert("Mở form tạo đơn hàng mua tại cửa hàng")} className="px-6 py-3 bg-amber-500 hover:bg-amber-600 rounded-2xl transition flex items-center gap-2 font-medium">
            <Plus size={16} />
            Thêm đơn hàng
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-6 mb-8">
        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800"><div className="flex items-center justify-between"><div><p className="text-zinc-400 text-sm">Tổng đơn hàng</p><p className="text-3xl font-bold text-white mt-1">1,284</p></div><div className="w-12 h-12 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center"><ShoppingBag size={24} /></div></div><p className="text-emerald-400 text-sm mt-3">↑ 12% so với tháng trước</p></div>
        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800"><div className="flex items-center justify-between"><div><p className="text-zinc-400 text-sm">Đơn hôm nay</p><p className="text-3xl font-bold text-white mt-1">47</p></div><div className="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center"><Clock3 size={24} /></div></div><p className="text-emerald-400 text-sm mt-3">↑ 8 đơn so với hôm qua</p></div>
        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800"><div className="flex items-center justify-between"><div><p className="text-zinc-400 text-sm">Doanh thu hôm nay</p><p className="text-3xl font-bold text-white mt-1">248.5M</p></div><div className="w-12 h-12 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center"><DollarSign size={24} /></div></div><p className="text-emerald-400 text-sm mt-3">↑ 18% so với hôm qua</p></div>
        <div className="bg-zinc-900 p-6 rounded-3xl border border-zinc-800"><div className="flex items-center justify-between"><div><p className="text-zinc-400 text-sm">Đơn chưa xử lý</p><p className="text-3xl font-bold text-red-400 mt-1">23</p></div><div className="w-12 h-12 rounded-xl bg-red-500/10 text-red-400 flex items-center justify-center"><AlertTriangle size={24} /></div></div><p className="text-red-400 text-sm mt-3">Cần xử lý gấp</p></div>
      </div>

      <div className="bg-zinc-900 rounded-3xl border border-zinc-800 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-zinc-950 border-b border-zinc-800">
              <tr>
                <th className="px-6 py-4 text-left text-sm text-zinc-400">Ngày tạo</th>
                <th className="px-6 py-4 text-left text-sm text-zinc-400">Mã đơn</th>
                <th className="px-6 py-4 text-left text-sm text-zinc-400">Khách hàng</th>
                <th className="px-6 py-4 text-left text-sm text-zinc-400">Loại</th>
                <th className="px-6 py-4 text-left text-sm text-zinc-400">Trạng thái</th>
                <th className="px-6 py-4 text-left text-sm text-zinc-400">Thanh toán</th>
                <th className="px-6 py-4 text-center text-sm text-zinc-400">Xóa mềm</th>
                <th className="px-6 py-4 text-right text-sm text-zinc-400">Tổng tiền</th>
                <th className="px-6 py-4 text-center text-sm text-zinc-400">Hành động</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-zinc-800">
              {pagedOrders.map((order) => (
                <tr key={order.id} className="hover:bg-zinc-800 transition">
                  <td className="px-6 py-4 whitespace-nowrap">{order.date}</td>
                  <td className="px-6 py-4 font-medium text-white">{order.code}</td>
                  <td className="px-6 py-4">{order.customer}</td>
                  <td className="px-6 py-4"><span className="font-medium">{order.type}</span></td>
                  <td className="px-6 py-4">{getStatusBadge(order.status)}</td>
                  <td className="px-6 py-4">{getPaymentBadge(order.payment)}</td>
                  <td className="px-6 py-4 text-center">
                    {softDeletedIds.includes(order.id) ? (
                      <span className="px-3 py-1 rounded-full text-xs font-medium bg-red-500/20 text-red-400">
                        Đã xóa mềm
                      </span>
                    ) : (
                      <span className="px-3 py-1 rounded-full text-xs font-medium bg-emerald-500/20 text-emerald-400">
                        Bình thường
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 text-right font-semibold text-white">{order.amount.toLocaleString("vi-VN")} đ</td>
                  <td className="px-6 py-4">
                    <div className="flex items-center justify-center gap-3">
                      <button onClick={() => setSelectedOrder(order)} className="text-blue-400 hover:text-blue-300"><Eye size={18} /></button>
                      {order.type === "ONLINE" && order.hasCustomerChat && (
                        <button
                          onClick={() => setChatOrder(order)}
                          className="text-cyan-400 hover:text-cyan-300 relative"
                          title={`Khách đã chat về đơn (${order.unreadMessages} chưa đọc)`}
                        >
                          <MessageCircle size={18} />
                          {order.unreadMessages > 0 && (
                            <span className="absolute -top-2 -right-2 min-w-4 h-4 px-1 rounded-full bg-rose-500 text-[10px] leading-4 text-white text-center">
                              {order.unreadMessages}
                            </span>
                          )}
                        </button>
                      )}
                      {!softDeletedIds.includes(order.id) && (
                        <>
                          <button onClick={() => alert(`Đang xử lý đơn hàng #${order.id}`)} className="text-emerald-400 hover:text-emerald-300"><Settings size={18} /></button>
                          <button onClick={() => window.confirm("Xác nhận hủy đơn hàng?") && alert(`Đã hủy đơn #${order.id}`)} className="text-orange-400 hover:text-orange-300"><Ban size={18} /></button>
                        </>
                      )}
                      {softDeletedIds.includes(order.id) ? (
                        <button onClick={() => handleRestore(order.id)} className="text-emerald-400 hover:text-emerald-300" title="Khôi phục"><RotateCcw size={18} /></button>
                      ) : (
                        <button onClick={() => handleSoftDelete(order.id)} className="text-red-400 hover:text-red-300" title="Xóa mềm"><Trash2 size={18} /></button>
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

      {selectedOrder && (
        <div className="fixed inset-0 bg-black/80 z-50 flex items-start justify-center p-4 overflow-y-auto">
          <div className="w-full max-w-2xl mt-10 bg-zinc-900 rounded-2xl border border-zinc-700 shadow-2xl">
            <div className="p-6 border-b border-zinc-700 flex items-center justify-between">
              <h3 className="text-xl font-semibold text-white">Đơn hàng #{selectedOrder.code}</h3>
              <button onClick={() => setSelectedOrder(null)} className="text-gray-400 hover:text-white"><X size={24} /></button>
            </div>

            <div className="p-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-gray-300">
                <p><strong>Khách hàng:</strong> {selectedOrder.customer}</p>
                <p><strong>Loại:</strong> {selectedOrder.type}</p>
                <p><strong>Ngày tạo:</strong> {selectedOrder.date}</p>
                <p><strong>Trạng thái:</strong> {selectedOrder.status}</p>
                <p><strong>Thanh toán:</strong> {selectedOrder.payment}</p>
                <div className="col-span-full pt-4">
                  <p className="text-2xl font-bold text-right text-white">Tổng tiền: {selectedOrder.amount.toLocaleString("vi-VN")} đ</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      <OrderChatDialog
        order={chatOrder}
        chatHistory={chatHistory}
        chatMessage={chatMessage}
        onChatMessageChange={setChatMessage}
        onSend={handleSendChat}
        onClose={() => setChatOrder(null)}
      />
    </div>
  );
}
