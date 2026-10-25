import { useEffect, useState } from "react";
import StatsCard from "../../components/admin/StatsCard";
import RevenueChart from "../../components/admin/RevenueChart";
import LowStockList from "../../components/admin/LowStockList";
import BestSellingProducts from "../../components/admin/BestSellingProducts";
import RecentOrders from "../../components/admin/RecentOrders";
import RecentImportReceipts from "../../components/admin/RecentImportReceipts";
import RecentExportReceipts from "../../components/admin/RecentExportReceipts";
import StoreSelector, { ALL_STORES } from "../../components/common/StoreSelector";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import useStoreOptions from "../../hooks/useStoreOptions";
import { dashboardApi } from "../../api/admin/dashboardApi";
import { getAdminSession } from "../../api/http/adminSession";
import { TONE_CLASSES, changeVsPrevious, compactMoney, count, greeting, longDate } from "./dashboardLogic";

import {
  ArrowUpRight,
  CalendarDays,
  DollarSign,
  PackagePlus,
  ShoppingBag,
  UserPlus,
  Package,
} from "lucide-react";
import { Link } from "react-router-dom";

function stat(current, previous) {
  const change = changeVsPrevious(current, previous);
  return { change: change.text, changeColor: TONE_CLASSES[change.tone] };
}

export default function AdminDashboard() {
  const { isGlobal, loading: permissionsLoading, hasPermission } = useAdminPermissions();
  const { stores, loading: storesLoading } = useStoreOptions({ endpoint: dashboardApi.STORES_PATH, enabled: isGlobal });
  const [storeId, setStoreId] = useState(ALL_STORES);
  // Kết quả gắn với khóa yêu cầu; đang tải khi khóa hiện tại chưa có kết quả.
  const [result, setResult] = useState({ key: null, data: null, error: "" });
  const requestKey = permissionsLoading ? null : (isGlobal && storeId !== ALL_STORES ? storeId : "");

  useEffect(() => {
    if (requestKey === null) return undefined;
    const controller = new AbortController();
    dashboardApi.overview(requestKey || undefined, { signal: controller.signal })
      .then(data => { if (!controller.signal.aborted) setResult({ key: requestKey, data, error: "" }); })
      .catch(failure => {
        if (!controller.signal.aborted) setResult(current => ({
          key: requestKey, data: current.data, error: failure.message || "Không thể tải số liệu tổng quan.",
        }));
      });
    return () => controller.abort();
  }, [requestKey]);

  const { data, error } = result;
  const loading = result.key !== requestKey;
  const username = getAdminSession()?.user?.username || "Admin";
  const month = data?.month;
  const showStore = isGlobal && storeId === ALL_STORES;
  const scopeLabel = data ? (data.storeName || "Toàn hệ thống") : "";

  return (
    <div className="admin-dashboard">
      <section className="admin-dashboard__intro">
        <div>
          <span className="admin-dashboard__eyebrow">Tổng quan vận hành{scopeLabel ? ` · ${scopeLabel}` : ""}</span>
          <h1>{greeting(new Date().getHours())}, {username}</h1>
          <p>Theo dõi hiệu suất kinh doanh và những việc cần ưu tiên hôm nay.</p>
        </div>

        <div className="admin-dashboard__intro-actions">
          {isGlobal && (
            <StoreSelector
              stores={stores}
              value={storeId}
              onChange={setStoreId}
              includeAll
              loading={storesLoading}
              label=""
              className="admin-dashboard__date bg-transparent"
            />
          )}
          {data?.today && (
            <span className="admin-dashboard__date">
              <CalendarDays size={15} aria-hidden="true" /> {longDate(data.today)}
            </span>
          )}
          {hasPermission("IMPORT_RECEIPT_VIEW") && (
            <Link to="/admin/imports" className="admin-dashboard__quick-action">
              <PackagePlus size={15} aria-hidden="true" /> Tạo phiếu nhập
            </Link>
          )}
        </div>
      </section>

      {error && <p className="rounded-2xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">{error}</p>}
      {loading && !data && !error && <p className="text-sm text-zinc-400">Đang tải số liệu…</p>}

      {data && (
        <>
          {/* Stats: từ đầu tháng đến hôm nay, so với cùng kỳ tháng trước */}
          <div className={`admin-dashboard__stats${loading ? " opacity-60" : ""}`}>
            <StatsCard
              title="Doanh thu tháng này"
              value={compactMoney(month.revenue)}
              unit="đ"
              icon={<DollarSign size={20} />}
              iconColor="text-emerald-400"
              {...stat(month.revenue, month.previousRevenue)}
            />

            <StatsCard
              title="Đơn hàng tháng này"
              value={count(month.orders)}
              icon={<ShoppingBag size={20} />}
              iconColor="text-amber-400"
              {...stat(month.orders, month.previousOrders)}
            />

            <StatsCard
              title="Khách hàng mới"
              value={count(month.newCustomers)}
              icon={<UserPlus size={20} />}
              iconColor="text-blue-400"
              {...stat(month.newCustomers, month.previousNewCustomers)}
            />

            <StatsCard
              title="Sản phẩm đã bán"
              value={count(month.itemsSold)}
              icon={<Package size={20} />}
              iconColor="text-purple-400"
              {...stat(month.itemsSold, month.previousItemsSold)}
            />
          </div>

          {/* Chart + stock */}
          <div className="admin-dashboard__grid admin-dashboard__grid--overview">
            <div className="admin-dashboard__chart">
              <RevenueChart daily={data.dailyRevenue} yearly={data.yearlyRevenue} />
            </div>

            {data.lowStock && <LowStockList items={data.lowStock} showStore={showStore} />}
          </div>

          {/* Products + orders */}
          <div className="admin-dashboard__grid admin-dashboard__grid--halves">
            <BestSellingProducts products={data.bestSellers} />
            {data.recentOrders && <RecentOrders orders={data.recentOrders} showStore={showStore} />}
          </div>

          {/* Import + export receipts */}
          {(data.recentImports || data.recentExports) && (
            <>
              <section className="admin-dashboard__section-heading">
                <div>
                  <span>Luân chuyển hàng hóa</span>
                  <h2>Phiếu kho gần đây</h2>
                </div>
                {hasPermission("INVENTORY_VIEW") && <Link to="/admin/inventory">Xem tồn kho <ArrowUpRight size={14} /></Link>}
              </section>
              <div className="admin-dashboard__grid admin-dashboard__grid--halves">
                {data.recentImports && <RecentImportReceipts receipts={data.recentImports} showStore={showStore} />}
                {data.recentExports && <RecentExportReceipts issues={data.recentExports} showStore={showStore} />}
              </div>
            </>
          )}
        </>
      )}

    </div>
  );
}
