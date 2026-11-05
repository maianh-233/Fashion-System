import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Copy, ExternalLink, Pencil, Plus } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { recruitmentApi } from "../../../api/admin/recruitmentApi";
import { useSystemNotification } from "../../../components/common/SystemNotification";
import {
  Badge, ErrorText, Field, Loading, Section, btnGhost, btnPrimary, control, textarea, useLoader, useRecruitment,
} from "../../../components/admin/recruitment/RecruitmentUi";
import {
  APPLICATION_STATUS, POSITION_STATUS, experienceText, formatDate, labelOf, positionToForm, salaryText, stagesToForm,
  stagesToPayload,
} from "./recruitmentLogic";
import { PositionDialog } from "./CampaignDialogs";
import PipelineEditor from "./PipelineEditor";
import { AddApplicationDialog } from "./ApplicationDialogs";

const TABS = [
  { key: "board", label: "Hồ sơ theo vòng" },
  { key: "pipeline", label: "Quy trình & tiêu chí" },
  { key: "info", label: "Mô tả công việc" },
];

/** Chi tiết vị trí: bảng hồ sơ theo vòng (pipeline), cấu hình quy trình động, JD. */
export default function PositionDetail() {
  const { id } = useParams();
  const notification = useSystemNotification();
  const { lookups } = useRecruitment();
  const [tab, setTab] = useState("board");
  const [dialog, setDialog] = useState(null);
  const position = useLoader((signal) => recruitmentApi.position(id, { signal }), [id]);
  const apps = useLoader((signal) => recruitmentApi.applications({ positionId: id, size: 200 }, { signal }), [id]);
  const owners = useLoader((signal) => (position.data
    ? recruitmentApi.users({ storeId: position.data.position.storeId || undefined }, { signal }) : Promise.resolve([])), [position.data?.position?.storeId]);

  const [pipeline, setPipeline] = useState(null);
  const [pipelineFor, setPipelineFor] = useState(null);
  const [saving, setSaving] = useState(false);
  if (position.data && pipelineFor !== position.data) {
    setPipelineFor(position.data);
    setPipeline(stagesToForm(position.data.stages));
  }

  if (position.loading && !position.data) return <Loading />;
  if (!position.data) return <ErrorText>{position.error}</ErrorText>;
  const d = position.data;
  const p = d.position;
  const canManage = d.canManage;

  const savePipeline = async () => {
    setSaving(true);
    try {
      await recruitmentApi.savePipeline(id, stagesToPayload(pipeline));
      notification.success("Đã lưu quy trình.");
      position.reload();
    } catch (e) {
      notification.error(e.message);
    } finally {
      setSaving(false);
    }
  };

  const changeStatus = async (status) => {
    try {
      position.setData(await recruitmentApi.positionStatus(id, status));
    } catch (e) {
      notification.error(e.message);
    }
  };

  const applications = apps.data?.content || [];
  const columns = [
    ...d.stages.map((s) => ({ key: s.id, title: s.name, items: applications.filter((a) => a.currentStageId === s.id && !["HIRED", "REJECTED", "WITHDRAWN", "OFFER_DECLINED", "QUALIFIED"].includes(a.status)) })),
  ];
  const unstaged = applications.filter((a) => !a.currentStageId && !["HIRED", "REJECTED", "WITHDRAWN"].includes(a.status));
  if (unstaged.length) columns.unshift({ key: "none", title: "Chưa vào vòng", items: unstaged });
  columns.push({ key: "qualified", title: "Đạt yêu cầu", items: applications.filter((a) => a.status === "QUALIFIED") });
  columns.push({ key: "hired", title: "Đã tuyển", items: applications.filter((a) => a.status === "HIRED") });
  columns.push({ key: "closed", title: "Đã đóng", items: applications.filter((a) => ["REJECTED", "WITHDRAWN", "OFFER_DECLINED"].includes(a.status)) });

  return (
    <div className="space-y-5">
      <Link to={`/admin/recruitment/campaigns/${p.campaignId}`} className="inline-flex items-center gap-1.5 text-sm text-zinc-400 hover:text-white">
        <ArrowLeft size={14} />{p.campaignName}
      </Link>
      <Section>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-1">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-xl font-semibold">{p.title}</h2>
              <Badge map={POSITION_STATUS} value={p.status} />
              {p.isPublic && p.status === "OPEN" && (
                <a href={`/careers/${p.code}`} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-xs text-amber-300 hover:underline">
                  Trang tuyển dụng <ExternalLink size={12} />
                </a>
              )}
            </div>
            <p className="text-sm text-zinc-400">
              {p.code} · {p.storeName || "Văn phòng"}{p.departmentName ? ` · ${p.departmentName}` : ""} · Hiring manager: {p.hiringManagerName || "—"}
            </p>
            <p className="text-sm text-zinc-400">
              Cần {p.headcount} · đã tuyển {p.hired} · {p.applications} hồ sơ · Lương {salaryText(d)} · Hạn {formatDate(p.deadline) || "—"}
            </p>
          </div>
          {canManage && (
            <div className="flex flex-wrap gap-2">
              <select className={`${control} w-40`} value={p.status} onChange={(e) => changeStatus(e.target.value)} aria-label="Trạng thái vị trí">
                {Object.entries(POSITION_STATUS).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
              </select>
              <button type="button" className={btnGhost} onClick={() => setDialog("edit")}><Pencil size={14} />Sửa</button>
              <button type="button" className={btnPrimary} onClick={() => setDialog("add")}><Plus size={14} />Thêm hồ sơ</button>
            </div>
          )}
        </div>
      </Section>

      <div className="flex gap-1 rounded-2xl border border-zinc-800 bg-zinc-900 p-1">
        {TABS.map((t) => (
          <button key={t.key} type="button" onClick={() => setTab(t.key)}
            className={`rounded-xl px-3.5 py-2 text-sm ${tab === t.key ? "bg-zinc-700 font-semibold text-white" : "text-zinc-400 hover:text-white"}`}>{t.label}</button>
        ))}
      </div>

      {tab === "board" && (
        <div className="flex gap-3 overflow-x-auto pb-2">
          {apps.loading && !apps.data && <Loading />}
          {columns.map((col) => {
            const stat = d.stageStats.find((s) => s.stageId === col.key);
            return (
              <div key={col.key} className="w-72 shrink-0 rounded-2xl border border-zinc-800 bg-zinc-900 p-3">
                <div className="mb-3 flex items-center justify-between">
                  <p className="truncate text-sm font-semibold">{col.title}</p>
                  <span className="rounded-full bg-zinc-800 px-2 text-xs tabular-nums text-zinc-300">{col.items.length}</span>
                </div>
                {stat && <p className="mb-2 text-xs text-zinc-500">Đã qua vòng: {stat.passed} đạt · {stat.failed} không đạt</p>}
                <div className="space-y-2">
                  {col.items.length === 0 && <p className="py-4 text-center text-xs text-zinc-600">Trống</p>}
                  {col.items.map((a) => (
                    <Link key={a.id} to={`/admin/recruitment/applications/${a.id}`}
                      className="block rounded-xl border border-zinc-800 bg-zinc-950 p-3 hover:border-amber-400/50">
                      <p className="truncate text-sm font-medium text-zinc-100">{a.candidateName}</p>
                      <p className="truncate text-xs text-zinc-500">{a.code} · {formatDate(a.appliedAt)}</p>
                      <div className="mt-2 flex items-center justify-between gap-2">
                        <Badge map={APPLICATION_STATUS} value={a.status} />
                        {a.lastScore != null && <span className="text-xs tabular-nums text-amber-300">{Number(a.lastScore)}</span>}
                      </div>
                    </Link>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {tab === "pipeline" && pipeline && (
        <Section title="Quy trình tuyển dụng" action={canManage && (
          <div className="flex gap-2">
            <button type="button" className={btnGhost} onClick={() => setDialog("template")}>Áp dụng mẫu</button>
            <button type="button" className={btnGhost} onClick={() => setDialog("saveTemplate")}><Copy size={14} />Lưu thành mẫu</button>
          </div>)}>
          <p className="mb-3 text-xs text-zinc-500">
            Thay đổi áp dụng ngay cho hồ sơ đang ở các vòng. Không xóa được vòng đã có hồ sơ đi qua hoặc tiêu chí đã được chấm.
          </p>
          <PipelineEditor value={pipeline} onChange={setPipeline} owners={owners.data || []} readOnly={!canManage}
            onSave={savePipeline} saving={saving} />
        </Section>
      )}

      {tab === "info" && (
        <div className="grid gap-5 lg:grid-cols-3">
          <Section title="Mô tả công việc" className="lg:col-span-2">
            <Block title="Công việc" text={d.jobDescription} />
            <Block title="Yêu cầu" text={d.requirements} />
            <Block title="Quyền lợi" text={d.benefits} />
          </Section>
          <Section title="Thông tin">
            <dl className="space-y-2 text-sm">
              <Row label="Hình thức" value={labelOf(lookups?.employmentTypes || [], d.employmentType) || "—"} />
              <Row label="Nơi làm việc" value={d.workLocation || "—"} />
              <Row label="Kinh nghiệm" value={experienceText(d)} />
              <Row label="Mức lương" value={salaryText(d)} />
              <Row label="Kỹ năng" value={d.skills.join(", ") || "—"} />
            </dl>
          </Section>
        </div>
      )}

      {dialog === "edit" && (
        <PositionDialog positionId={id} storeId={p.storeId} initial={positionToForm(d)} onClose={() => setDialog(null)}
          onSaved={(saved) => { position.setData(saved); setDialog(null); notification.success("Đã lưu vị trí."); }} />
      )}
      {dialog === "add" && (
        <AddApplicationDialog positionId={id} onClose={() => setDialog(null)} onSaved={() => { setDialog(null); apps.reload(); position.reload(); }} />
      )}
      {dialog === "template" && (
        <ApplyTemplateDialog positionId={id} onClose={() => setDialog(null)} onApplied={() => { setDialog(null); position.reload(); }} />
      )}
      {dialog === "saveTemplate" && (
        <SaveTemplateDialog positionId={id} defaultName={p.title} onClose={() => setDialog(null)}
          onSaved={() => { setDialog(null); notification.success("Đã lưu mẫu quy trình."); }} />
      )}
    </div>
  );
}

function Block({ title, text }) {
  return (
    <div className="mb-4">
      <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-zinc-500">{title}</p>
      <p className="whitespace-pre-line text-sm text-zinc-200">{text || "—"}</p>
    </div>
  );
}

function Row({ label, value }) {
  return <div className="flex justify-between gap-3"><dt className="text-zinc-400">{label}</dt><dd className="text-right text-zinc-100">{value}</dd></div>;
}

function ApplyTemplateDialog({ positionId, onClose, onApplied }) {
  const templates = useLoader((signal) => recruitmentApi.templates({ signal }), []);
  const [templateId, setTemplateId] = useState("");
  const [error, setError] = useState("");
  const apply = async () => {
    try {
      await recruitmentApi.applyTemplate(positionId, templateId);
      onApplied();
    } catch (e) {
      setError(e.message);
    }
  };
  const chosen = templates.data?.find((t) => t.id === templateId);
  return (
    <AdminDialog onClose={onClose}>
      <AdminDialogHeader title="Áp dụng mẫu quy trình" description="Thay toàn bộ vòng hiện tại (chỉ khi chưa có hồ sơ nào vào quy trình)." onClose={onClose} />
      <AdminDialogBody className="space-y-3">
        <select className={control} value={templateId} onChange={(e) => setTemplateId(e.target.value)}>
          <option value="">Chọn mẫu…</option>
          {templates.data?.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
        </select>
        {chosen && <p className="text-sm text-zinc-300">{chosen.stages.map((s) => s.name).join(" → ")}</p>}
        <ErrorText>{error}</ErrorText>
      </AdminDialogBody>
      <AdminDialogFooter>
        <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
        <button type="button" className={btnPrimary} disabled={!templateId} onClick={apply}>Áp dụng</button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}

function SaveTemplateDialog({ positionId, defaultName, onClose, onSaved }) {
  const [name, setName] = useState(`Quy trình ${defaultName}`);
  const [description, setDescription] = useState("");
  const [error, setError] = useState("");
  const save = async () => {
    try {
      await recruitmentApi.saveAsTemplate(positionId, { name: name.trim(), description: description.trim() || null });
      onSaved();
    } catch (e) {
      setError(e.message);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <AdminDialogHeader title="Lưu quy trình thành mẫu" onClose={onClose} />
      <AdminDialogBody className="space-y-3">
        <Field label="Tên mẫu" required><input className={control} value={name} maxLength={150} onChange={(e) => setName(e.target.value)} /></Field>
        <Field label="Mô tả"><textarea className={textarea} rows={3} value={description} onChange={(e) => setDescription(e.target.value)} /></Field>
        <ErrorText>{error}</ErrorText>
      </AdminDialogBody>
      <AdminDialogFooter>
        <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
        <button type="button" className={btnPrimary} disabled={!name.trim()} onClick={save}>Lưu mẫu</button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}
