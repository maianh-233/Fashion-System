export default function RecentImportReceipts() {
  const receipts = [
    {
      id: "PN240522-018",
      supplier: "Công ty Vải Minh Phát",
      total: "45.600.000 đ",
      status: "Đã nhập kho",
      color: "text-emerald-400 bg-emerald-500/20",
    },
    {
      id: "PN240522-017",
      supplier: "Global Textile Co.",
      total: "28.300.000 đ",
      status: "Chờ kiểm hàng",
      color: "text-amber-400 bg-amber-500/20",
    },
    {
      id: "PN240521-016",
      supplier: "An Phú Accessories",
      total: "17.900.000 đ",
      status: "Đã xác nhận",
      color: "text-blue-400 bg-blue-500/20",
    },
  ];

  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <div className="flex justify-between mb-4">
        <h3 className="font-semibold">Phiếu nhập gần đây</h3>
        <button className="text-amber-400 text-sm hover:underline">Xem tất cả →</button>
      </div>

      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-zinc-700 text-zinc-400">
            <th className="py-3 text-left">Mã phiếu</th>
            <th className="py-3 text-left">Nhà cung cấp</th>
            <th className="py-3 text-left">Giá trị</th>
            <th className="py-3 text-center">Trạng thái</th>
          </tr>
        </thead>

        <tbody className="divide-y divide-zinc-800">
          {receipts.map((receipt) => (
            <tr key={receipt.id}>
              <td className="py-4">{receipt.id}</td>
              <td className="py-4">{receipt.supplier}</td>
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
