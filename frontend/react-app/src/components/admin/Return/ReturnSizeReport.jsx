import { Fragment, useEffect, useState } from "react";
import { AlertTriangle, ChevronDown, ChevronRight, Download, Ruler } from "lucide-react";
import Button from "../../common/Button";
import StoreSelector, { ALL_STORES } from "../../common/StoreSelector";
import SizeChartDialog from "../catalog/SizeChartDialog";
import { useAdminPermissions } from "../../../contexts/AdminPermissionsContext";
import useStoreOptions from "../../../hooks/useStoreOptions";
import { returnAdminApi } from "../../../api/admin/returnApi";
import {
  FIT_HINT_LABELS, REPORT_PERIODS, formatRate, rateTone, reportRange, sizeReportCsv,
} from "../../../pages/admin/returnManagementLogic";

const control = "h-11 rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none transition focus:border-amber-400";
const count = (value) => Number(value || 0).toLocaleString("vi-VN");

function Stat({ label, value, hint, tone = "text-zinc-100" }) {
  return (
    <div className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4">
      <p className="text-xs text-zinc-500">{label}</p>
      <p className={`mt-1 text-2xl font-semibold ${tone}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-zinc-500">{hint}</p>}
    </div>
  );
}

/**
 * Tỷ lệ đổi / trả theo sản phẩm và size (yêu cầu đã duyệt hoặc hoàn tất, cả đổi lẫn trả, trên đơn hoàn thành tạo trong
 * kỳ). Sản phẩm có nhiều món "không vừa size" lên đầu kèm gợi ý form nhỏ / rộng để chỉnh bảng size của danh mục.
 */
export default function ReturnSizeReport() {
  const { isGlobal, currentStoreName, loading: permissionsLoading } = useAdminPermissions();
  const { stores, loading: storesLoading } = useStoreOptions({ endpoint: returnAdminApi.STORES_PATH, enabled: isGlobal });
  const [days, setDays] = useState(90);
  const [storeId, setStoreId] = useState(ALL_STORES);
  const [result, setResult] = useState({ key: null, data: null, error: "" });
  const [expanded, setExpanded] = useState({});
  const [sizeChartFor, setSizeChartFor] = useState(null);
  const store = isGlobal && storeId !== ALL_STORES ? storeId : "";
  const requestKey = permissionsLoading ? null : `${store}|${days}`;

  useEffect(() => {
    if (!requestKey) return undefined;
    const controller = new AbortController();
    returnAdminApi.sizeReport({ storeId: store, ...reportRange(days) }, { signal: controller.signal })
      .then((data) => setResult({ key: requestKey, data, error: "" }))
      .catch((failure) => {
        if (!controller.signal.aborted) setResult({ key: requestKey, data: null, error: failure.message || "Không tải được báo cáo." });
      });
    return () => controller.abort();
  }, [requestKey, store, days]);

  const loading = result.key !== requestKey;
  const report = loading ? null : result.data;
  const summary = report?.summary;
  const toggle = (id) => setExpanded((current) => ({ ...current, [id]: !current[id] }));
  const download = () => {
    const blob = new Blob([sizeReportCsv(report)], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = Object.assign(document.createElement("a"), { href: url, download: `doi-tra-theo-size-${report.from}-${report.to}.csv` });
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <section className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3 rounded-3xl border border-zinc-800 bg-zinc-900 p-4 sm:p-6">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex gap-1 rounded-xl border border-zinc-700 bg-zinc-950 p-1" role="group" aria-label="Kỳ báo cáo">
            {REPORT_PERIODS.map((period) => (
              <button key={period.days} type="button" aria-pressed={days === period.days} onClick={() => setDays(period.days)}
                className={`rounded-lg px-3 py-1.5 text-sm ${days === period.days ? "bg-amber-500/15 text-amber-200" : "text-zinc-400 hover:text-zinc-200"}`}>
                {period.label}
              </button>
            ))}
          </div>
          {isGlobal
            ? <StoreSelector stores={stores} value={storeId} onChange={setStoreId} includeAll loading={storesLoading} label="" className={control} />
            : <span className="text-sm text-zinc-400">{currentStoreName || "Cửa hàng của bạn"}</span>}
        </div>
        <Button type="button" disabled={!report?.products?.length} onClick={download}
          className="inline-flex h-11 items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-800 px-4 text-sm text-zinc-200 hover:border-amber-500 hover:text-amber-300 disabled:opacity-50">
          <Download size={16} />Xuất CSV
        </Button>
      </div>

      {result.error && !loading && <p role="alert" className="rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-sm text-rose-200">{result.error}</p>}

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <Stat label="Món đã bán" value={summary ? count(summary.soldQuantity) : "…"} hint="Đơn hoàn thành tạo trong kỳ" />
        <Stat label="Món đổi / trả" value={summary ? count(summary.returnedQuantity) : "…"}
          hint={summary ? `${formatRate(summary.returnRate)} số món đã bán` : ""} />
        <Stat label="Không vừa size" value={summary ? count(summary.wrongSizeQuantity) : "…"}
          hint={summary ? `${formatRate(summary.wrongSizeRate)} số món đã bán` : ""} tone={rateTone(summary?.wrongSizeRate)} />
        <Stat label="Sản phẩm cần xem lại bảng size" value={report ? count(report.flaggedProducts) : "…"}
          hint="Từ 2 món và từ 10% không vừa size" tone={report?.flaggedProducts ? "text-rose-300" : "text-zinc-100"} />
      </div>

      {report?.reasons?.length > 0 && (
        <div className="flex flex-wrap gap-2 text-xs">
          {report.reasons.map((reason) => (
            <span key={reason.code} className={`rounded-full border px-3 py-1 ${reason.code === "WRONG_SIZE" ? "border-amber-400/40 bg-amber-500/10 text-amber-200" : "border-zinc-700 text-zinc-400"}`}>
              {reason.label}: {count(reason.quantity)}
            </span>
          ))}
        </div>
      )}

      <div className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[900px] text-sm">
            <thead className="bg-zinc-800/70 text-zinc-300">
              <tr>{["Sản phẩm", "Đã bán", "Đổi / trả", "Không vừa size", "Xu hướng", ""].map((head) =>
                <th key={head} className="px-4 py-3 text-left font-semibold">{head}</th>)}</tr>
            </thead>
            <tbody>
              {loading && <tr><td colSpan="6" className="px-4 py-12 text-center text-zinc-400">Đang tải…</td></tr>}
              {report && report.products.length === 0 && (
                <tr><td colSpan="6" className="px-4 py-12 text-center text-zinc-400">Chưa có món nào bị đổi / trả trong kỳ này.</td></tr>
              )}
              {report?.products.map((product) => {
                const open = !!expanded[product.productId];
                return (
                  <Fragment key={product.productId}>
                    <tr className="cursor-pointer border-t border-zinc-800 align-top transition-colors hover:bg-zinc-800/30" onClick={() => toggle(product.productId)}>
                      <td className="px-4 py-3">
                        <div className="flex items-start gap-2">
                          {open ? <ChevronDown size={16} className="mt-0.5 text-zinc-500" /> : <ChevronRight size={16} className="mt-0.5 text-zinc-500" />}
                          <div>
                            <p className="font-semibold text-zinc-100">{product.productName || "Sản phẩm đã xóa"}</p>
                            <p className="text-xs text-zinc-500">{product.categoryName || "Chưa có danh mục"}</p>
                            {product.flagged && <p className="mt-1 inline-flex items-center gap-1 text-xs text-rose-300"><AlertTriangle size={13} />Nên xem lại bảng size</p>}
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-zinc-300">{count(product.soldQuantity)}</td>
                      <td className="px-4 py-3 text-zinc-300">{count(product.returnedQuantity)} <span className="text-zinc-500">· {formatRate(product.returnRate)}</span></td>
                      <td className={`px-4 py-3 font-semibold ${rateTone(product.wrongSizeRate)}`}>{count(product.wrongSizeQuantity)} · {formatRate(product.wrongSizeRate)}</td>
                      <td className="px-4 py-3 text-xs text-zinc-300">{FIT_HINT_LABELS[product.fitHint] || <span className="text-zinc-500">Chưa rõ</span>}</td>
                      <td className="px-4 py-3 text-right">
                        {product.categoryId && (
                          <Button permission="CATEGORY_VIEW" type="button" title="Mở bảng size của danh mục"
                            onClick={(event) => { event.stopPropagation(); setSizeChartFor({ id: product.categoryId, name: product.categoryName }); }}
                            className="inline-flex items-center gap-1 text-amber-300 hover:text-amber-100">
                            <Ruler size={15} />Bảng size
                          </Button>
                        )}
                      </td>
                    </tr>
                    {open && (
                      <tr className="border-t border-zinc-800/60 bg-zinc-950/60">
                        <td colSpan="6" className="px-4 py-3">
                          <table className="w-full text-xs">
                            <thead className="text-zinc-500">
                              <tr>{["Size", "Đã bán", "Đổi / trả", "Không vừa size", "Đổi sang", "Xu hướng"].map((head) =>
                                <th key={head} className="py-1.5 pr-4 text-left font-medium">{head}</th>)}</tr>
                            </thead>
                            <tbody>
                              {product.sizes.map((size) => (
                                <tr key={size.size} className="border-t border-zinc-800/60">
                                  <td className="py-1.5 pr-4 font-semibold text-zinc-200">{size.size}</td>
                                  <td className="py-1.5 pr-4 text-zinc-300">{count(size.soldQuantity)}</td>
                                  <td className="py-1.5 pr-4 text-zinc-300">{count(size.returnedQuantity)} · {formatRate(size.returnRate)}</td>
                                  <td className={`py-1.5 pr-4 ${rateTone(size.wrongSizeRate)}`}>{count(size.wrongSizeQuantity)} · {formatRate(size.wrongSizeRate)}</td>
                                  <td className="py-1.5 pr-4 text-zinc-300">{size.exchanges.length ? size.exchanges.map((flow) => `${flow.toSize} ×${flow.quantity}`).join(", ") : "—"}</td>
                                  <td className="py-1.5 pr-4 text-zinc-400">{FIT_HINT_LABELS[size.fitHint] || "—"}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {sizeChartFor && <SizeChartDialog category={sizeChartFor} onClose={() => setSizeChartFor(null)} />}
    </section>
  );
}
