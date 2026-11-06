import { useState } from "react";
import { Download, ReceiptText } from "lucide-react";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import { payslipApi } from "../../../api/admin/payrollApi";
import { saveBlob } from "../../../api/admin/attendanceApi";
import { Badge, Empty, ErrorText, Loading, Section, btnGhost, card, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import PayslipView from "../payroll/PayslipView";
import { PAYMENT_STATUS, money } from "../payroll/payrollLogic";

export default function MyPayslips() {
  const list = useLoader((signal) => payslipApi.list({ signal }), []);
  const [selectedId, setSelectedId] = useState(null);
  const current = selectedId || list.data?.[0]?.id || null;

  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
      <AdminCatalogPageHeader icon={ReceiptText} eyebrow="Cá nhân" title="Phiếu lương của tôi"
        description="Phiếu lương các tháng đã chốt. Thông tin lương là bảo mật — chỉ bạn xem được phiếu của mình." />
      <ErrorText>{list.error}</ErrorText>
      {list.loading && !list.data ? <Loading /> : (list.data || []).length === 0 ? (
        <Section><Empty>Chưa có phiếu lương nào được phát hành.</Empty></Section>
      ) : (
        <div className="grid gap-5 lg:grid-cols-[280px_1fr] [&>*]:min-w-0">
          <nav className={`${card} h-fit p-2`} aria-label="Lịch sử phiếu lương">
            {list.data.map((p) => (
              <button key={p.id} type="button" onClick={() => setSelectedId(p.id)}
                className={`flex w-full items-center justify-between gap-2 rounded-xl px-3 py-2.5 text-left text-sm ${current === p.id ? "bg-zinc-800 text-amber-200" : "text-zinc-300 hover:bg-zinc-800/60"}`}>
                <span><span className="block font-medium">Tháng {monthOf(p)}</span><span className="text-xs text-zinc-500">{money(p.netPay)}</span></span>
                <Badge map={PAYMENT_STATUS} value={p.paymentStatus} />
              </button>
            ))}
          </nav>
          {current && <Payslip id={current} />}
        </div>
      )}
    </div>
  );
}

const monthOf = (row) => `${row.periodMonth.slice(5, 7)}/${row.periodMonth.slice(0, 4)}`;

function Payslip({ id }) {
  const { data, error, loading } = useLoader((signal) => payslipApi.detail(id, { signal }), [id]);
  const [downloadError, setDownloadError] = useState("");
  return (
    <Section title={data ? `Phiếu lương tháng ${data.periodMonth.slice(5, 7)}/${data.periodMonth.slice(0, 4)}` : "Phiếu lương"} action={data && (
      <button type="button" className={btnGhost} onClick={async () => {
        setDownloadError("");
        try { saveBlob(await payslipApi.pdf(id), `phieu-luong_${data.periodMonth.slice(0, 7)}.pdf`); } catch (e) { setDownloadError(e.message); }
      }}><Download size={15} />Tải PDF</button>
    )}>
      <ErrorText>{error || downloadError}</ErrorText>
      {loading && !data ? <Loading /> : data && <PayslipView detail={data} />}
    </Section>
  );
}
