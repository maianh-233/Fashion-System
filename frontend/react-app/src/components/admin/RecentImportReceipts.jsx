import { Link } from "react-router-dom";
import { money } from "../../pages/admin/dashboardLogic";
import { RECEIPT_STATUSES, RECEIPT_STATUS_STYLES } from "../../pages/admin/warehouseLogic";

export default function RecentImportReceipts({ receipts = [], showStore = false }) {
  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <div className="flex justify-between mb-4">
        <h3 className="font-semibold">Phiếu nhập gần đây</h3>
        <Link to="/admin/imports" className="text-amber-400 text-sm hover:underline">Xem tất cả →</Link>
      </div>

      {receipts.length === 0 ? (
        <p className="text-sm text-zinc-400">Chưa có phiếu nhập.</p>
      ) : (
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
                <td className="py-4 pr-3">
                  <p className="max-w-[11rem] truncate" title={receipt.code}>{receipt.code}</p>
                  {showStore && <p className="text-xs text-zinc-500">{receipt.storeName}</p>}
                </td>
                <td className="py-4">{receipt.supplierName || "—"}</td>
                <td className="py-4 font-medium">{money(receipt.totalAmount)}</td>
                <td className="py-4 text-center">
                  <span className={`px-4 py-1 rounded-full text-xs ${RECEIPT_STATUS_STYLES[receipt.status] || ""}`}>
                    {RECEIPT_STATUSES[receipt.status] || receipt.status}
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
