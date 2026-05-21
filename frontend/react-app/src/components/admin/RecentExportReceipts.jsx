export default function RecentExportReceipts() {
  const receipts = [
    {
      id: "PX240522-041",
      destination: "Chi nhánh Quận 1",
      total: "22.500.000 đ",
      status: "Đã xuất kho",
      color: "text-emerald-400 bg-emerald-500/20",
    },
    {
      id: "PX240522-040",
      destination: "Kho Hà Nội",
      total: "15.200.000 đ",
      status: "Đang vận chuyển",
      color: "text-amber-400 bg-amber-500/20",
    },
    {
      id: "PX240521-039",
      destination: "Kho Đà Nẵng",
      total: "18.900.000 đ",
      status: "Chờ duyệt",
      color: "text-blue-400 bg-blue-500/20",
    },
  ];

  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <div className="flex justify-between mb-4">
        <h3 className="font-semibold">Phiếu xuất gần đây</h3>
        <button className="text-amber-400 text-sm hover:underline">Xem tất cả →</button>
      </div>

      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-zinc-700 text-zinc-400">
            <th className="py-3 text-left">Mã phiếu</th>
            <th className="py-3 text-left">Điểm đến</th>
            <th className="py-3 text-left">Giá trị</th>
            <th className="py-3 text-center">Trạng thái</th>
          </tr>
        </thead>

        <tbody className="divide-y divide-zinc-800">
          {receipts.map((receipt) => (
            <tr key={receipt.id}>
              <td className="py-4">{receipt.id}</td>
              <td className="py-4">{receipt.destination}</td>
              <td className="py-4 font-medium">{receipt.total}</td>
              <td className="py-4 text-center">
                <span className={`px-4 py-1 rounded-full text-xs ${receipt.color}`}>{receipt.status}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
