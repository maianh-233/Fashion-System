import { useState } from "react";
import { Wallet } from "lucide-react";
import PageGuide from "../../components/admin/common/PageGuide";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import Pagination from "../../components/common/Pagination";
import { Badge, Empty, ErrorText, Field, Loading, ReasonDialog, Section, btnDanger, btnPrimary, control, useLoader } from "../../components/admin/recruitment/RecruitmentUi";
import { refundApi } from "../../api/admin/refundApi";

const STATUS = {
  PENDING: { label: "Chờ hoàn tiền", tone: "amber" },
  COMPLETED: { label: "Đã hoàn tiền", tone: "emerald" },
  REJECTED: { label: "Từ chối", tone: "rose" },
};
const money = (value) => `${Number(value || 0).toLocaleString("vi-VN")}đ`;
const dateTime = (value) => (value ? new Date(value).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" }) : "");

/** Kế toán xử lý các khoản hoàn tiền phát sinh từ thanh toán online. */
export default function RefundManagement() {
  const [status, setStatus] = useState("PENDING");
  const [page, setPage] = useState(1);
  const [version, setVersion] = useState(0);
  const [dialog, setDialog] = useState(null);
  const { data, error, loading } = useLoader((signal) => refundApi.list({ status, page: page - 1, size: 20 }, { signal }), [status, page, version]);
  const rows = data?.content || [];
  const done = () => { setDialog(null); setVersion((v) => v + 1); };

  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
      <AdminCatalogPageHeader icon={Wallet} eyebrow="Bán hàng" title="Hoàn tiền"
        description="Các khoản cần hoàn cho khách từ thanh toán VNPay / MoMo: tiền về sau khi hủy đơn, trả trùng, hủy sau khi giao thất bại." />
      <PageGuide id="refunds" />
      <Section>
        <Field label="Trạng thái" className="w-full sm:w-56">
          <select className={control} value={status} onChange={(e) => { setStatus(e.target.value); setPage(1); }}>
            <option value="">Tất cả</option>
            {Object.entries(STATUS).map(([value, s]) => <option key={value} value={value}>{s.label}</option>)}
          </select>
        </Field>
      </Section>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : !rows.length ? <Empty>Không có khoản hoàn tiền nào.</Empty> : (
        <Section className="overflow-x-auto p-0">
          <table className="w-full min-w-[920px] text-sm">
            <thead className="text-left text-xs text-zinc-400">
              <tr className="border-b border-zinc-800">
                <th className="px-4 py-3 font-medium">Đơn hàng</th>
                <th className="px-4 py-3 font-medium">Khách hàng</th>
                <th className="px-4 py-3 text-right font-medium">Số tiền</th>
                <th className="px-4 py-3 font-medium">Giao dịch gốc</th>
                <th className="px-4 py-3 font-medium">Lý do</th>
                <th className="px-4 py-3 font-medium">Trạng thái</th>
                <th className="px-4 py-3 font-medium">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className="border-b border-zinc-800/70 align-top">
                  <td className="px-4 py-3 font-semibold text-zinc-100">{r.orderCode || "—"}<p className="text-xs font-normal text-zinc-500">{r.storeName || "Nhiều cửa hàng"}</p></td>
                  <td className="px-4 py-3 text-zinc-300">{r.customerName || "Khách vãng lai"}<p className="text-xs text-zinc-500">{r.customerPhone}</p></td>
                  <td className="px-4 py-3 text-right font-semibold tabular-nums text-zinc-100">{money(r.amount)}</td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{r.paymentMethod}<p className="break-all text-zinc-500">{r.gatewayTransactionCode || r.paymentCode}</p></td>
                  <td className="max-w-[18rem] px-4 py-3 text-xs text-zinc-300">{r.reason}{r.rejectReason && <p className="mt-1 text-rose-300">Từ chối: {r.rejectReason}</p>}<p className="mt-1 text-zinc-500">{dateTime(r.requestedAt)}</p></td>
                  <td className="px-4 py-3"><Badge map={STATUS} value={r.status} />{r.duplicatePayment && <p className="mt-1 text-xs text-amber-300">Trả trùng</p>}{r.processedAt && <p className="mt-1 text-xs text-zinc-500">{dateTime(r.processedAt)}</p>}</td>
                  <td className="px-4 py-3">
                    {r.status === "PENDING" && (
                      <div className="flex flex-wrap gap-2">
                        <button type="button" className={btnPrimary} onClick={() => setDialog({ type: "complete", refund: r })}>Đã hoàn tiền</button>
                        <button type="button" className={btnDanger} onClick={() => setDialog({ type: "reject", refund: r })}>Từ chối</button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Section>
      )}
      <Pagination currentPage={page} totalPages={data?.page?.totalPages || data?.totalPages || 1} onPageChange={setPage} />
      {dialog?.type === "complete" && (
        <ReasonDialog title={`Xác nhận đã hoàn ${money(dialog.refund.amount)}`} description="Khách sẽ nhận thông báo và trạng thái đơn tự cập nhật." label="Mã giao dịch hoàn tiền"
          confirmText="Xác nhận đã hoàn" onClose={() => setDialog(null)} onConfirm={async (code) => { await refundApi.complete(dialog.refund.id, code); done(); }} />
      )}
      {dialog?.type === "reject" && (
        <ReasonDialog title="Từ chối hoàn tiền" description="Chỉ từ chối khi khoản này không cần hoàn, ví dụ đã hoàn bằng cách khác." danger confirmText="Từ chối"
          onClose={() => setDialog(null)} onConfirm={async (reason) => { await refundApi.reject(dialog.refund.id, reason); done(); }} />
      )}
    </div>
  );
}
