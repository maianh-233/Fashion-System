import StatsCard from "../../components/admin/StatsCard";
import RevenueChart from "../../components/admin/RevenueChart";
import LowStockList from "../../components/admin/LowStockList";
import BestSellingProducts from "../../components/admin/BestSellingProducts";
import RecentOrders from "../../components/admin/RecentOrders";
import RecentImportReceipts from "../../components/admin/RecentImportReceipts";
import RecentExportReceipts from "../../components/admin/RecentExportReceipts";
import AdminChatWidget from "../../components/admin/AdminChatWidget";

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

export default function AdminDashboard() {
  return (
    <div className="admin-dashboard">
      <section className="admin-dashboard__intro">
        <div>
          <span className="admin-dashboard__eyebrow">Tổng quan vận hành</span>
          <h1>Chào buổi sáng, Admin</h1>
          <p>Theo dõi hiệu suất kinh doanh và những việc cần ưu tiên hôm nay.</p>
        </div>

        <div className="admin-dashboard__intro-actions">
          <span className="admin-dashboard__date">
            <CalendarDays size={15} aria-hidden="true" /> 01 tháng 09, 2026
          </span>
          <Link to="/admin/imports" className="admin-dashboard__quick-action">
            <PackagePlus size={15} aria-hidden="true" /> Tạo phiếu nhập
          </Link>
        </div>
      </section>

      {/* Stats */}
      <div className="admin-dashboard__stats">
        <StatsCard
          title="Doanh thu tháng này"
          value="685.4M"
          unit="đ"
          icon={<DollarSign size={20} />}
          iconColor="text-emerald-400"
          change="↑ 18.2% so với tháng trước"
          changeColor="text-emerald-400"
        />

        <StatsCard
          title="Tổng đơn hàng"
          value="1,284"
          icon={<ShoppingBag size={20} />}
          iconColor="text-amber-400"
          change="↑ 9% so với tháng trước"
          changeColor="text-emerald-400"
        />

        <StatsCard
          title="Khách hàng mới"
          value="87"
          icon={<UserPlus size={20} />}
          iconColor="text-blue-400"
          change="↓ 3 khách so với tuần trước"
          changeColor="text-red-400"
        />

        <StatsCard
          title="Sản phẩm đã bán"
          value="2,394"
          icon={<Package size={20} />}
          iconColor="text-purple-400"
          change="↑ 24% so với tháng trước"
          changeColor="text-emerald-400"
        />
      </div>

      {/* Chart + stock */}
      <div className="admin-dashboard__grid admin-dashboard__grid--overview">
        <div className="admin-dashboard__chart">
          <RevenueChart />
        </div>

        <LowStockList />
      </div>

      {/* Products + orders */}
      <div className="admin-dashboard__grid admin-dashboard__grid--halves">
        <BestSellingProducts />
        <RecentOrders />
      </div>

      {/* Import + export receipts */}
      <section className="admin-dashboard__section-heading">
        <div>
          <span>Luân chuyển hàng hóa</span>
          <h2>Phiếu kho gần đây</h2>
        </div>
        <Link to="/admin/inventory">Xem tồn kho <ArrowUpRight size={14} /></Link>
      </section>
      <div className="admin-dashboard__grid admin-dashboard__grid--halves">
        <RecentImportReceipts />
        <RecentExportReceipts />
      </div>

      <AdminChatWidget />
    </div>
  );
}
