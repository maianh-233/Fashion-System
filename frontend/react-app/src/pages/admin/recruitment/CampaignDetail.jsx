import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Pencil, Plus, Trash2 } from "lucide-react";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { useSystemNotification } from "../../../components/common/SystemNotification";
import {
  Badge, ErrorText, Loading, Section, btnDanger, btnGhost, btnPrimary, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { CAMPAIGN_STATUS, POSITION_STATUS, campaignToForm, formatDate } from "./recruitmentLogic";
import { CampaignDialog, PositionDialog } from "./CampaignDialogs";

const NEXT_STATUS = {
  DRAFT: ["OPEN", "CANCELLED"], OPEN: ["PAUSED", "CLOSED"], PAUSED: ["OPEN", "CLOSED"], CLOSED: ["OPEN"], CANCELLED: ["OPEN"],
};

/** Chi tiết chiến dịch: thông tin, thành viên, các vị trí và tiến độ từng vị trí. */
export default function CampaignDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const notification = useSystemNotification();
  const { data, error, loading, reload, setData } = useLoader((signal) => recruitmentApi.campaign(id, { signal }), [id]);
  const [dialog, setDialog] = useState(null);
  const [busy, setBusy] = useState(false);

  if (loading && !data) return <Loading />;
  if (!data) return <ErrorText>{error}</ErrorText>;
  const c = data.campaign;

  const changeStatus = async (status) => {
    setBusy(true);
    try {
      setData(await recruitmentApi.campaignStatus(id, status));
      notification.success(`Chiến dịch: ${CAMPAIGN_STATUS[status].label}`);
    } catch (e) {
      notification.error(e.message);
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!await notification.confirm(`Xóa chiến dịch "${c.name}"?`, { confirmText: "Xóa" })) return;
    try {
      await recruitmentApi.deleteCampaign(id);
      navigate("/admin/recruitment/campaigns");
    } catch (e) {
      notification.error(e.message);
    }
  };

  return (
    <div className="space-y-5">
      <Link to="/admin/recruitment/campaigns" className="inline-flex items-center gap-1.5 text-sm text-zinc-400 hover:text-white"><ArrowLeft size={14} />Chiến dịch</Link>
      <Section>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0 space-y-1">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-xl font-semibold">{c.name}</h2>
              <Badge map={CAMPAIGN_STATUS} value={c.status} />
            </div>
            <p className="text-sm text-zinc-400">
              {c.code} · {c.storeName || "Văn phòng / toàn chuỗi"}{c.departmentName ? ` · ${c.departmentName}` : ""} · Phụ trách: {c.ownerName || "—"}
            </p>
            <p className="text-sm text-zinc-400">{formatDate(c.startDate)}{c.endDate ? ` – ${formatDate(c.endDate)}` : " – chưa đặt ngày kết thúc"}</p>
            {data.description && <p className="max-w-3xl whitespace-pre-line pt-2 text-sm text-zinc-300">{data.description}</p>}
          </div>
          {data.canManage && (
            <div className="flex flex-wrap gap-2">
              {NEXT_STATUS[c.status].map((s) => (
                <button key={s} type="button" className={btnGhost} disabled={busy} onClick={() => changeStatus(s)}>
                  {s === "OPEN" ? (c.status === "DRAFT" ? "Mở tuyển" : "Mở lại") : CAMPAIGN_STATUS[s].label}
                </button>
              ))}
              <button type="button" className={btnGhost} onClick={() => setDialog({ type: "campaign" })}><Pencil size={14} />Sửa</button>
              {c.applications === 0 && <button type="button" className={btnDanger} onClick={remove}><Trash2 size={14} /></button>}
            </div>
          )}
        </div>
        <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {[["Vị trí đang mở", `${c.openPositionCount}/${c.positionCount}`], ["Đã tuyển / chỉ tiêu", `${c.hired}/${c.headcount}`],
            ["Tổng hồ sơ", c.applications], ["Đang xử lý", c.inProcess]].map(([label, value]) => (
            <div key={label} className="rounded-2xl border border-zinc-800 bg-zinc-950/50 px-4 py-3">
              <p className="text-xs text-zinc-400">{label}</p><p className="text-lg font-semibold tabular-nums">{value}</p>
            </div>
          ))}
        </div>
        {data.members.length > 0 && (
          <div className="mt-4 flex flex-wrap gap-2 text-xs">
            {data.members.map((m) => (
              <span key={m.userId} className="rounded-full border border-zinc-700 px-2.5 py-1 text-zinc-300">
                {m.fullName} · {m.role === "RECRUITER" ? "Recruiter" : "Hiring Manager"}
              </span>
            ))}
          </div>
        )}
      </Section>

      <Section title="Vị trí tuyển dụng" action={data.canManage && (
        <button type="button" className={btnPrimary} onClick={() => setDialog({ type: "position" })}><Plus size={15} />Thêm vị trí</button>)}>
        {data.positions.length === 0 ? <p className="text-sm text-zinc-500">Chưa có vị trí nào.</p> : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[860px] text-sm">
              <thead className="text-xs text-zinc-400"><tr>
                {["Vị trí", "Hiring manager", "Số vòng", "Đã tuyển / cần", "Hồ sơ", "Hạn nộp", "Trạng thái"].map((h) => <th key={h} className="px-3 py-2 text-left font-medium">{h}</th>)}
              </tr></thead>
              <tbody>
                {data.positions.map((p) => (
                  <tr key={p.id} className="border-t border-zinc-800 hover:bg-zinc-800/30">
                    <td className="px-3 py-3">
                      <Link to={`/admin/recruitment/positions/${p.id}`} className="font-semibold text-zinc-100 hover:text-amber-200">{p.title}</Link>
                      <p className="text-xs text-zinc-500">{p.code}{p.departmentName ? ` · ${p.departmentName}` : ""}{p.isPublic ? " · công khai" : ""}</p>
                    </td>
                    <td className="px-3 py-3 text-xs text-zinc-300">{p.hiringManagerName || "—"}</td>
                    <td className="px-3 py-3 text-xs text-zinc-300">{p.stageCount || <span className="text-amber-300">Chưa cấu hình</span>}</td>
                    <td className="px-3 py-3 tabular-nums text-xs text-zinc-300">{p.hired}/{p.headcount}</td>
                    <td className="px-3 py-3 tabular-nums text-xs text-zinc-300">{p.applications} <span className="text-zinc-500">({p.inProcess} đang xử lý)</span></td>
                    <td className="px-3 py-3 text-xs text-zinc-300">{formatDate(p.deadline) || "—"}</td>
                    <td className="px-3 py-3"><Badge map={POSITION_STATUS} value={p.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Section>

      {dialog?.type === "campaign" && (
        <CampaignDialog campaignId={id} initial={campaignToForm(data)} onClose={() => setDialog(null)}
          onSaved={(saved) => { setData(saved); setDialog(null); notification.success("Đã lưu chiến dịch."); }} />
      )}
      {dialog?.type === "position" && (
        <PositionDialog campaignId={id} storeId={c.storeId} onClose={() => setDialog(null)}
          onSaved={(saved) => { setDialog(null); reload(); navigate(`/admin/recruitment/positions/${saved.position.id}`); }} />
      )}
    </div>
  );
}
