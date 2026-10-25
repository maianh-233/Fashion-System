import Button from "../../components/common/Button";
import { useEffect, useState } from "react";
import {
  ArrowDownRight,
  ArrowUpRight,
  Boxes,
  CircleDollarSign,
  Download,
  Minus,
  Package,
  Percent,
  RefreshCw,
  ShoppingBag,
  Truck,
  UserPlus,
  Warehouse,
} from "lucide-react";
import {
  Cell,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import StoreSelector, { ALL_STORES } from "../../components/common/StoreSelector";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import useStoreOptions from "../../hooks/useStoreOptions";
import { statisticsApi } from "../../api/admin/statisticsApi";
import { compactMoney, count, money, variantLabel } from "./dashboardLogic";
import {
  ORDER_TYPE_LABELS, ORDER_TYPE_OPTIONS, PERIOD_OPTIONS, TABLES, changeVsPreviousPeriod, downloadCsv, monthLabel,
  paymentLabel, rangeLabel, rateChange, reportSections, seriesLabel, shares, statusGroups,
} from "./statisticsLogic";

const SELECT_CLASS = "rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm text-zinc-100 outline-none transition focus:border-amber-500";
const CARD_CLASS = "rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20";
const EXPORT_BUTTON_CLASS = "inline-flex items-center gap-2 rounded-xl border border-amber-500/40 bg-amber-500/15 px-3 py-2 text-xs font-semibold text-amber-300 transition hover:bg-amber-500 hover:text-zinc-950";
const TONE_TEXT = { up: "text-emerald-400", down: "text-rose-400", flat: "text-zinc-400" };
const TONE_ICON = { up: ArrowUpRight, down: ArrowDownRight, flat: Minus };

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) {
    return null;
  }

  return (
    <div className="rounded-2xl border border-zinc-700 bg-zinc-900/95 px-4 py-3 shadow-lg backdrop-blur">
      <p className="text-sm text-zinc-300">{label}</p>
      <p className="mt-1 text-base font-semibold text-amber-400">{money(payload[0].value)}</p>
      <p className="text-xs text-zinc-400">{count(payload[0].payload.orders)} đơn</p>
    </div>
  );
}

function ChangeLine({ change }) {
  if (!change) return <p className="mt-4 text-sm text-zinc-500">Chưa đủ dữ liệu để so sánh</p>;
  const Icon = TONE_ICON[change.tone];
  return (
    <p className={`mt-4 inline-flex items-center gap-1 text-sm font-medium ${TONE_TEXT[change.tone]}`}>
      <Icon size={16} />
      {change.text.replace(/^[↑↓] /, "")}
    </p>
  );
}

function ExportButton({ onClick }) {
  return (
    <Button permission="STATISTICS_EXPORT" type="button" onClick={onClick} className={EXPORT_BUTTON_CLASS}>
      <Download size={14} />
      Xuất Excel
    </Button>
  );
}

function Empty({ children }) {
  return <p className="px-4 py-6 text-center text-sm text-zinc-500">{children}</p>;
}

function ShareList({ rows }) {
  if (rows.length === 0) return <Empty>Chưa có đơn trong kỳ.</Empty>;
  return (
    <div className="mt-6 space-y-4">
      {rows.map((item) => (
        <div key={item.key} className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4 transition hover:border-zinc-700">
          <div className="flex items-center justify-between">
            <p className="text-sm text-zinc-400">{item.label}</p>
            <p className="text-xl font-semibold text-zinc-100">{item.percent.toLocaleString("vi-VN")}%</p>
          </div>
          <p className="mt-1 text-sm text-zinc-500">{count(item.orders)} đơn · {compactMoney(item.revenue)} đ</p>
        </div>
      ))}
    </div>
  );
}

export default function StatisticsManagement() {
  const { isGlobal, currentStoreName, loading: permissionsLoading } = useAdminPermissions();
  const { stores, loading: storesLoading } = useStoreOptions({ endpoint: statisticsApi.STORES_PATH, enabled: isGlobal });
  const [period, setPeriod] = useState("LAST_30_DAYS");
  const [orderType, setOrderType] = useState("");
  const [storeId, setStoreId] = useState(ALL_STORES);
  const [revision, setRevision] = useState(0);
  // Kết quả gắn với khóa yêu cầu; đang tải khi khóa hiện tại chưa có kết quả.
  const [result, setResult] = useState({ key: null, data: null, error: "" });
  const store = isGlobal && storeId !== ALL_STORES ? storeId : "";
  const requestKey = permissionsLoading ? null : [store, period, orderType, revision].join("|");

  useEffect(() => {
    if (requestKey === null) return undefined;
    const controller = new AbortController();
    statisticsApi.report({ storeId: store, period, orderType }, { signal: controller.signal })
      .then(data => { if (!controller.signal.aborted) setResult({ key: requestKey, data, error: "" }); })
      .catch(failure => {
        if (!controller.signal.aborted) setResult(current => ({
          key: requestKey, data: current.data, error: failure.message || "Không thể tải báo cáo thống kê.",
        }));
      });
    return () => controller.abort();
  // requestKey đã gồm store, period, orderType và revision.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestKey]);

  const { data: report, error } = result;
  const loading = result.key !== requestKey;
  const showStore = isGlobal && storeId === ALL_STORES;
  const fileSuffix = report ? `${report.from}_${report.to}` : "";

  const exportTable = (name) => downloadCsv(`thong-ke-${name}-${fileSuffix}.csv`, [TABLES[name](report)]);

  const o = report?.overview;
  const overviewCards = o ? [
    {
      key: "revenue", title: "Doanh thu", value: `${compactMoney(o.revenue)} đ`,
      change: changeVsPreviousPeriod(o.revenue, o.previousRevenue),
      icon: CircleDollarSign, iconClass: "text-amber-500 bg-amber-500/10",
    },
    {
      key: "orders", title: "Tổng đơn hàng", value: count(o.orders),
      change: changeVsPreviousPeriod(o.orders, o.previousOrders),
      icon: ShoppingBag, iconClass: "text-sky-400 bg-sky-500/10",
    },
    {
      key: "profit", title: "Lợi nhuận gộp (ước tính)", value: `${compactMoney(o.grossProfit)} đ`,
      change: changeVsPreviousPeriod(o.grossProfit, o.previousGrossProfit),
      note: o.uncostedQuantity > 0 ? `${count(o.uncostedQuantity)} sản phẩm đã bán chưa có giá nhập` : "",
      icon: Percent, iconClass: "text-emerald-400 bg-emerald-500/10",
    },
    {
      key: "new-customers", title: "Khách hàng mới", value: count(o.newCustomers),
      change: changeVsPreviousPeriod(o.newCustomers, o.previousNewCustomers),
      icon: UserPlus, iconClass: "text-violet-400 bg-violet-500/10",
    },
  ] : [];

  const series = report?.revenueSeries.map(point => ({
    label: seriesLabel(point.label, report.granularity),
    revenue: Number(point.revenue || 0),
    orders: point.orders,
  })) || [];
  const pieData = statusGroups(report?.statusCounts);
  const retention = report?.retention;

  return (
    <div className="admin-statistics-page admin-catalog-page bg-zinc-950 text-zinc-100">
      <AdminCatalogPageHeader
        icon={CircleDollarSign}
        eyebrow="Phân tích kinh doanh"
        title="Báo cáo & thống kê"
        description="Theo dõi doanh thu, đơn hàng, khách hàng và hiệu quả tồn kho theo kỳ."
      />
      {/* Filter Bar */}
      <section className="admin-catalog-toolbar admin-statistics-toolbar rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
          <div className="grid flex-1 grid-cols-1 gap-3 md:grid-cols-3">
            <select aria-label="Kỳ báo cáo" value={period} onChange={(event) => setPeriod(event.target.value)} className={SELECT_CLASS}>
              {PERIOD_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>

            <select aria-label="Loại đơn" value={orderType} onChange={(event) => setOrderType(event.target.value)} className={SELECT_CLASS}>
              {ORDER_TYPE_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>

            {isGlobal ? (
              <StoreSelector
                stores={stores}
                value={storeId}
                onChange={setStoreId}
                includeAll
                loading={storesLoading}
                label=""
                className={SELECT_CLASS}
              />
            ) : (
              <p className={`${SELECT_CLASS} text-zinc-400`}>{currentStoreName || "Cửa hàng của bạn"}</p>
            )}
          </div>

          <div className="flex flex-wrap gap-3">
            <Button
              permission="STATISTICS_EXPORT"
              type="button"
              disabled={!report}
              onClick={() => downloadCsv(`bao-cao-thong-ke-${fileSuffix}.csv`, reportSections(report))}
              className="inline-flex items-center gap-2 rounded-2xl border border-zinc-700 bg-zinc-800 px-5 py-3 text-sm font-medium text-zinc-200 transition hover:border-amber-500 hover:text-amber-400 disabled:opacity-50"
            >
              <Download size={16} />
              Xuất Excel
            </Button>
            <Button
              type="button"
              disabled={loading}
              onClick={() => setRevision(value => value + 1)}
              className="inline-flex items-center gap-2 rounded-2xl bg-amber-500 px-5 py-3 text-sm font-semibold text-zinc-950 transition hover:bg-amber-400 disabled:opacity-60"
            >
              <RefreshCw size={16} className={loading ? "animate-spin" : ""} />
              Làm mới
            </Button>
          </div>
        </div>
        {report && (
          <p className="mt-4 text-sm text-zinc-400">
            Kỳ này: {rangeLabel(report.from, report.to)} · So sánh với: {rangeLabel(report.previousFrom, report.previousTo)}
            {" · "}{report.storeName || "Toàn hệ thống"}
          </p>
        )}
      </section>

      {error && <p className="rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">{error}</p>}
      {loading && !report && !error && <p className="text-sm text-zinc-400">Đang tải báo cáo…</p>}

      {report && (
        <div className={`contents${loading ? " [&>*]:opacity-60" : ""}`}>
          {/* Overview Stats */}
          <section className="admin-catalog-stats admin-statistics-overview grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
            {overviewCards.map((item) => {
              const Icon = item.icon;
              return (
                <article
                  key={item.key}
                  className="group rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20 transition hover:-translate-y-0.5 hover:border-zinc-700"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <p className="text-sm text-zinc-400">{item.title}</p>
                      <p className="mt-2 text-3xl font-bold text-zinc-100">{item.value}</p>
                    </div>
                    <div className={`rounded-2xl p-3 ${item.iconClass}`}>
                      <Icon size={22} />
                    </div>
                  </div>
                  <ChangeLine change={item.change} />
                  {item.note && <p className="mt-1 text-xs text-amber-300/80">{item.note}</p>}
                </article>
              );
            })}
          </section>

          {/* Revenue Chart */}
          <section className="admin-statistics-card admin-statistics-card--revenue rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-xl font-semibold text-zinc-100">Biểu đồ doanh thu</h2>
              <p className="text-sm text-zinc-400">Đơn hoàn thành · theo {report.granularity === "MONTH" ? "tháng" : "ngày"}</p>
            </div>
            <div className="h-80 w-full">
              <ResponsiveContainer>
                <LineChart data={series}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#3f3f46" vertical={false} />
                  <XAxis dataKey="label" stroke="#a1a1aa" tickLine={false} axisLine={false} minTickGap={16} />
                  <YAxis
                    stroke="#a1a1aa"
                    tickLine={false}
                    axisLine={false}
                    allowDecimals={false}
                    domain={[0, max => Math.max(max, 1000000)]}
                    tickFormatter={(value) => compactMoney(value)}
                    width={72}
                  />
                  <Tooltip content={<ChartTooltip />} />
                  <Line
                    type="monotone"
                    dataKey="revenue"
                    stroke="#f59e0b"
                    strokeWidth={3}
                    dot={series.length <= 31 ? { r: 3, strokeWidth: 2, fill: "#18181b" } : false}
                    activeDot={{ r: 6 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </section>

          {/* Order Analytics */}
          <section className="admin-statistics-grid grid grid-cols-1 gap-5 xl:grid-cols-3">
            <article className={CARD_CLASS}>
              <h3 className="text-lg font-semibold text-zinc-100">Trạng thái đơn hàng</h3>
              {pieData.length === 0 ? <Empty>Chưa có đơn trong kỳ.</Empty> : (
                <div className="mt-4 h-72">
                  <ResponsiveContainer>
                    <PieChart>
                      <Pie data={pieData} dataKey="value" nameKey="name" cy="45%" innerRadius={50} outerRadius={80} paddingAngle={3}>
                        {pieData.map((entry) => (
                          <Cell key={entry.name} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip
                        formatter={(value) => [`${count(value)} đơn`]}
                        contentStyle={{
                          borderRadius: 14,
                          border: "1px solid #3f3f46",
                          backgroundColor: "#18181b",
                          color: "#e4e4e7",
                        }}
                      />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}
            </article>

            <article className={CARD_CLASS}>
              <h3 className="text-lg font-semibold text-zinc-100">Online vs Tại cửa hàng</h3>
              <ShareList rows={shares(report.byOrderType, key => ORDER_TYPE_LABELS[key] || key)} />
            </article>

            <article className={CARD_CLASS}>
              <h3 className="text-lg font-semibold text-zinc-100">Phương thức thanh toán</h3>
              <ShareList rows={shares(report.byPaymentMethod, paymentLabel)} />
            </article>
          </section>

          {/* Product Analytics */}
          <section className="admin-statistics-grid grid grid-cols-1 gap-5 xl:grid-cols-2">
            <article className={CARD_CLASS}>
              <div className="flex items-center justify-between gap-3">
                <h3 className="text-lg font-semibold text-zinc-100">Top sản phẩm bán chạy</h3>
                <ExportButton onClick={() => exportTable("topProducts")} />
              </div>
              <div className="mt-5 overflow-x-auto rounded-2xl border border-zinc-800">
                <table className="min-w-full text-sm">
                  <thead className="bg-zinc-950 text-zinc-400">
                    <tr>
                      <th className="px-4 py-3 text-left font-medium">Sản phẩm</th>
                      <th className="px-4 py-3 text-right font-medium">Đã bán</th>
                      <th className="px-4 py-3 text-right font-medium">Doanh thu</th>
                      <th className="px-4 py-3 text-right font-medium">Tồn kho</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800">
                    {report.topProducts.map((item) => (
                      <tr key={item.productId || item.productName} className="transition hover:bg-zinc-800/60">
                        <td className="px-4 py-3 text-zinc-100">{item.productName}</td>
                        <td className="px-4 py-3 text-right text-zinc-300">{count(item.sold)}</td>
                        <td className="px-4 py-3 text-right font-medium text-amber-400">{compactMoney(item.revenue)} đ</td>
                        <td className="px-4 py-3 text-right text-zinc-300">{count(item.stock)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {report.topProducts.length === 0 && <Empty>Chưa có đơn hoàn thành trong kỳ.</Empty>}
              </div>
            </article>

            <article className={CARD_CLASS}>
              <div className="flex items-center justify-between gap-3">
                <div>
                  <h3 className="text-lg font-semibold text-zinc-100">Sản phẩm sắp hết hàng</h3>
                  <p className="text-xs text-zinc-500">Còn 1–5 sản phẩm (tồn quầy + online), tồn hiện tại</p>
                </div>
                <ExportButton onClick={() => exportTable("lowStock")} />
              </div>
              <div className="mt-5 max-h-72 overflow-auto rounded-2xl border border-zinc-800 [scrollbar-color:#52525b_#18181b] [scrollbar-width:thin]">
                <table className="min-w-full text-sm">
                  <thead className="sticky top-0 bg-zinc-950 text-zinc-400">
                    <tr>
                      <th className="px-4 py-3 text-left font-medium">SKU</th>
                      <th className="px-4 py-3 text-left font-medium">Sản phẩm</th>
                      {showStore && <th className="px-4 py-3 text-left font-medium">Cửa hàng</th>}
                      <th className="px-4 py-3 text-right font-medium">Tồn kho</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800">
                    {report.lowStock.map((item) => (
                      <tr key={`${item.storeId}-${item.productVariantId}`} className="transition hover:bg-zinc-800/60">
                        <td className="px-4 py-3 text-zinc-300"><p className="max-w-[10rem] truncate" title={item.sku}>{item.sku}</p></td>
                        <td className="px-4 py-3 text-zinc-100">{variantLabel(item)}</td>
                        {showStore && <td className="px-4 py-3 text-zinc-400">{item.storeName}</td>}
                        <td className="px-4 py-3 text-right font-medium text-rose-400">{item.quantity}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {report.lowStock.length === 0 && <Empty>Chưa có sản phẩm nào sắp hết hàng.</Empty>}
              </div>
            </article>
          </section>

          {/* Customer Analytics */}
          <section className="admin-statistics-grid grid grid-cols-1 gap-5 xl:grid-cols-3">
            <article className={`${CARD_CLASS} xl:col-span-2`}>
              <h3 className="text-lg font-semibold text-zinc-100">Khách hàng mới theo tháng</h3>
              <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
                {report.newCustomersByMonth.map((item) => (
                  <div
                    key={item.month}
                    className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4 text-center transition hover:border-zinc-700"
                  >
                    <p className="text-sm text-zinc-400">{monthLabel(item.month)}</p>
                    <p className="mt-2 text-2xl font-semibold text-zinc-100">{count(item.count)}</p>
                  </div>
                ))}
              </div>
            </article>

            <article className={CARD_CLASS}>
              <h3 className="text-lg font-semibold text-zinc-100">Tỷ lệ khách quay lại</h3>
              <div className="mt-8 rounded-2xl border border-zinc-800 bg-zinc-950/70 p-5">
                <p className="text-4xl font-bold text-amber-400">
                  {retention.rate === null ? "—" : `${retention.rate.toLocaleString("vi-VN")}%`}
                </p>
                {(() => {
                  const change = rateChange(retention.rate, retention.previousRate);
                  return change
                    ? <p className={`mt-2 text-sm ${TONE_TEXT[change.tone]}`}>{change.text}</p>
                    : <p className="mt-2 text-sm text-zinc-500">Chưa đủ dữ liệu để so sánh</p>;
                })()}
                <p className="mt-4 text-sm text-zinc-500">
                  {count(retention.returningCustomers)} / {count(retention.activeCustomers)} khách mua trong kỳ đã có từ 2 đơn trở lên.
                </p>
              </div>
            </article>
          </section>

          <section className="admin-statistics-card rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-lg font-semibold text-zinc-100">Top khách VIP</h3>
              <ExportButton onClick={() => exportTable("vipCustomers")} />
            </div>
            <div className="mt-5 overflow-x-auto rounded-2xl border border-zinc-800">
              <table className="min-w-full text-sm">
                <thead className="bg-zinc-950 text-zinc-400">
                  <tr>
                    <th className="px-4 py-3 text-left font-medium">Khách hàng</th>
                    <th className="px-4 py-3 text-left font-medium">Hạng</th>
                    <th className="px-4 py-3 text-right font-medium">Số đơn</th>
                    <th className="px-4 py-3 text-right font-medium">Chi tiêu</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800">
                  {report.vipCustomers.map((item) => (
                    <tr key={item.id} className="transition hover:bg-zinc-800/60">
                      <td className="px-4 py-3 text-zinc-100">
                        {item.fullName || "Khách chưa có tên"}
                        <p className="text-xs text-zinc-500">{item.customerCode}</p>
                      </td>
                      <td className="px-4 py-3">
                        <span className="rounded-full border border-amber-500/30 bg-amber-500/10 px-3 py-1 text-xs font-medium text-amber-400">
                          {item.tier || "Chưa xếp hạng"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right text-zinc-300">{count(item.orders)}</td>
                      <td className="px-4 py-3 text-right font-medium text-amber-400">{compactMoney(item.spend)} đ</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {report.vipCustomers.length === 0 && <Empty>Chưa có khách mua hàng trong kỳ.</Empty>}
            </div>
          </section>

          {/* Inventory Analytics */}
          <section className="admin-statistics-inventory grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
            {[
              { title: "Tổng nhập kho", value: count(report.inventory.importedQuantity), unit: "sản phẩm · phiếu hoàn thành trong kỳ", icon: Boxes },
              { title: "Tổng xuất kho", value: count(report.inventory.exportedQuantity), unit: "sản phẩm · phiếu hoàn thành trong kỳ", icon: Truck },
              {
                title: "Giá trị tồn kho", value: compactMoney(report.inventory.stockValue),
                unit: `VNĐ theo giá nhập · ${count(report.inventory.stockQuantity)} sản phẩm`
                  + (report.inventory.uncostedVariants > 0 ? ` · ${count(report.inventory.uncostedVariants)} mã chưa có giá nhập` : ""),
                icon: Warehouse,
              },
              { title: "Tồn chưa bán được", value: count(report.inventory.unsoldVariants), unit: "mã còn hàng nhưng không bán trong kỳ", icon: Package },
            ].map((item) => {
              const Icon = item.icon;
              return (
                <article
                  key={item.title}
                  className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20 transition hover:border-zinc-700"
                >
                  <div className="flex items-center justify-between">
                    <p className="text-sm text-zinc-400">{item.title}</p>
                    <div className="rounded-xl bg-zinc-800 p-2 text-amber-400">
                      <Icon size={18} />
                    </div>
                  </div>
                  <p className="mt-3 text-3xl font-bold text-zinc-100">{item.value}</p>
                  <p className="mt-1 text-sm text-zinc-500">{item.unit}</p>
                </article>
              );
            })}
          </section>
        </div>
      )}
    </div>
  );
}
