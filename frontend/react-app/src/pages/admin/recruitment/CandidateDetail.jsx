import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Pencil, Plus, Trash2, Upload } from "lucide-react";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { useSystemNotification } from "../../../components/common/SystemNotification";
import {
  Badge, DocumentList, ErrorText, Loading, Section, UploadDialog, btnDanger, btnGhost, btnPrimary, useLoader, useRecruitment,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { APPLICATION_STATUS, candidateToForm, formatDate } from "./recruitmentLogic";
import { AddApplicationDialog, CandidateDialog } from "./ApplicationDialogs";

/** Chi tiết ứng viên: thông tin, tài liệu & phiên bản CV, các lần ứng tuyển, cảnh báo trùng. */
export default function CandidateDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const notification = useSystemNotification();
  const { can, sourceLabel } = useRecruitment();
  const { data, error, loading, reload, setData } = useLoader((signal) => recruitmentApi.candidate(id, { signal }), [id]);
  const [dialog, setDialog] = useState(null);

  if (loading && !data) return <Loading />;
  if (!data) return <ErrorText>{error}</ErrorText>;
  const c = data;

  const remove = async () => {
    if (!await notification.confirm(`Xóa ứng viên ${c.fullName}?`, { confirmText: "Xóa" })) return;
    try {
      await recruitmentApi.deleteCandidate(id);
      navigate("/admin/recruitment/candidates");
    } catch (e) {
      notification.error(e.message);
    }
  };

  const info = [
    ["Email", c.email], ["Điện thoại", c.phone], ["Ngày sinh", formatDate(c.dateOfBirth)],
    ["Giới tính", { MALE: "Nam", FEMALE: "Nữ", OTHER: "Khác" }[c.gender]], ["Địa chỉ", c.address],
    ["Chức danh hiện tại", [c.currentTitle, c.currentCompany].filter(Boolean).join(" – ")],
    ["Kinh nghiệm", c.yearsExperience != null ? `${Number(c.yearsExperience)} năm` : null], ["Học vấn", c.education],
    ["Nguồn", [sourceLabel(c.source), c.sourceDetail].filter(Boolean).join(" · ")], ["Cửa hàng tiếp nhận", c.storeName || "Văn phòng"],
  ];

  return (
    <div className="space-y-5">
      <Link to="/admin/recruitment/candidates" className="inline-flex items-center gap-1.5 text-sm text-zinc-400 hover:text-white"><ArrowLeft size={14} />Ứng viên</Link>
      {data.duplicates.length > 0 && (
        <div className="rounded-2xl border border-amber-500/40 bg-amber-500/10 p-4 text-sm text-amber-100">
          Có thể trùng với:{" "}
          {data.duplicates.map((m, i) => (
            <span key={m.id}>{i > 0 && ", "}<Link className="underline" to={`/admin/recruitment/candidates/${m.id}`}>{m.code} – {m.fullName}</Link>
              <span className="text-amber-200/70"> ({m.matchedOn.map((x) => (x === "EMAIL" ? "email" : "SĐT")).join(", ")})</span></span>
          ))}
        </div>
      )}
      <div className="grid gap-5 xl:grid-cols-3">
        <div className="space-y-5 xl:col-span-2">
          <Section>
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <h2 className="text-xl font-semibold">{c.fullName}</h2>
                {c.employeeCode && <p className="text-sm text-emerald-300">Đã là nhân viên · mã {c.employeeCode}</p>}
                <p className="text-sm text-zinc-400">{c.code} · tạo {formatDate(c.createdAt)}{c.createdByName ? ` bởi ${c.createdByName}` : " qua trang tuyển dụng"}</p>
              </div>
              {c.canManage && (
                <div className="flex gap-2">
                  <button type="button" className={btnGhost} onClick={() => setDialog({ type: "edit" })}><Pencil size={14} />Sửa</button>
                  {can.admin && <button type="button" className={btnDanger} onClick={remove}><Trash2 size={14} /></button>}
                </div>
              )}
            </div>
            <dl className="mt-4 grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
              {info.filter(([, v]) => v).map(([label, value]) => (
                <div key={label} className="flex justify-between gap-3 border-b border-zinc-800/60 py-1.5"><dt className="text-zinc-400">{label}</dt><dd className="text-right text-zinc-100">{value}</dd></div>
              ))}
            </dl>
            {c.skills.length > 0 && <div className="mt-4 flex flex-wrap gap-1.5">{c.skills.map((s) => <span key={s} className="rounded-full bg-zinc-800 px-2.5 py-1 text-xs">{s}</span>)}</div>}
            {c.tags.length > 0 && <div className="mt-2 flex flex-wrap gap-1.5">{c.tags.map((s) => <span key={s} className="rounded-full border border-amber-400/40 px-2.5 py-1 text-xs text-amber-200">#{s}</span>)}</div>}
            {(c.linkedinUrl || c.portfolioUrl) && (
              <p className="mt-3 space-x-3 text-sm">
                {c.linkedinUrl && <a className="text-amber-300 underline" href={c.linkedinUrl} target="_blank" rel="noreferrer">LinkedIn</a>}
                {c.portfolioUrl && <a className="text-amber-300 underline" href={c.portfolioUrl} target="_blank" rel="noreferrer">Portfolio</a>}
              </p>
            )}
            {c.summary && <p className="mt-3 whitespace-pre-line text-sm text-zinc-300">{c.summary}</p>}
            {c.notes && <p className="mt-3 rounded-xl bg-zinc-950 p-3 text-sm text-zinc-400">Ghi chú nội bộ: {c.notes}</p>}
          </Section>

          <Section title="Các lần ứng tuyển" action={c.canManage && (
            <button type="button" className={btnPrimary} onClick={() => setDialog({ type: "apply" })}><Plus size={15} />Ứng tuyển vị trí khác</button>)}>
            {c.applications.length === 0 ? <p className="text-sm text-zinc-500">Chưa ứng tuyển vị trí nào.</p> : (
              <ul className="divide-y divide-zinc-800">
                {c.applications.map((a) => (
                  <li key={a.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                    <div>
                      <Link to={`/admin/recruitment/applications/${a.id}`} className="font-medium text-zinc-100 hover:text-amber-200">{a.positionTitle}</Link>
                      <p className="text-xs text-zinc-500">{a.code} · {a.campaignName} · nộp {formatDate(a.appliedAt)}{a.currentStageName ? ` · vòng ${a.stageIndex}/${a.stageCount}: ${a.currentStageName}` : ""}</p>
                    </div>
                    <Badge map={APPLICATION_STATUS} value={a.status} />
                  </li>
                ))}
              </ul>
            )}
          </Section>
        </div>
        <Section title="CV & tài liệu" action={c.canManage && (
          <button type="button" className={btnGhost} onClick={() => setDialog({ type: "upload", docType: "CV" })}><Upload size={14} />Tải lên</button>)}>
          <DocumentList candidateId={id} documents={c.documents} canManage={c.canManage} onChanged={reload}
            onUpload={(docType) => setDialog({ type: "upload", docType })} />
          <p className="mt-3 text-xs text-zinc-500">File lưu ở kho riêng tư, chỉ xem / tải qua link có thời hạn.</p>
        </Section>
      </div>

      {dialog?.type === "edit" && (
        <CandidateDialog candidateId={id} initial={candidateToForm(c)} onClose={() => setDialog(null)}
          onSaved={(saved) => { setData(saved); setDialog(null); notification.success("Đã lưu ứng viên."); }} />
      )}
      {dialog?.type === "apply" && (
        <AddApplicationDialog candidateId={id} onClose={() => setDialog(null)} onSaved={() => { setDialog(null); reload(); }} />
      )}
      {dialog?.type === "upload" && (
        <UploadDialog candidateId={id} defaultType={dialog.docType} onClose={() => setDialog(null)}
          onUploaded={() => { setDialog(null); reload(); notification.success("Đã tải tài liệu."); }} />
      )}
    </div>
  );
}
