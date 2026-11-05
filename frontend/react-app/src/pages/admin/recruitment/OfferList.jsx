import { useState } from "react";
import { Link } from "react-router-dom";
import Pagination from "../../../components/common/Pagination";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { Badge, ErrorText, card, control, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { OFFER_STATUS, formatDate, money, pageCount } from "./recruitmentLogic";

/** Tất cả offer người dùng được xem (quản lý / duyệt vị trí); offer chờ duyệt lên đầu. */
export default function OfferList() {
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(1);
  const params = { status, page: page - 1, size: 20 };
  const { data, error, loading } = useLoader((signal) => recruitmentApi.offers(params, { signal }), [JSON.stringify(params)]);
  return (
    <div className="space-y-4">
      <select className={`${control} w-56`} value={status} onChange={(e) => { setStatus(e.target.value); setPage(1); }}>
        <option value="">Mọi trạng thái</option>
        {Object.entries(OFFER_STATUS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
      </select>
      <ErrorText>{error}</ErrorText>
      <section className={`${card} overflow-hidden`}>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[900px] text-sm">
            <thead className="bg-zinc-800/70 text-zinc-300">
              <tr>{["Ứng viên", "Chức danh", "Lương", "Bắt đầu", "Hạn phản hồi", "Người tạo / duyệt", "Trạng thái"].map((h) => <th key={h} className="px-4 py-3 text-left font-semibold">{h}</th>)}</tr>
            </thead>
            <tbody>
              {loading && !data && <tr><td colSpan="7" className="px-4 py-12 text-center text-zinc-400">Đang tải…</td></tr>}
              {data?.content?.length === 0 && <tr><td colSpan="7" className="px-4 py-12 text-center text-zinc-400">Chưa có offer.</td></tr>}
              {data?.content?.map((o) => (
                <tr key={o.id} className="border-t border-zinc-800 hover:bg-zinc-800/30">
                  <td className="px-4 py-3">
                    <Link to={`/admin/recruitment/applications/${o.applicationId}`} className="font-semibold text-zinc-100 hover:text-amber-200">{o.candidateName}</Link>
                    <p className="text-xs text-zinc-500">{o.applicationCode} · {o.positionTitle}</p>
                  </td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{o.jobTitle}</td>
                  <td className="px-4 py-3 tabular-nums text-xs text-zinc-100">{money(o.salary)}</td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{formatDate(o.startDate)}</td>
                  <td className="px-4 py-3 text-xs text-zinc-300">{formatDate(o.responseDeadline)}</td>
                  <td className="px-4 py-3 text-xs text-zinc-400">{o.createdByName}{o.approvedByName ? <p>Duyệt: {o.approvedByName}</p> : null}</td>
                  <td className="px-4 py-3">
                    <Badge map={OFFER_STATUS} value={o.status} />
                    {o.status === "PENDING_APPROVAL" && o.canApprove && <p className="mt-1 text-xs text-amber-300">Chờ bạn duyệt</p>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pagination currentPage={page} totalPages={pageCount(data)} onPageChange={setPage} />
      </section>
    </div>
  );
}
