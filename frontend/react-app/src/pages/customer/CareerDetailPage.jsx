import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Briefcase, CheckCircle2, Clock3, GraduationCap, MapPin, Users, Wallet } from "lucide-react";
import { careersApi } from "../../api/storefront/careersApi";
import { experienceText, formatDate, salaryText, validateFile } from "../admin/recruitment/recruitmentLogic";

const EMPLOYMENT = { FULL_TIME: "Toàn thời gian", PART_TIME: "Bán thời gian", CONTRACT: "Hợp đồng", INTERN: "Thực tập", TEMPORARY: "Thời vụ" };
const input = "h-11 w-full rounded-xl border border-[var(--customer-page-border)] bg-black/30 px-3 text-sm outline-none focus:border-[var(--customer-page-accent)]";
const SOURCES = [["WEBSITE", "Website Lunaria"], ["FACEBOOK", "Facebook"], ["TOPCV", "TopCV"], ["VIETNAMWORKS", "VietnamWorks"], ["LINKEDIN", "LinkedIn"], ["REFERRAL", "Người quen giới thiệu"], ["OTHER", "Khác"]];

/** Chi tiết vị trí + biểu mẫu ứng tuyển (CV bắt buộc; thư xin việc, portfolio tùy chọn). */
export default function CareerDetailPage() {
  const { code } = useParams();
  const [state, setState] = useState({ code: null, position: null, error: "" });
  const [form, setForm] = useState({ fullName: "", email: "", phone: "", dateOfBirth: "", currentTitle: "", yearsExperience: "", skills: "", linkedinUrl: "", portfolioUrl: "", coverNote: "", source: "WEBSITE", consent: false });
  const [files, setFiles] = useState({ cv: null, coverLetter: null, portfolio: null });
  const [status, setStatus] = useState({ sending: false, error: "", result: null });

  useEffect(() => {
    const controller = new AbortController();
    careersApi.position(code, { signal: controller.signal })
      .then((position) => setState({ code, position, error: "" }))
      .catch((e) => { if (!controller.signal.aborted) setState({ code, position: null, error: e.message }); });
    return () => controller.abort();
  }, [code]);

  const set = (patch) => setForm((f) => ({ ...f, ...patch }));
  const submit = async (event) => {
    event.preventDefault();
    if (!form.fullName.trim() || !form.email.trim() || !form.phone.trim()) return setStatus({ sending: false, error: "Vui lòng nhập họ tên, email và số điện thoại.", result: null });
    const fileError = validateFile(files.cv) || (files.coverLetter ? validateFile(files.coverLetter) : "") || (files.portfolio ? validateFile(files.portfolio) : "");
    if (fileError) return setStatus({ sending: false, error: fileError, result: null });
    if (!form.consent) return setStatus({ sending: false, error: "Vui lòng đồng ý cho phép Lunaria lưu và xử lý hồ sơ.", result: null });
    const data = new FormData();
    Object.entries(form).forEach(([k, v]) => { if (v !== "" && v != null) data.append(k, String(v)); });
    data.append("cv", files.cv);
    if (files.coverLetter) data.append("coverLetter", files.coverLetter);
    if (files.portfolio) data.append("portfolio", files.portfolio);
    setStatus({ sending: true, error: "", result: null });
    try {
      setStatus({ sending: false, error: "", result: await careersApi.apply(code, data) });
    } catch (e) {
      setStatus({ sending: false, error: e.message, result: null });
    }
  };

  const p = state.position;
  if (state.code !== code) return <div className="customer-page"><p className="py-24 text-center text-sm opacity-70">Đang tải…</p></div>;
  if (!p) {
    return (
      <div className="customer-page">
        <div className="mx-auto max-w-3xl px-4 py-24 text-center">
          <p className="text-lg">{state.error || "Vị trí không tồn tại"}</p>
          <Link to="/careers" className="mt-4 inline-block text-[var(--customer-page-accent)] underline">Xem các vị trí khác</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="customer-page">
      <div className="mx-auto max-w-6xl space-y-6 px-4 py-8">
        <Link to="/careers" className="inline-flex items-center gap-1.5 text-sm opacity-70 hover:opacity-100"><ArrowLeft size={14} />Tất cả vị trí</Link>
        <header className="space-y-3">
          <p className="text-xs uppercase tracking-[0.2em] text-[var(--customer-page-accent)]">{p.storeName || "Văn phòng Lunaria"}{p.departmentName ? ` · ${p.departmentName}` : ""}</p>
          <h1 className="text-3xl font-semibold">{p.title}</h1>
          <div className="flex flex-wrap gap-x-5 gap-y-2 text-sm opacity-85">
            <span className="inline-flex items-center gap-1.5"><Wallet size={15} />{salaryText(p)}</span>
            {p.employmentType && <span className="inline-flex items-center gap-1.5"><Briefcase size={15} />{EMPLOYMENT[p.employmentType] || p.employmentType}</span>}
            <span className="inline-flex items-center gap-1.5"><GraduationCap size={15} />Kinh nghiệm: {experienceText(p)}</span>
            <span className="inline-flex items-center gap-1.5"><Users size={15} />Tuyển {p.headcount} người</span>
            {(p.workLocation || p.storeAddress) && <span className="inline-flex items-center gap-1.5"><MapPin size={15} />{p.workLocation || p.storeAddress}</span>}
            {p.deadline && <span className="inline-flex items-center gap-1.5"><Clock3 size={15} />Hạn nộp {formatDate(p.deadline)}</span>}
          </div>
        </header>
        <div className="grid gap-6 lg:grid-cols-5">
          <article className="space-y-6 rounded-3xl border border-[var(--customer-page-border)] bg-[var(--customer-page-surface)] p-6 lg:col-span-3">
            {[["Mô tả công việc", p.jobDescription], ["Yêu cầu", p.requirements], ["Quyền lợi", p.benefits]].filter(([, t]) => t).map(([title, text]) => (
              <section key={title}><h2 className="mb-2 text-lg font-semibold">{title}</h2><p className="whitespace-pre-line text-sm leading-relaxed opacity-85">{text}</p></section>
            ))}
            {p.skills.length > 0 && (
              <section><h2 className="mb-2 text-lg font-semibold">Kỹ năng</h2>
                <div className="flex flex-wrap gap-2">{p.skills.map((s) => <span key={s} className="rounded-full border border-white/15 px-3 py-1 text-sm">{s}</span>)}</div>
              </section>
            )}
          </article>
          <aside className="rounded-3xl border border-[var(--customer-page-border)] bg-[var(--customer-page-surface)] p-6 lg:col-span-2" id="apply">
            {status.result ? (
              <div className="space-y-3 py-6 text-center">
                <CheckCircle2 size={40} className="mx-auto text-emerald-400" />
                <h2 className="text-lg font-semibold">Đã gửi hồ sơ</h2>
                <p className="text-sm opacity-85">{status.result.message}</p>
                <Link to="/careers" className="inline-block text-sm text-[var(--customer-page-accent)] underline">Xem vị trí khác</Link>
              </div>
            ) : (
              <form onSubmit={submit} className="space-y-3">
                <h2 className="text-lg font-semibold">Ứng tuyển</h2>
                <input className={input} placeholder="Họ và tên *" value={form.fullName} maxLength={200} onChange={(e) => set({ fullName: e.target.value })} autoComplete="name" />
                <input className={input} type="email" placeholder="Email *" value={form.email} onChange={(e) => set({ email: e.target.value })} autoComplete="email" />
                <input className={input} placeholder="Số điện thoại *" value={form.phone} onChange={(e) => set({ phone: e.target.value })} autoComplete="tel" />
                <div className="grid grid-cols-2 gap-3">
                  <label className="text-xs opacity-70">Ngày sinh<input className={input} type="date" value={form.dateOfBirth} onChange={(e) => set({ dateOfBirth: e.target.value })} /></label>
                  <label className="text-xs opacity-70">Số năm kinh nghiệm<input className={input} type="number" min="0" step="0.5" value={form.yearsExperience} onChange={(e) => set({ yearsExperience: e.target.value })} /></label>
                </div>
                <input className={input} placeholder="Công việc hiện tại" value={form.currentTitle} onChange={(e) => set({ currentTitle: e.target.value })} />
                <input className={input} placeholder="Kỹ năng (ngăn cách bằng dấu phẩy)" value={form.skills} onChange={(e) => set({ skills: e.target.value })} />
                <input className={input} placeholder="Link portfolio / LinkedIn" value={form.portfolioUrl} onChange={(e) => set({ portfolioUrl: e.target.value })} />
                <textarea className={`${input} h-24 py-2`} placeholder="Giới thiệu ngắn về bạn" value={form.coverNote} maxLength={4000} onChange={(e) => set({ coverNote: e.target.value })} />
                {[["cv", "CV *"], ["coverLetter", "Thư xin việc"], ["portfolio", "Portfolio"]].map(([key, label]) => (
                  <label key={key} className="block text-xs opacity-80">{label} <span className="opacity-60">(PDF, DOC, DOCX, JPG, PNG · ≤ 10MB)</span>
                    <input type="file" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" className="mt-1 block w-full text-sm file:mr-3 file:rounded-full file:border-0 file:bg-white/10 file:px-3 file:py-1.5 file:text-inherit"
                      onChange={(e) => setFiles((f) => ({ ...f, [key]: e.target.files?.[0] || null }))} />
                  </label>
                ))}
                <label className="block text-xs opacity-80">Bạn biết tin tuyển dụng qua
                  <select className={input} value={form.source} onChange={(e) => set({ source: e.target.value })}>
                    {SOURCES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                  </select>
                </label>
                <label className="flex items-start gap-2 text-xs opacity-85">
                  <input type="checkbox" className="mt-0.5" checked={form.consent} onChange={(e) => set({ consent: e.target.checked })} />
                  Tôi đồng ý để Lunaria lưu trữ và xử lý hồ sơ cho mục đích tuyển dụng.
                </label>
                {status.error && <p role="alert" className="text-sm text-rose-300">{status.error}</p>}
                <button type="submit" disabled={status.sending}
                  className="h-11 w-full rounded-full bg-[var(--customer-page-accent)] text-sm font-semibold text-black disabled:opacity-60">
                  {status.sending ? "Đang gửi…" : "Gửi hồ sơ"}
                </button>
              </form>
            )}
          </aside>
        </div>
      </div>
    </div>
  );
}
