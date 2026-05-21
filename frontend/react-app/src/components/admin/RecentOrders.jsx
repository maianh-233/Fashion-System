export default function RecentOrders() {
  const orders = [
    {
      id: "#LN2405128",
      customer: "Trần Thị Lan Anh",
      total: "2.850.000 đ",
      status: "Hoàn thành",
      color: "text-emerald-400 bg-emerald-500/20",
    },
    {
      id: "#LN2405127",
      customer: "Lê Minh Quân",
      total: "1.690.000 đ",
      status: "Đang giao",
      color: "text-amber-400 bg-amber-500/20",
    },
    {
      id: "#LN2405126",
      customer: "Phạm Ngọc Bích",
      total: "3.450.000 đ",
      status: "Đã xác nhận",
      color: "text-blue-400 bg-blue-500/20",
    },
  ];

  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <div className="flex justify-between mb-4">
        <h3 className="font-semibold">
          Đơn hàng gần đây
        </h3>

        <button className="text-amber-400 text-sm hover:underline">
          Xem tất cả →
        </button>
      </div>

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
          {orders.map((order, index) => (
            <tr key={index}>
              <td className="py-4">{order.id}</td>

              <td className="py-4">
                {order.customer}
              </td>

              <td className="py-4 font-medium">
                {order.total}
              </td>

              <td className="py-4 text-center">
                <span
                  className={`px-4 py-1 rounded-full text-xs ${order.color}`}
                >
                  {order.status}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}