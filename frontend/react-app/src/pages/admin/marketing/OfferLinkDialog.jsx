import { useEffect, useState } from "react";
import { Link2, Search } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import Pagination from "../../../components/common/Pagination";
import { Badge, Empty, ErrorText, Loading, btnGhost, btnPrimary, control, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { marketingApi } from "../../../api/admin/marketingApi";
import { OFFER_STATUS, OFFER_TYPES, formatWindow, offerLabel } from "./marketingLogic";

/**
 * Chọn ưu đãi có sẵn để liên kết. Module Marketing không tạo ưu đãi: muốn có ưu đãi mới thì tạo ở màn hình
 * Khuyến mãi / Chiến dịch voucher / Flash sale rồi quay lại liên kết.
 */
export default function OfferLinkDialog({ campaignId, onClose, onLinked }) {
  const [type, setType] = useState("PROMOTION");
  const [draft, setDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const timer = setTimeout(() => { setKeyword(draft.trim()); setPage(1); }, 300);
    return () => clearTimeout(timer);
  }, [draft]);

  const { data, error: loadError, loading } = useLoader((signal) => marketingApi.candidates({ type, q: keyword, page: page - 1, size: 8 }, { signal }),
    [type, keyword, page]);
  const rows = data?.content || [];

  const link = async (item) => {
    setBusyId(item.itemId);
    setError("");
    try {
      onLinked(await marketingApi.link(campaignId, type, item.itemId));
    } catch (e) {
      setError(e.message);
      setBusyId(null);
    }
  };

  return (
    <AdminDialog onClose={onClose}>
      <AdminDialogHeader title="Liên kết ưu đãi" onClose={onClose}
        description="Chọn ưu đãi đã tạo sẵn. Mỗi ưu đãi chỉ thuộc một chiến dịch; ưu đãi đã thuộc chiến dịch khác không chọn được." />
      <AdminDialogBody className="space-y-4">
        <div className="flex flex-wrap gap-2" role="tablist">
          {Object.entries(OFFER_TYPES).map(([key, t]) => (
            <button key={key} type="button" role="tab" aria-selected={type === key}
              onClick={() => { setType(key); setPage(1); }}
              className={`rounded-xl border px-3 py-1.5 text-sm ${type === key ? "border-amber-400 bg-amber-500/10 text-amber-200" : "border-zinc-700 text-zinc-400 hover:text-zinc-200"}`}>
              {t.label}
            </button>
          ))}
        </div>
        <p className="text-xs text-zinc-500">{OFFER_TYPES[type].hint}</p>
        <div className="relative">
          <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
          <input className={`${control} pl-9`} value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="Tìm theo mã hoặc tên…" aria-label="Tìm ưu đãi" />
        </div>
        <ErrorText>{loadError || error}</ErrorText>
        {loading && !data ? <Loading /> : !rows.length ? <Empty>Không có ưu đãi phù hợp.</Empty> : (
          <ul className="space-y-2">
            {rows.map((item) => (
              <li key={item.itemId} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/60 px-3 py-2.5">
                <div className="min-w-0 flex-1">
                  <p className="break-words text-sm font-semibold text-zinc-100">{offerLabel(item)}</p>
                  <p className="text-xs text-zinc-400">{item.detail}</p>
                  <p className="text-xs text-zinc-500">{formatWindow(item.validFrom, item.validTo)}</p>
                  {item.reason && <p className="mt-1 text-xs text-amber-300">{item.reason}</p>}
                </div>
                <div className="flex items-center gap-2">
                  {item.itemStatus && <Badge map={OFFER_STATUS} value={item.itemStatus} />}
                  <button type="button" className={btnPrimary} disabled={Boolean(item.reason) || busyId !== null} onClick={() => link(item)}>
                    <Link2 size={15} />{busyId === item.itemId ? "Đang liên kết…" : "Liên kết"}
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
        <Pagination currentPage={page} totalPages={data?.page?.totalPages || data?.totalPages || 1} onPageChange={setPage} />
      </AdminDialogBody>
      <AdminDialogFooter>
        <button type="button" className={btnGhost} onClick={onClose}>Đóng</button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}
