import { Link } from "react-router-dom";
import { count } from "../../pages/admin/dashboardLogic";
import { ISSUE_TYPES, RECEIPT_STATUSES, RECEIPT_STATUS_STYLES } from "../../pages/admin/warehouseLogic";

/** Phiếu xuất không có giá trị tiền nên hiển thị loại xuất và số lượng. */
export default function RecentExportReceipts({ issues = [], showStore = false }) {
  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <div className="flex justify-between mb-4">
        <h3 className="font-semibold">Phiếu xuất gần đây</h3>
        <Link to="/admin/exports" className="text-amber-400 text-sm hover:underline">Xem tất cả →</Link>
      </div>

      {issues.length === 0 ? (
        <p className="text-sm text-zinc-400">Chưa có phiếu xuất.</p>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-zinc-700 text-zinc-400">
              <th className="py-3 text-left">Mã phiếu</th>
              <th className="py-3 text-left">Loại xuất</th>
              <th className="py-3 text-left">Số lượng</th>
              <th className="py-3 text-center">Trạng thái</th>
            </tr>
          </thead>

          <tbody className="divide-y divide-zinc-800">
            {issues.map((issue) => (
              <tr key={issue.id}>
                <td className="py-4 pr-3">
                  <p className="max-w-[11rem] truncate" title={issue.code}>{issue.code}</p>
                  {showStore && <p className="text-xs text-zinc-500">{issue.storeName}</p>}
                </td>
                <td className="py-4">{ISSUE_TYPES[issue.issueType] || issue.issueType}</td>
                <td className="py-4 font-medium">{count(issue.totalQuantity)}</td>
                <td className="py-4 text-center">
                  <span className={`px-4 py-1 rounded-full text-xs ${RECEIPT_STATUS_STYLES[issue.status] || ""}`}>
                    {RECEIPT_STATUSES[issue.status] || issue.status}
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
