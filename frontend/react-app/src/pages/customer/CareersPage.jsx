import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Briefcase, Clock3, MapPin, Search, Wallet } from "lucide-react";
import CustomerPageIntro from "../../components/customer/CustomerPageIntro";
import { careersApi } from "../../api/storefront/careersApi";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { formatDate, salaryText } from "../admin/recruitment/recruitmentLogic";

const EMPLOYMENT = { FULL_TIME: "Toàn thời gian", PART_TIME: "Bán thời gian", CONTRACT: "Hợp đồng", INTERN: "Thực tập", TEMPORARY: "Thời vụ" };

/** Trang tuyển dụng công khai: các vị trí đang tuyển của văn phòng và từng cửa hàng. */
export default function CareersPage() {
  const [keyword, setKeyword] = useState("");
  const kw = useDebouncedValue(keyword, 300);
  const [state, setState] = useState({ key: null, rows: [], error: "" });

  useEffect(() => {
    const controller = new AbortController();
    careersApi.positions({ keyword: kw }, { signal: controller.signal })
      .then((rows) => setState({ key: kw, rows, error: "" }))
      .catch((e) => { if (!controller.signal.aborted) setState({ key: kw, rows: [], error: e.message }); });
    return () => controller.abort();
  }, [kw]);

  const loading = state.key !== kw;
  return (
    <div className="customer-page">
      <div className="mx-auto max-w-6xl space-y-6 px-4 py-8">
        <CustomerPageIntro eyebrow="Tuyển dụng" title="Gia nhập Lunaria"
          description="Cùng chúng tôi mang thời trang thanh lịch đến gần hơn với mọi người. Ứng tuyển trực tuyến, đội ngũ tuyển dụng sẽ liên hệ nếu hồ sơ phù hợp."
          meta={loading ? "" : `${state.rows.length} vị trí đang tuyển`}>
          <label className="relative block w-full max-w-sm">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 opacity-60" />
            <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Tìm vị trí, cửa hàng, khu vực…"
              className="h-11 w-full rounded-full border border-white/15 bg-black/30 pl-10 pr-4 text-sm outline-none focus:border-[var(--customer-page-accent)]" />
          </label>
        </CustomerPageIntro>
        {state.error && <p role="alert" className="text-sm text-rose-300">{state.error}</p>}
        {loading && <p className="py-12 text-center text-sm opacity-70">Đang tải…</p>}
        {!loading && state.rows.length === 0 && !state.error && <p className="py-12 text-center text-sm opacity-70">Hiện chưa có vị trí phù hợp. Vui lòng quay lại sau.</p>}
        <div className="grid gap-4 md:grid-cols-2">
          {state.rows.map((p) => (
            <Link key={p.code} to={`/careers/${p.code}`}
              className="group rounded-3xl border border-[var(--customer-page-border)] bg-[var(--customer-page-surface)] p-5 transition hover:border-[var(--customer-page-accent)]">
              <p className="text-xs uppercase tracking-[0.2em] text-[var(--customer-page-accent)]">{p.storeName || "Văn phòng Lunaria"}</p>
              <h2 className="mt-1 text-lg font-semibold group-hover:text-[var(--customer-page-accent)]">{p.title}</h2>
              <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1.5 text-sm opacity-80">
                <span className="inline-flex items-center gap-1.5"><Wallet size={14} />{salaryText(p)}</span>
                {p.employmentType && <span className="inline-flex items-center gap-1.5"><Briefcase size={14} />{EMPLOYMENT[p.employmentType] || p.employmentType}</span>}
                {(p.workLocation || p.storeAddress) && <span className="inline-flex items-center gap-1.5"><MapPin size={14} />{p.workLocation || p.storeAddress}</span>}
                {p.deadline && <span className="inline-flex items-center gap-1.5"><Clock3 size={14} />Hạn {formatDate(p.deadline)}</span>}
              </div>
              {p.skills.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-1.5">{p.skills.slice(0, 5).map((s) => <span key={s} className="rounded-full border border-white/10 px-2.5 py-0.5 text-xs opacity-80">{s}</span>)}</div>
              )}
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
