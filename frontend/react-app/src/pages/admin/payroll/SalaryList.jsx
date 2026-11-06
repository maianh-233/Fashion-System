import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Lock, Search } from "lucide-react";
import Pagination from "../../../components/common/Pagination";
import { payrollApi } from "../../../api/admin/payrollApi";
import { Badge, Empty, ErrorText, Field, Loading, Section, btnGhost, card, control, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { formatDate } from "../attendance/attendanceLogic";
import { usePayroll } from "./PayrollLayout";
import { money } from "./payrollLogic";

export default function SalaryList() {
  const { stores, scopeStoreId } = usePayroll();
  const [params] = useSearchParams();
  const [filter, setFilter] = useState({ keyword: "", storeId: "", filter: params.get("filter") || "" });
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const { data, error, loading } = useLoader((signal) => payrollApi.salaries({ ...filter, page: page - 1, size: 20 }, { signal }), [JSON.stringify(filter), page]);
  const update = (patch) => { setFilter((f) => ({ ...f, ...patch })); setPage(1); };

  return (
    <div className="space-y-4">
      <Section>
        <form className="grid gap-3 md:grid-cols-4" onSubmit={(e) => { e.preventDefault(); update({ keyword: keyword.trim() }); }}>
          <Field label="Nhân viên" className="md:col-span-2">
            <div className="flex gap-2"><input className={control} value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Mã hoặc tên" /><button type="submit" className={btnGhost} aria-label="Tìm"><Search size={15} /></button></div>
          </Field>
          <Field label="Cửa hàng">
            <select className={control} value={filter.storeId} onChange={(e) => update({ storeId: e.target.value })} disabled={Boolean(scopeStoreId)}>
              {!scopeStoreId && <option value="">Tất cả</option>}
              {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <Field label="Hồ sơ lương">
            <select className={control} value={filter.filter} onChange={(e) => update({ filter: e.target.value })}>
              <option value="">Tất cả</option><option value="HAS">Đang có hiệu lực</option><option value="MISSING">Chưa có cấu hình lương</option>
            </select>
          </Field>
        </form>
      </Section>
      <ErrorText>{error}</ErrorText>
      <div className={`${card} overflow-x-auto`}>
        <table className="min-w-full text-sm">
          <thead className="border-b border-zinc-800 text-left text-xs uppercase tracking-wide text-zinc-400">
            <tr><th className="px-4 py-3">Nhân viên</th><th className="px-4 py-3">Cửa hàng / phòng ban</th><th className="px-4 py-3">Chức vụ</th>
              <th className="px-4 py-3 text-right">Lương cơ bản hiện hành</th><th className="px-4 py-3">Hiệu lực từ</th><th className="px-4 py-3">Ngày vào làm</th></tr>
          </thead>
          <tbody className="divide-y divide-zinc-800">
            {loading && !data && <tr><td colSpan="6"><Loading /></td></tr>}
            {data?.content?.length === 0 && <tr><td colSpan="6"><Empty>Không có nhân viên phù hợp.</Empty></td></tr>}
            {data?.content?.map((e) => (
              <tr key={e.userId} className="hover:bg-zinc-800/40">
                <td className="px-4 py-3"><Link className="font-medium text-amber-200 hover:underline" to={`/admin/payroll/salaries/${e.userId}`}>{e.fullName}</Link><div className="text-xs text-zinc-500">{e.employeeCode} · {e.employeeType === "STORE" ? "Cửa hàng" : "Global"}</div></td>
                <td className="px-4 py-3 text-zinc-300">{e.storeName || "Văn phòng"}{e.departmentName && <div className="text-xs text-zinc-500">{e.departmentName}</div>}</td>
                <td className="px-4 py-3 text-zinc-300">{e.positionName || "—"}</td>
                <td className="px-4 py-3 text-right tabular-nums">
                  {!e.current ? <Badge tone="amber">Chưa cấu hình</Badge> : e.current.baseSalary == null
                    ? <span className="inline-flex items-center gap-1 text-zinc-500"><Lock size={13} />Bảo mật</span> : money(e.current.baseSalary)}
                </td>
                <td className="px-4 py-3 text-zinc-400">{e.current ? formatDate(e.current.effectiveFrom) : "—"}</td>
                <td className="px-4 py-3 text-zinc-400">{formatDate(e.hireDate) || "—"}{e.terminationDate && <div className="text-xs text-rose-300">Nghỉ {formatDate(e.terminationDate)}</div>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination currentPage={page} totalPages={data?.totalPages || 1} onPageChange={setPage} />
    </div>
  );
}
