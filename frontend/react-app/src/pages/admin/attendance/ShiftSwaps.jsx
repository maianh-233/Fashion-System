import { useState } from "react";
import PageGuide from "../../../components/admin/common/PageGuide";
import Pagination from "../../../components/common/Pagination";
import { ArrowLeftRight, Check, X } from "lucide-react";
import { shiftApi } from "../../../api/admin/shiftApi";
import { useAdminPermissions } from "../../../contexts/AdminPermissionsContext";
import {
  Badge, Empty, ErrorText, Field, Loading, ReasonDialog, Section, btnDanger, btnPrimary, control, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { useAttendance } from "./AttendanceLayout";
import { formatDate, formatDateTime } from "./attendanceLogic";
import { SWAP_STATUS, SWAP_TYPE } from "./shiftLogic";

/** Duyệt yêu cầu đổi ca / nhờ làm thay sau khi đồng nghiệp đã đồng ý. */
export default function ShiftSwaps() {
  const { stores, scopeStoreId } = useAttendance();
  const { hasPermission } = useAdminPermissions();
  const canManage = hasPermission("SHIFT_MANAGE") || hasPermission("ATTENDANCE_ADMIN");
  const [filter, setFilter] = useState({ status: "PENDING_APPROVAL", storeId: "", keyword: "" });
  const [page, setPage] = useState(1);
  const { data, error, loading, reload } = useLoader(
    (signal) => shiftApi.swaps({ ...filter, page: page - 1, size: 20 }, { signal }), [filter, page]);
  const [rejecting, setRejecting] = useState(null);
  const [actionError, setActionError] = useState("");
  const [busy, setBusy] = useState("");

  const approve = async (swap) => {
    setBusy(swap.id);
    setActionError("");
    try {
      await shiftApi.approveSwap(swap.id);
      reload();
    } catch (e) {
      setActionError(`${swap.code}: ${e.message}`);
    } finally {
      setBusy("");
    }
  };
  const update = (patch) => { setFilter((f) => ({ ...f, ...patch })); setPage(1); };

  return (
    <div className="space-y-5">
      <PageGuide id="shiftSwaps" />
      <Section>
        <div className="grid gap-3 sm:grid-cols-3">
          <Field label="Trạng thái">
            <select className={control} value={filter.status} onChange={(e) => update({ status: e.target.value })}>
              <option value="">Tất cả</option>
              {Object.entries(SWAP_STATUS).map(([value, s]) => <option key={value} value={value}>{s.label}</option>)}
            </select>
          </Field>
          <Field label="Cửa hàng">
            <select className={control} value={scopeStoreId || filter.storeId} onChange={(e) => update({ storeId: e.target.value })} disabled={Boolean(scopeStoreId)}>
              {!scopeStoreId && <option value="">Toàn chuỗi</option>}
              {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <Field label="Tìm kiếm">
            <input className={control} placeholder="Mã đơn, tên nhân viên…" value={filter.keyword} onChange={(e) => update({ keyword: e.target.value })} />
          </Field>
        </div>
      </Section>
      <ErrorText>{error || actionError}</ErrorText>
      {loading && !data ? <Loading /> : !data?.content?.length ? <Empty>Không có yêu cầu đổi ca.</Empty> : (
        <div className="space-y-3">
          {data.content.map((s) => (
            <Section key={s.id}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-zinc-100">{s.code}</span>
                    <Badge tone={s.swapType === "SWAP" ? "violet" : "sky"}>{SWAP_TYPE[s.swapType]}</Badge>
                    <Badge map={SWAP_STATUS} value={s.status} />
                    {!scopeStoreId && <span className="text-xs text-zinc-500">{s.storeName}</span>}
                  </div>
                  <p className="text-xs text-zinc-500">Tạo lúc {formatDateTime(s.createdAt)}</p>
                </div>
                {canManage && (s.status === "PENDING_APPROVAL" || s.status === "PENDING_PEER") && (
                  <div className="flex gap-2">
                    <button type="button" className={btnDanger} onClick={() => setRejecting(s)} disabled={busy === s.id}><X size={15} />Từ chối</button>
                    {s.status === "PENDING_APPROVAL" && (
                      <button type="button" className={btnPrimary} onClick={() => approve(s)} disabled={busy === s.id}><Check size={15} />{busy === s.id ? "Đang duyệt…" : "Duyệt"}</button>
                    )}
                  </div>
                )}
              </div>
              <div className="mt-4 grid items-center gap-3 md:grid-cols-[1fr_auto_1fr]">
                <Side title="Người yêu cầu" name={s.requesterName} code={s.requesterCode} date={s.requesterDate} shift={s.requesterShift}
                  after={`→ ${s.targetName} làm`} />
                <ArrowLeftRight className="mx-auto hidden text-zinc-500 md:block" size={18} />
                {s.swapType === "SWAP"
                  ? <Side title="Đồng nghiệp" name={s.targetName} code={s.targetCode} date={s.targetDate} shift={s.targetShift} after={`→ ${s.requesterName} làm`} />
                  : <div className="rounded-2xl border border-dashed border-zinc-700 p-3 text-sm text-zinc-400"><p className="text-xs text-zinc-500">Người làm thay</p>
                      <p className="font-medium text-zinc-100">{s.targetName} <span className="text-xs text-zinc-500">{s.targetCode}</span></p>Nhận ca, không đổi lại ca nào.</div>}
              </div>
              <div className="mt-3 space-y-1 text-sm text-zinc-300">
                <p><span className="text-zinc-500">Lý do:</span> {s.reason}</p>
                {s.peerNote && <p><span className="text-zinc-500">Đồng nghiệp ghi chú:</span> {s.peerNote}</p>}
                {s.decidedByName && <p className="text-xs text-zinc-500">Xử lý bởi {s.decidedByName} lúc {formatDateTime(s.decidedAt)}</p>}
                {s.rejectionReason && <p className="text-rose-300">Lý do từ chối / hủy: {s.rejectionReason}</p>}
              </div>
            </Section>
          ))}
          <Pagination currentPage={page} totalPages={data?.page?.totalPages || data?.totalPages || 1} onPageChange={setPage} />
        </div>
      )}
      {rejecting && (
        <ReasonDialog title={`Từ chối ${rejecting.code}`} description="Hai nhân viên sẽ nhận được lý do từ chối." confirmText="Từ chối" danger
          onClose={() => setRejecting(null)}
          onConfirm={async (reason) => { await shiftApi.rejectSwap(rejecting.id, reason); setRejecting(null); reload(); }} />
      )}
    </div>
  );
}

function Side({ title, name, code, date, shift, after }) {
  return (
    <div className="rounded-2xl border border-zinc-800 bg-zinc-950/40 p-3 text-sm">
      <p className="text-xs text-zinc-500">{title}</p>
      <p className="font-medium text-zinc-100">{name} <span className="text-xs text-zinc-500">{code}</span></p>
      <p className="text-zinc-300">{formatDate(date)} · {shift}</p>
      <p className="text-xs text-zinc-500">{after}</p>
    </div>
  );
}
