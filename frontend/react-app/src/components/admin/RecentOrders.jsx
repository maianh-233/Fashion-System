import { Link } from "react-router-dom";
import { money } from "../../pages/admin/dashboardLogic";
import { ORDER_STATUS_LABELS, ORDER_STATUS_STYLES } from "../../pages/admin/orderManagementLogic";

/** 5 đơn mới nhất của phạm vi đang xem; {@code showStore} khi xem toàn hệ thống. */
export default function RecentOrders({ orders = [], showStore = false }) {
  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <div className="flex justify-between mb-4">
        <h3 className="font-semibold">
          Đơn hàng gần đây
        </h3>

        <Link to="/admin/orders" className="text-amber-400 text-sm hover:underline">
          Xem tất cả →
        </Link>
      </div>

      {orders.length === 0 ? (
        <p className="text-sm text-zinc-400">Chưa có đơn hàng.</p>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-zinc-700 text-zinc-400">
              <th className="py-3 text-left">Mã đơn</th>
              <th className="py-3 text-left">Khách</th>
              <th className="py-3 text-left">Giá trị</th>
              <th className="py-3 text-center">Trạng thái</th>
            </tr>
          </thead>

          <tbody className="divide-y divide-zinc-800">
            {orders.map((order) => (
              <tr key={order.id}>
                <td className="py-4">
                  {order.orderCode}
                  {showStore && <p className="text-xs text-zinc-500">{order.storeName}</p>}
                </td>

                <td className="py-4">
                  {order.customerName || "Khách lẻ"}
                </td>

                <td className="py-4 font-medium">
                  {money(order.totalAmount)}
                </td>

                <td className="py-4 text-center">
                  <span
                    className={`px-4 py-1 rounded-full text-xs ${ORDER_STATUS_STYLES[order.status] || ""}`}
                  >
                    {ORDER_STATUS_LABELS[order.status] || order.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
