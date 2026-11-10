import { useState } from "react";
import { Download, Info } from "lucide-react";
import { Empty, ErrorText, Loading, Section, Stat, btnGhost, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { marketingApi } from "../../../api/admin/marketingApi";
import { saveBlob } from "../../../api/admin/attendanceApi";
import { OFFER_TYPES, formatDate, formatDateTime, formatMoney, formatPercent, formatRatio, shortDate, targetProgress } from "./marketingLogic";

/**
 * Báo cáo hiệu quả của một chiến dịch: chỉ tiêu so với thực tế, chi phí, mức tiếp cận, theo ngày, theo ưu đãi. Mọi con số
 * doanh thu là của đơn có dùng ưu đãi — không phải doanh thu tăng thêm; giới hạn dữ liệu hiển thị ngay dưới báo cáo.
 */
export default function PerformancePanel({ campaignId, campaignCode }) {
  const { data, error, loading, reload } = useLoader((signal) => marketingApi.performance(campaignId, { signal }), [campaignId]);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState("");

  const download = async () => {
    setExporting(true);
    setExportError("");
    try {
      saveBlob(await marketingApi.exportPerformance(campaignId), `bao-cao-${campaignCode}.xlsx`);
    } catch (e) {
      setExportError(e.message);
    } finally {
      setExporting(false);
    }
  };

  const action = (
    <div className="flex gap-2">
      <button type="button" className={btnGhost} onClick={reload} disabled={loading}>Làm mới</button>
      <button type="button" className={btnGhost} onClick={download} disabled={exporting}><Download size={15} />{exporting ? "Đang xuất…" : "Xuất Excel"}</button>
    </div>
  );
  if (!data) return <Section title="Báo cáo hiệu quả" action={action}><ErrorText>{error}</ErrorText>{loading && <Loading />}</Section>;

  const k = data.kpi;
  const maxDay = Math.max(1, ...data.daily.map((d) => Number(d.revenue)));
  return (
    <Section title="Báo cáo hiệu quả" action={action}>
      <ErrorText>{exportError}</ErrorText>
      <p className="mb-4 text-xs text-zinc-500">Cập nhật lúc {formatDateTime(data.generatedAt)}. Doanh thu là của đơn <b>có dùng ưu đãi</b> của chiến dịch, không phải doanh thu tăng thêm nhờ chiến dịch.</p>

      <div className="space-y-3">
        <Target label="Doanh thu thuần" actual={k.netRevenue} target={k.targetRevenue} format={formatMoney}
          hint={`Gồm cả đơn đang xử lý: ${formatMoney(k.revenue)} · đã hoàn tiền ${formatMoney(k.refunds)}`} />
        <Target label="Số đơn có dùng ưu đãi" actual={k.orders} target={k.targetOrders} hint={`Online ${k.onlineOrders} · tại quầy ${k.offlineOrders}`} />
        <Target label="Khách mua" actual={k.buyers} target={k.targetBuyers} hint={`Trong đó mua lần đầu: ${k.firstTimeBuyers}`} />
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Tổng chi phí" value={formatMoney(k.totalCost)} tone={k.budgetUsagePercent > 100 ? "rose" : "amber"}
          hint={k.budgetUsagePercent === null ? "Chưa đặt ngân sách" : `${k.budgetUsagePercent}% ngân sách`} />
        <Stat label="ROAS" value={formatRatio(k.roas)} tone="emerald" hint="Doanh thu thuần / tổng chi phí" />
        <Stat label="Giá trị đơn trung bình" value={formatMoney(k.averageOrderValue)} tone="sky" />
        <Stat label="Voucher đã dùng" value={`${k.vouchersUsed}/${k.vouchersIssued}`} tone="violet" hint={formatPercent(k.vouchersUsed, k.vouchersIssued)} />
      </div>
      <dl className="mt-3 grid gap-x-6 gap-y-1.5 text-sm sm:grid-cols-2">
        <Row label="Giảm giá khuyến mãi / voucher">{formatMoney(k.discountCost)}</Row>
        <Row label="Giảm giá flash sale (ước tính)">{formatMoney(k.flashDiscountCost)}</Row>
        <Row label="Chi phí thực tế">{formatMoney(k.expenseCost)}</Row>
        <Row label="Ngân sách dự kiến">{formatMoney(k.plannedBudget)}</Row>
        <Row label="Thông báo đã gửi / đã đọc">{k.notificationsSent} / {k.notificationsRead} ({formatPercent(k.notificationsRead, k.notificationsSent)})</Row>
        <Row label="Người nhận có mua sau khi nhận tin">{k.buyersAfterNotification}</Row>
        <Row label="Giá vốn ước tính (đơn đã hoàn tất)">{formatMoney(k.cogs)}{k.uncostedQuantity > 0 ? ` · ${k.uncostedQuantity} món chưa có giá nhập` : ""}</Row>
        <Row label="Lợi nhuận gộp ước tính">{formatMoney(k.grossProfit)}</Row>
        <Row label="ROI theo lợi nhuận gộp">{formatRatio(k.roi)}</Row>
      </dl>

      {data.experiments.length > 0 && (
        <>
          <h3 className="mb-2 mt-5 text-sm font-semibold text-zinc-200">Nhóm đối chứng — mức tăng thêm</h3>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-sm">
              <thead className="text-left text-xs text-zinc-400">
                <tr className="border-b border-zinc-800">
                  <th className="py-2 pr-3 font-medium">Đợt</th>
                  <th className="py-2 pr-3 text-right font-medium">Nhận tin (mua)</th>
                  <th className="py-2 pr-3 text-right font-medium">Đối chứng (mua)</th>
                  <th className="py-2 pr-3 text-right font-medium">Chênh lệch</th>
                  <th className="py-2 text-right font-medium">Tăng thêm (ước tính)</th>
                </tr>
              </thead>
              <tbody>
                {data.experiments.map((e) => (
                  <tr key={e.dispatchId} className="border-b border-zinc-800/70">
                    <td className="py-2 pr-3"><p className="text-zinc-100">{e.code}</p><p className="text-xs text-zinc-500">Đối chứng {e.holdoutPercent}%</p></td>
                    <td className="py-2 pr-3 text-right tabular-nums">{e.treated} ({e.treatedBuyers}) · {e.treatedRate ?? "—"}%</td>
                    <td className="py-2 pr-3 text-right tabular-nums">{e.holdout} ({e.holdoutBuyers}) · {e.holdoutRate ?? "—"}%</td>
                    <td className="py-2 pr-3 text-right tabular-nums">
                      {e.upliftPoints === null ? "—" : `${e.upliftPoints > 0 ? "+" : ""}${e.upliftPoints} điểm %`}
                      <p className={`text-xs ${e.significant ? "text-emerald-300" : "text-zinc-500"}`}>{e.significant ? `Có ý nghĩa (z = ${e.zScore})` : "Chưa đủ ý nghĩa thống kê"}</p>
                    </td>
                    <td className="py-2 text-right tabular-nums">{e.incrementalOrders ?? "—"} đơn · {formatMoney(e.incrementalRevenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <h3 className="mb-2 mt-5 text-sm font-semibold text-zinc-200">Theo ngày</h3>
      {data.daily.length === 0 ? <Empty>Chưa có đơn nào dùng ưu đãi của chiến dịch.</Empty> : (
        <div className="overflow-x-auto" data-table-mode="scroll">
          <div className="flex min-w-max items-end gap-1.5 pb-1" role="img" aria-label="Doanh thu theo ngày">
            {data.daily.map((d) => (
              <div key={d.date} className="flex w-10 flex-col items-center gap-1" title={`${formatDate(d.date)}: ${d.orders} đơn · ${formatMoney(d.revenue)}`}>
                <span className="text-[10px] tabular-nums text-zinc-400">{d.orders}</span>
                <div className="w-6 rounded-t bg-amber-400/80" style={{ height: `${Math.max(4, (Number(d.revenue) / maxDay) * 96)}px` }} />
                <span className="text-[10px] text-zinc-500">{shortDate(d.date)}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <h3 className="mb-2 mt-5 text-sm font-semibold text-zinc-200">Theo ưu đãi</h3>
      {data.offers.length === 0 ? <Empty>Chưa liên kết ưu đãi.</Empty> : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[480px] text-sm">
            <thead className="text-left text-xs text-zinc-400">
              <tr className="border-b border-zinc-800">
                <th className="py-2 pr-3 font-medium">Ưu đãi</th>
                <th className="py-2 pr-3 text-right font-medium">Số đơn</th>
                <th className="py-2 text-right font-medium">Tiền giảm</th>
              </tr>
            </thead>
            <tbody>
              {data.offers.map((o) => (
                <tr key={`${o.type}-${o.code || o.name}`} className="border-b border-zinc-800/70">
                  <td className="py-2 pr-3"><p className="text-zinc-100">{o.code ? `${o.code} – ${o.name}` : o.name}</p><p className="text-xs text-zinc-500">{OFFER_TYPES[o.type]?.label}</p></td>
                  <td className="py-2 pr-3 text-right tabular-nums">{o.orders}</td>
                  <td className="py-2 text-right tabular-nums">{formatMoney(o.discount)}{o.type === "FLASH_SALE" ? " (ước tính)" : ""}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ul className="mt-5 space-y-1.5 rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3 text-xs text-zinc-400" aria-label="Lưu ý về dữ liệu">
        {data.dataNotes.map((note) => <li key={note} className="flex gap-2"><Info size={13} className="mt-0.5 shrink-0 text-sky-300" /><span>{note}</span></li>)}
      </ul>
    </Section>
  );
}

function Target({ label, actual, target, format = (v) => Number(v || 0).toLocaleString("vi-VN"), hint }) {
  const progress = targetProgress(actual, target);
  return (
    <div>
      <div className="flex flex-wrap items-baseline justify-between gap-2 text-sm">
        <span className="text-zinc-300">{label}</span>
        <span className="tabular-nums text-zinc-100"><b>{format(actual)}</b>{progress !== null && <span className="text-zinc-500"> / {format(target)} · {progress}%</span>}</span>
      </div>
      {progress !== null && (
        <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-zinc-800" role="progressbar" aria-label={label} aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100}>
          <div className={`h-full ${progress >= 100 ? "bg-emerald-400" : "bg-amber-400"}`} style={{ width: `${Math.min(100, progress)}%` }} />
        </div>
      )}
      {hint && <p className="mt-0.5 text-xs text-zinc-500">{hint}</p>}
    </div>
  );
}

function Row({ label, children }) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-zinc-800/60 py-1">
      <dt className="text-zinc-400">{label}</dt>
      <dd className="text-right tabular-nums text-zinc-100">{children}</dd>
    </div>
  );
}
