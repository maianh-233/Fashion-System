import { useState } from "react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import Pagination from "../../../components/common/Pagination";
import { Badge, Empty, ErrorText, Loading, btnGhost, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { marketingApi } from "../../../api/admin/marketingApi";
import { RECIPIENT_STATUS, SKIP_REASONS, formatDateTime } from "./marketingLogic";

/** Danh sách người nhận đã chốt của một đợt gửi: trạng thái từng người, số lần thử, lỗi, đã đọc chưa. */
export default function RecipientsDialog({ campaignId, dispatch, onClose }) {
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(1);
  const { data, error, loading } = useLoader((signal) => marketingApi.recipients(campaignId, dispatch.id, { status, page: page - 1, size: 20 }, { signal }),
    [status, page]);
  const rows = data?.content || [];

  return (
    <AdminDialog onClose={onClose}>
      <AdminDialogHeader title={`Người nhận · ${dispatch.code}`} onClose={onClose}
        description={dispatch.snapshotAt ? `Danh sách chốt lúc ${formatDateTime(dispatch.snapshotAt)}.` : "Danh sách chưa được chốt."} />
      <AdminDialogBody className="space-y-4">
        <div className="flex flex-wrap gap-2" role="tablist">
          {[["", "Tất cả"], ...Object.entries(RECIPIENT_STATUS).map(([k, v]) => [k, v.label])].map(([key, label]) => (
            <button key={key || "ALL"} type="button" role="tab" aria-selected={status === key} onClick={() => { setStatus(key); setPage(1); }}
              className={`rounded-xl border px-3 py-1.5 text-sm ${status === key ? "border-amber-400 bg-amber-500/10 text-amber-200" : "border-zinc-700 text-zinc-400 hover:text-zinc-200"}`}>
              {label}
            </button>
          ))}
        </div>
        <ErrorText>{error}</ErrorText>
        {loading && !data ? <Loading /> : !rows.length ? <Empty>Không có người nhận.</Empty> : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[620px] text-sm">
              <thead className="text-left text-xs text-zinc-400">
                <tr className="border-b border-zinc-800">
                  <th className="py-2 pr-3 font-medium">Khách hàng</th>
                  <th className="py-2 pr-3 font-medium">Trạng thái</th>
                  <th className="py-2 pr-3 font-medium">Gửi lúc</th>
                  <th className="py-2 pr-3 font-medium">Đã đọc</th>
                  <th className="py-2 font-medium">Ghi chú</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.customerId} className="border-b border-zinc-800/70">
                    <td className="py-2 pr-3"><p className="text-zinc-100">{r.fullName || "—"}</p><p className="text-xs text-zinc-500">{r.customerCode} · {r.phone || "—"}</p></td>
                    <td className="py-2 pr-3"><Badge map={RECIPIENT_STATUS} value={r.status} /></td>
                    <td className="py-2 pr-3 text-xs text-zinc-300">{formatDateTime(r.sentAt) || "—"}</td>
                    <td className="py-2 pr-3 text-xs text-zinc-300">{formatDateTime(r.readAt) || "—"}</td>
                    <td className="py-2 text-xs text-zinc-400">
                      {r.skipReason && <span>{SKIP_REASONS[r.skipReason] || r.skipReason}</span>}
                      {r.lastError && <span className="block break-words text-rose-300">{r.lastErrorCode}: {r.lastError}</span>}
                      {r.attemptCount > 1 && <span className="block">Đã thử {r.attemptCount} lần</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination currentPage={page} totalPages={data?.page?.totalPages || data?.totalPages || 1} onPageChange={setPage} />
      </AdminDialogBody>
      <AdminDialogFooter>
        <button type="button" className={btnGhost} onClick={onClose}>Đóng</button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}
