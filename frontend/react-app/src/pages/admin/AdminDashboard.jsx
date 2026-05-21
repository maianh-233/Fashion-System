import StatsCard from "../../components/admin/StatsCard";
import RevenueChart from "../../components/admin/RevenueChart";
import LowStockList from "../../components/admin/LowStockList";
import BestSellingProducts from "../../components/admin/BestSellingProducts";
import RecentOrders from "../../components/admin/RecentOrders";
import RecentImportReceipts from "../../components/admin/RecentImportReceipts";
import RecentExportReceipts from "../../components/admin/RecentExportReceipts";
import AdminChatWidget from "../../components/admin/AdminChatWidget";

import {
  DollarSign,
  ShoppingBag,
  UserPlus,
  Package,
} from "lucide-react";

export default function AdminDashboard() {
  return (
    <div className="space-y-8">
      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatsCard
          title="Doanh thu tháng này"
          value="685.4M"
          unit="đ"
          icon={<DollarSign size={40} />}
          iconColor="text-emerald-400"
          change="↑ 18.2% so với tháng trước"
          changeColor="text-emerald-400"
        />

        <StatsCard
          title="Tổng đơn hàng"
          value="1,284"
          icon={<ShoppingBag size={40} />}
          iconColor="text-amber-400"
          change="↑ 9% so với tháng trước"
          changeColor="text-emerald-400"
        />

        <StatsCard
          title="Khách hàng mới"
          value="87"
          icon={<UserPlus size={40} />}
          iconColor="text-blue-400"
          change="↓ 3 khách so với tuần trước"
          changeColor="text-red-400"
        />

        <StatsCard
          title="Sản phẩm đã bán"
          value="2,394"
          icon={<Package size={40} />}
          iconColor="text-purple-400"
          change="↑ 24% so với tháng trước"
          changeColor="text-emerald-400"
        />
      </div>

      {/* Chart + stock */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <RevenueChart />
        </div>

        <LowStockList />
      </div>

      {/* Products + orders */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <BestSellingProducts />
        <RecentOrders />
      </div>

      {/* Import + export receipts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RecentImportReceipts />
        <RecentExportReceipts />
      </div>

      <AdminChatWidget />
    </div>
  );
}
