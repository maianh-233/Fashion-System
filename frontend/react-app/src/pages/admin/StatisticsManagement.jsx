import { useMemo, useState } from "react";
import {
  ArrowDownRight,
  ArrowUpRight,
  Boxes,
  CircleDollarSign,
  Download,
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

const PERIOD_OPTIONS = [
  "7 ngày gần nhất",
  "30 ngày gần nhất",
  "Quý này",
  "12 tháng gần nhất",
];

const ORDER_TYPE_OPTIONS = [
  "Tất cả đơn hàng",
  "Đơn online",
  "Đơn tại cửa hàng",
  "Đơn sỉ",
];

const BRANCH_OPTIONS = [
  "Tất cả chi nhánh",
  "LUNARIA Flagship Q1",
  "LUNARIA Crescent Mall",
  "LUNARIA Online Hub",
];

const OVERVIEW_STATS = [
  {
    key: "revenue",
    title: "Doanh thu",
    value: "4.82 tỷ",
    change: 14.3,
    icon: CircleDollarSign,
    iconClass: "text-amber-500 bg-amber-500/10",
  },
  {
    key: "orders",
    title: "Tổng đơn hàng",
    value: "8,421",
    change: 9.1,
    icon: ShoppingBag,
    iconClass: "text-sky-400 bg-sky-500/10",
  },
  {
    key: "profit",
    title: "Lợi nhuận",
    value: "1.26 tỷ",
    change: 7.6,
    icon: Percent,
    iconClass: "text-emerald-400 bg-emerald-500/10",
  },
  {
    key: "new-customers",
    title: "Khách hàng mới",
    value: "1,198",
    change: -2.4,
    icon: UserPlus,
    iconClass: "text-violet-400 bg-violet-500/10",
  },
];

const MONTHLY_REVENUE = [
  { month: "T1", revenue: 280 },
  { month: "T2", revenue: 320 },
  { month: "T3", revenue: 360 },
  { month: "T4", revenue: 345 },
  { month: "T5", revenue: 410 },
  { month: "T6", revenue: 435 },
  { month: "T7", revenue: 470 },
  { month: "T8", revenue: 455 },
  { month: "T9", revenue: 495 },
  { month: "T10", revenue: 540 },
  { month: "T11", revenue: 575 },
  { month: "T12", revenue: 620 },
];

const ORDER_STATUS_DATA = [
  { name: "Hoàn thành", value: 62, color: "#f59e0b" },
  { name: "Đang xử lý", value: 24, color: "#38bdf8" },
  { name: "Đã hủy", value: 14, color: "#ef4444" },
];

const ONLINE_OFFLINE_DATA = [
  { label: "Online", value: "68%", hint: "5,726 đơn" },
  { label: "Offline", value: "32%", hint: "2,695 đơn" },
];

const PICKUP_DELIVERY_DATA = [
  { label: "Pickup", value: "27%", hint: "2,273 đơn" },
  { label: "Delivery", value: "73%", hint: "6,148 đơn" },
];

const TOP_PRODUCTS = [
  { name: "LUNARIA Silk Dress", sold: 812, revenue: "486.2M", stock: 124 },
  { name: "Moonlight Blazer", sold: 754, revenue: "392.4M", stock: 88 },
  { name: "Velvet Pleated Skirt", sold: 689, revenue: "338.1M", stock: 52 },
  { name: "Aurora Knit Top", sold: 621, revenue: "248.6M", stock: 41 },
];

const LOW_STOCK_PRODUCTS = [
  { sku: "LNA-DR-102", name: "Rosaline Maxi Dress", stock: 6, threshold: 20 },
  { sku: "LNA-BL-083", name: "Noir Edge Blazer", stock: 9, threshold: 25 },
  { sku: "LNA-SK-044", name: "Ivory Wrap Skirt", stock: 11, threshold: 30 },
  { sku: "LNA-TS-137", name: "Fleur Lace Top", stock: 7, threshold: 20 },
];

const NEW_CUSTOMERS_BY_MONTH = [
  { month: "T1", value: 62 },
  { month: "T2", value: 74 },
  { month: "T3", value: 88 },
  { month: "T4", value: 92 },
  { month: "T5", value: 107 },
  { month: "T6", value: 118 },
];

const VIP_CUSTOMERS = [
  { name: "Nguyễn Ngọc Anh", tier: "Platinum", spend: "219.8M", orders: 51 },
  { name: "Trần Hoài Thương", tier: "Gold", spend: "186.2M", orders: 44 },
  { name: "Lê Minh Châu", tier: "Gold", spend: "172.5M", orders: 39 },
  { name: "Phạm Quỳnh Mai", tier: "Silver", spend: "140.7M", orders: 35 },
];

const INVENTORY_STATS = [
  { title: "Tổng nhập kho", value: "18,240", unit: "sản phẩm", icon: Boxes },
  { title: "Tổng xuất kho", value: "15,980", unit: "sản phẩm", icon: Truck },
  { title: "Giá trị tồn kho", value: "3.92 tỷ", unit: "VNĐ", icon: Warehouse },
  { title: "Dead stock products", value: "42", unit: "mã sản phẩm", icon: Package },
];

function formatMoney(value) {
  return `${value.toLocaleString("vi-VN")} triệu`;
}

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) {
    return null;
  }

  return (
    <div className="rounded-2xl border border-zinc-700 bg-zinc-900/95 px-4 py-3 shadow-lg backdrop-blur">
      <p className="text-sm text-zinc-300">{`Tháng ${label}`}</p>
      <p className="mt-1 text-base font-semibold text-amber-400">{formatMoney(payload[0].value)}</p>
    </div>
  );
}

export default function StatisticsManagement() {
  const [period, setPeriod] = useState(PERIOD_OPTIONS[3]);
  const [orderType, setOrderType] = useState(ORDER_TYPE_OPTIONS[0]);
  const [branch, setBranch] = useState(BRANCH_OPTIONS[0]);

  const overviewCards = useMemo(() => OVERVIEW_STATS, []);
  const topProducts = useMemo(() => TOP_PRODUCTS, []);
  const lowStockProducts = useMemo(() => LOW_STOCK_PRODUCTS, []);
  const vipCustomers = useMemo(() => VIP_CUSTOMERS, []);

  const handleRefresh = () => {
    window.alert("Đã làm mới dữ liệu thống kê.");
  };

  const handleExport = () => {
    window.alert("Đang xuất file Excel...");
  };

  const handleTableExport = (tableName) => {
    window.alert(`Đang xuất Excel cho bảng: ${tableName}`);
  };

  return (
    <div className="space-y-8 bg-zinc-950 text-zinc-100">
      {/* Filter Bar */}
      <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
          <div className="grid flex-1 grid-cols-1 gap-3 md:grid-cols-3">
            <select
              value={period}
              onChange={(event) => setPeriod(event.target.value)}
              className="rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm text-zinc-100 outline-none transition focus:border-amber-500"
            >
              {PERIOD_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>

            <select
              value={orderType}
              onChange={(event) => setOrderType(event.target.value)}
              className="rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm text-zinc-100 outline-none transition focus:border-amber-500"
            >
              {ORDER_TYPE_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>

            <select
              value={branch}
              onChange={(event) => setBranch(event.target.value)}
              className="rounded-2xl border border-zinc-700 bg-zinc-800 px-4 py-3 text-sm text-zinc-100 outline-none transition focus:border-amber-500"
            >
              {BRANCH_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={handleExport}
              className="inline-flex items-center gap-2 rounded-2xl border border-zinc-700 bg-zinc-800 px-5 py-3 text-sm font-medium text-zinc-200 transition hover:border-amber-500 hover:text-amber-400"
            >
              <Download size={16} />
              Export Excel
            </button>
            <button
              type="button"
              onClick={handleRefresh}
              className="inline-flex items-center gap-2 rounded-2xl bg-amber-500 px-5 py-3 text-sm font-semibold text-zinc-950 transition hover:bg-amber-400"
            >
              <RefreshCw size={16} />
              Refresh Data
            </button>
          </div>
        </div>
      </section>

      {/* Overview Stats */}
      <section className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
        {overviewCards.map((item) => {
          const Icon = item.icon;
          const isPositive = item.change >= 0;

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
              <p
                className={`mt-4 inline-flex items-center gap-1 text-sm font-medium ${
                  isPositive ? "text-emerald-400" : "text-rose-400"
                }`}
              >
                {isPositive ? <ArrowUpRight size={16} /> : <ArrowDownRight size={16} />}
                {Math.abs(item.change)}% so với kỳ trước
              </p>
            </article>
          );
        })}
      </section>

      {/* Revenue Chart */}
      <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
        <div className="mb-6 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-zinc-100">Biểu đồ doanh thu</h2>
          <p className="text-sm text-zinc-400">Đơn vị: triệu VNĐ</p>
        </div>
        <div className="h-80 w-full">
          <ResponsiveContainer>
            <LineChart data={MONTHLY_REVENUE}>
              <CartesianGrid strokeDasharray="3 3" stroke="#3f3f46" vertical={false} />
              <XAxis dataKey="month" stroke="#a1a1aa" tickLine={false} axisLine={false} />
              <YAxis
                stroke="#a1a1aa"
                tickLine={false}
                axisLine={false}
                tickFormatter={(value) => `${value}`}
              />
              <Tooltip content={<ChartTooltip />} />
              <Line
                type="monotone"
                dataKey="revenue"
                stroke="#f59e0b"
                strokeWidth={3}
                dot={{ r: 4, strokeWidth: 2, fill: "#18181b" }}
                activeDot={{ r: 6 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </section>

      {/* Order Analytics */}
      <section className="grid grid-cols-1 gap-5 xl:grid-cols-3">
        <article className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
          <h3 className="text-lg font-semibold text-zinc-100">Trạng thái đơn hàng</h3>
          <div className="mt-4 h-64">
            <ResponsiveContainer>
              <PieChart>
                <Pie
                  data={ORDER_STATUS_DATA}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={55}
                  outerRadius={90}
                  paddingAngle={3}
                >
                  {ORDER_STATUS_DATA.map((entry) => (
                    <Cell key={entry.name} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
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
        </article>

        <article className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
          <h3 className="text-lg font-semibold text-zinc-100">Online vs Offline</h3>
          <div className="mt-6 space-y-4">
            {ONLINE_OFFLINE_DATA.map((item) => (
              <div
                key={item.label}
                className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4 transition hover:border-zinc-700"
              >
                <div className="flex items-center justify-between">
                  <p className="text-sm text-zinc-400">{item.label}</p>
                  <p className="text-xl font-semibold text-zinc-100">{item.value}</p>
                </div>
                <p className="mt-1 text-sm text-zinc-500">{item.hint}</p>
              </div>
            ))}
          </div>
        </article>

        <article className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
          <h3 className="text-lg font-semibold text-zinc-100">Pickup vs Delivery</h3>
          <div className="mt-6 space-y-4">
            {PICKUP_DELIVERY_DATA.map((item) => (
              <div
                key={item.label}
                className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4 transition hover:border-zinc-700"
              >
                <div className="flex items-center justify-between">
                  <p className="text-sm text-zinc-400">{item.label}</p>
                  <p className="text-xl font-semibold text-zinc-100">{item.value}</p>
                </div>
                <p className="mt-1 text-sm text-zinc-500">{item.hint}</p>
              </div>
            ))}
          </div>
        </article>
      </section>

      {/* Product Analytics */}
      <section className="grid grid-cols-1 gap-5 xl:grid-cols-2">
        <article className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
          <div className="flex items-center justify-between gap-3">
            <h3 className="text-lg font-semibold text-zinc-100">Top sản phẩm bán chạy</h3>
            <button
              type="button"
              onClick={() => handleTableExport("Top sản phẩm bán chạy")}
              className="inline-flex items-center gap-2 rounded-xl border border-amber-500/40 bg-amber-500/15 px-3 py-2 text-xs font-semibold text-amber-300 transition hover:bg-amber-500 hover:text-zinc-950"
            >
              <Download size={14} />
              Xuất Excel
            </button>
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
                {topProducts.map((item) => (
                  <tr key={item.name} className="transition hover:bg-zinc-800/60">
                    <td className="px-4 py-3 text-zinc-100">{item.name}</td>
                    <td className="px-4 py-3 text-right text-zinc-300">{item.sold}</td>
                    <td className="px-4 py-3 text-right font-medium text-amber-400">{item.revenue}</td>
                    <td className="px-4 py-3 text-right text-zinc-300">{item.stock}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </article>

        <article className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
          <div className="flex items-center justify-between gap-3">
            <h3 className="text-lg font-semibold text-zinc-100">Sản phẩm sắp hết hàng</h3>
            <button
              type="button"
              onClick={() => handleTableExport("Sản phẩm sắp hết hàng")}
              className="inline-flex items-center gap-2 rounded-xl border border-amber-500/40 bg-amber-500/15 px-3 py-2 text-xs font-semibold text-amber-300 transition hover:bg-amber-500 hover:text-zinc-950"
            >
              <Download size={14} />
              Xuất Excel
            </button>
          </div>
          <div className="mt-5 max-h-72 overflow-auto rounded-2xl border border-zinc-800 [scrollbar-color:#52525b_#18181b] [scrollbar-width:thin]">
            <table className="min-w-full text-sm">
              <thead className="sticky top-0 bg-zinc-950 text-zinc-400">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">SKU</th>
                  <th className="px-4 py-3 text-left font-medium">Tên sản phẩm</th>
                  <th className="px-4 py-3 text-right font-medium">Tồn kho</th>
                  <th className="px-4 py-3 text-right font-medium">Mức an toàn</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800">
                {lowStockProducts.map((item) => (
                  <tr key={item.sku} className="transition hover:bg-zinc-800/60">
                    <td className="px-4 py-3 text-zinc-300">{item.sku}</td>
                    <td className="px-4 py-3 text-zinc-100">{item.name}</td>
                    <td className="px-4 py-3 text-right font-medium text-rose-400">{item.stock}</td>
                    <td className="px-4 py-3 text-right text-zinc-300">{item.threshold}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </article>
      </section>

      {/* Customer Analytics */}
      <section className="grid grid-cols-1 gap-5 xl:grid-cols-3">
        <article className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20 xl:col-span-2">
          <h3 className="text-lg font-semibold text-zinc-100">Khách hàng mới theo tháng</h3>
          <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
            {NEW_CUSTOMERS_BY_MONTH.map((item) => (
              <div
                key={item.month}
                className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4 text-center transition hover:border-zinc-700"
              >
                <p className="text-sm text-zinc-400">{item.month}</p>
                <p className="mt-2 text-2xl font-semibold text-zinc-100">{item.value}</p>
              </div>
            ))}
          </div>
        </article>

        <article className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
          <h3 className="text-lg font-semibold text-zinc-100">Tỷ lệ khách quay lại</h3>
          <div className="mt-8 rounded-2xl border border-zinc-800 bg-zinc-950/70 p-5">
            <p className="text-4xl font-bold text-amber-400">43.8%</p>
            <p className="mt-2 text-sm text-zinc-400">+3.4% so với tháng trước</p>
            <p className="mt-4 text-sm text-zinc-500">Phần trăm khách hàng phát sinh từ đơn hàng lặp lại.</p>
          </div>
        </article>
      </section>

      <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-sm shadow-black/20">
        <div className="flex items-center justify-between gap-3">
          <h3 className="text-lg font-semibold text-zinc-100">Top khách VIP</h3>
          <button
            type="button"
            onClick={() => handleTableExport("Top khách VIP")}
            className="inline-flex items-center gap-2 rounded-xl border border-amber-500/40 bg-amber-500/15 px-3 py-2 text-xs font-semibold text-amber-300 transition hover:bg-amber-500 hover:text-zinc-950"
          >
            <Download size={14} />
            Xuất Excel
          </button>
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
              {vipCustomers.map((item) => (
                <tr key={item.name} className="transition hover:bg-zinc-800/60">
                  <td className="px-4 py-3 text-zinc-100">{item.name}</td>
                  <td className="px-4 py-3">
                    <span className="rounded-full border border-amber-500/30 bg-amber-500/10 px-3 py-1 text-xs font-medium text-amber-400">
                      {item.tier}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right text-zinc-300">{item.orders}</td>
                  <td className="px-4 py-3 text-right font-medium text-amber-400">{item.spend}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Inventory Analytics */}
      <section className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">
        {INVENTORY_STATS.map((item) => {
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
  );
}
