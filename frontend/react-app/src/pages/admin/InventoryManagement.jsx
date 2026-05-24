import { useEffect, useMemo, useState } from "react";
import {
  AlertTriangle,
  Archive,
  Barcode,
  Boxes,
  Eye,
  FileSpreadsheet,
  FilterX,
  History,
  PackageCheck,
  Settings,
  ShieldAlert,
  Trash2,
  TrendingUp,
  Warehouse,
} from "lucide-react";
import Pagination from "../../components/common/Pagination";

const PAGE_SIZE = 5;

const stores = ["Tất cả kho", "Kho Tổng HCM", "Kho Hà Nội", "Chi nhánh Quận 1", "Kho Đà Nẵng"];
const stockStatuses = ["Tất cả trạng thái", "In Stock", "Low Stock", "Out of Stock"];
const brands = ["Tất cả brand", "UrbanVibe", "Minimal Studio", "HerBloom", "Classic Thread"];
const categories = ["Tất cả category", "Áo", "Quần", "Váy", "Phụ kiện"];

const inventorySeed = [
  {
    id: 1,
    image: "https://images.unsplash.com/photo-1618354691551-44de113f0164?auto=format&fit=crop&w=320&q=80",
    productName: "Áo Hoodie Oversize",
    sku: "HD-OVR-BLK-M",
    barcode: "8938501000012",
    variant: { color: "Đen", size: "M" },
    store: "Kho Tổng HCM",
    brand: "UrbanVibe",
    category: "Áo",
    available: 24,
    reserved: 5,
    damaged: 1,
    holding: 2,
    updatedAt: "2026-05-22 09:15",
  },
  {
    id: 2,
    image: "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=320&q=80",
    productName: "Quần Cargo Utility",
    sku: "CG-UTL-KHK-L",
    barcode: "8938501000013",
    variant: { color: "Khaki", size: "L" },
    store: "Kho Hà Nội",
    brand: "Minimal Studio",
    category: "Quần",
    available: 4,
    reserved: 2,
    damaged: 0,
    holding: 1,
    updatedAt: "2026-05-22 08:48",
  },
  {
    id: 3,
    image: "https://images.unsplash.com/photo-1623609163859-ca93c959b98a?auto=format&fit=crop&w=320&q=80",
    productName: "Váy Floral Midi",
    sku: "DR-FLR-PNK-S",
    barcode: "8938501000014",
    variant: { color: "Hồng", size: "S" },
    store: "Chi nhánh Quận 1",
    brand: "HerBloom",
    category: "Váy",
    available: 0,
    reserved: 1,
    damaged: 0,
    holding: 0,
    updatedAt: "2026-05-21 20:30",
  },
  {
    id: 4,
    image: "https://images.unsplash.com/photo-1622445275576-721325763afe?auto=format&fit=crop&w=320&q=80",
    productName: "Áo Polo Premium",
    sku: "PL-PRM-WHT-XL",
    barcode: "8938501000015",
    variant: { color: "Trắng", size: "XL" },
    store: "Kho Đà Nẵng",
    brand: "Classic Thread",
    category: "Áo",
    available: 38,
    reserved: 4,
    damaged: 2,
    holding: 3,
    updatedAt: "2026-05-22 07:25",
  },
  {
    id: 5,
    image: "https://images.unsplash.com/photo-1485968579580-b6d095142e6e?auto=format&fit=crop&w=320&q=80",
    productName: "Áo Hoodie Oversize",
    sku: "HD-OVR-GRY-L",
    barcode: "8938501000016",
    variant: { color: "Xám", size: "L" },
    store: "Kho Hà Nội",
    brand: "UrbanVibe",
    category: "Áo",
    available: 9,
    reserved: 2,
    damaged: 1,
    holding: 0,
    updatedAt: "2026-05-22 09:02",
  },
  {
    id: 6,
    image: "https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?auto=format&fit=crop&w=320&q=80",
    productName: "Quần Cargo Utility",
    sku: "CG-UTL-BLK-M",
    barcode: "8938501000017",
    variant: { color: "Đen", size: "M" },
    store: "Kho Tổng HCM",
    brand: "Minimal Studio",
    category: "Quần",
    available: 2,
    reserved: 3,
    damaged: 1,
    holding: 2,
    updatedAt: "2026-05-22 10:10",
  },
  {
    id: 7,
    image: "https://images.unsplash.com/photo-1612722432474-b971cdcea546?auto=format&fit=crop&w=320&q=80",
    productName: "Váy Floral Midi",
    sku: "DR-FLR-BLU-M",
    barcode: "8938501000018",
    variant: { color: "Xanh dương", size: "M" },
    store: "Kho Đà Nẵng",
    brand: "HerBloom",
    category: "Váy",
    available: 12,
    reserved: 1,
    damaged: 0,
    holding: 0,
    updatedAt: "2026-05-21 18:12",
  },
  {
    id: 8,
    image: "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=320&q=80",
    productName: "Áo Polo Premium",
    sku: "PL-PRM-NVY-L",
    barcode: "8938501000019",
    variant: { color: "Navy", size: "L" },
    store: "Chi nhánh Quận 1",
    brand: "Classic Thread",
    category: "Áo",
    available: 5,
    reserved: 2,
    damaged: 0,
    holding: 1,
    updatedAt: "2026-05-22 06:40",
  },
  {
    id: 9,
    image: "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=320&q=80",
    productName: "Chân Váy Pleated",
    sku: "SK-PLT-BEG-S",
    barcode: "8938501000020",
    variant: { color: "Be", size: "S" },
    store: "Kho Tổng HCM",
    brand: "HerBloom",
    category: "Váy",
    available: 0,
    reserved: 0,
    damaged: 3,
    holding: 0,
    updatedAt: "2026-05-22 08:03",
  },
  {
    id: 10,
    image: "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=320&q=80",
    productName: "Áo Sơ Mi Linen",
    sku: "SH-LNN-WHT-M",
    barcode: "8938501000021",
    variant: { color: "Trắng", size: "M" },
    store: "Kho Hà Nội",
    brand: "Minimal Studio",
    category: "Áo",
    available: 16,
    reserved: 3,
    damaged: 0,
    holding: 1,
    updatedAt: "2026-05-22 11:12",
  },
  {
    id: 11,
    image: "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=320&q=80",
    productName: "Quần Jeans Slim",
    sku: "JN-SLM-BLU-32",
    barcode: "8938501000022",
    variant: { color: "Xanh denim", size: "32" },
    store: "Kho Đà Nẵng",
    brand: "UrbanVibe",
    category: "Quần",
    available: 27,
    reserved: 5,
    damaged: 1,
    holding: 2,
    updatedAt: "2026-05-22 09:44",
  },
  {
    id: 12,
    image: "https://images.unsplash.com/photo-1495385794356-15371f348c31?auto=format&fit=crop&w=320&q=80",
    productName: "Áo Polo Premium",
    sku: "PL-PRM-BLK-M",
    barcode: "8938501000023",
    variant: { color: "Đen", size: "M" },
    store: "Kho Tổng HCM",
    brand: "Classic Thread",
    category: "Áo",
    available: 3,
    reserved: 1,
    damaged: 1,
    holding: 1,
    updatedAt: "2026-05-22 10:58",
  },
];

const createHistory = (item) => {
  const total = item.available + item.reserved + item.damaged;
  return [
    { id: `${item.id}-h1`, type: "IMPORT", quantity: Math.max(total + 15, 20), balanceAfter: total + 15, createdAt: "2026-05-15 09:10" },
    { id: `${item.id}-h2`, type: "SALE", quantity: -Math.max(item.reserved + 3, 4), balanceAfter: total + 8, createdAt: "2026-05-18 14:25" },
    { id: `${item.id}-h3`, type: "ADJUST", quantity: -item.damaged, balanceAfter: total + 8 - item.damaged, createdAt: "2026-05-20 11:50" },
    { id: `${item.id}-h4`, type: "RESERVE", quantity: -item.reserved, balanceAfter: total + 8 - item.damaged - item.reserved, createdAt: "2026-05-22 09:05" },
  ];
};

const getStockStatus = (available) => {
  if (available === 0) {
    return { label: "Out of Stock", className: "bg-red-500/15 text-red-300 border-red-400/30" };
  }
  if (available <= 5) {
    return { label: "Low Stock", className: "bg-amber-500/15 text-amber-300 border-amber-300/30" };
  }
  return { label: "In Stock", className: "bg-emerald-500/15 text-emerald-300 border-emerald-300/30" };
};

const formatGrowth = (value) => `${value > 0 ? "+" : ""}${value}% so với tuần trước`;

const statsConfig = [
  { key: "totalSku", label: "Tổng SKU tồn kho", icon: Boxes, accent: "text-cyan-300", growth: 8.2 },
  { key: "totalQuantity", label: "Tổng số lượng tồn", icon: Warehouse, accent: "text-indigo-300", growth: 5.7 },
  { key: "lowStockSku", label: "SKU sắp hết hàng", icon: AlertTriangle, accent: "text-amber-300", growth: -2.1 },
  { key: "outOfStockSku", label: "SKU hết hàng", icon: Archive, accent: "text-rose-300", growth: -1.3 },
  { key: "holdingQty", label: "Hàng đang giữ", icon: PackageCheck, accent: "text-blue-300", growth: 3.4 },
  { key: "damagedQty", label: "Hàng lỗi", icon: ShieldAlert, accent: "text-orange-300", growth: -0.9 },
];

export default function InventoryManagement() {
  const [inventoryData, setInventoryData] = useState(
    inventorySeed.map((item) => ({
      ...item,
      isDeleted: false,
      inventoryByStore: stores.slice(1).map((storeName, idx) => ({
        store: storeName,
        available: Math.max(item.available - idx * 2, 0),
        reserved: Math.max(item.reserved - (idx > 1 ? 1 : 0), 0),
        damaged: idx === 0 ? item.damaged : Math.max(item.damaged - 1, 0),
      })),
      transactions: createHistory(item),
    }))
  );

  const [searchTerm, setSearchTerm] = useState("");
  const [selectedStore, setSelectedStore] = useState(stores[0]);
  const [selectedStatus, setSelectedStatus] = useState(stockStatuses[0]);
  const [selectedBrand, setSelectedBrand] = useState(brands[0]);
  const [selectedCategory, setSelectedCategory] = useState(categories[0]);
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedItem, setSelectedItem] = useState(null);

  const activeInventory = useMemo(
    () => inventoryData.filter((item) => !item.isDeleted),
    [inventoryData]
  );

  const statistics = useMemo(() => {
    const lowStockSku = activeInventory.filter((item) => item.available > 0 && item.available <= 5).length;
    const outOfStockSku = activeInventory.filter((item) => item.available === 0).length;

    return {
      totalSku: activeInventory.length,
      totalQuantity: activeInventory.reduce((sum, item) => sum + item.available + item.reserved + item.damaged, 0),
      lowStockSku,
      outOfStockSku,
      holdingQty: activeInventory.reduce((sum, item) => sum + item.holding, 0),
      damagedQty: activeInventory.reduce((sum, item) => sum + item.damaged, 0),
    };
  }, [activeInventory]);

  const filteredInventory = useMemo(() => {
    return activeInventory.filter((item) => {
      const keyword = searchTerm.trim().toLowerCase();
      const matchesSearch =
        !keyword ||
        item.productName.toLowerCase().includes(keyword) ||
        item.sku.toLowerCase().includes(keyword) ||
        item.barcode.toLowerCase().includes(keyword);

      const statusLabel = getStockStatus(item.available).label;
      const matchesStore = selectedStore === "Tất cả kho" || item.store === selectedStore;
      const matchesStatus = selectedStatus === "Tất cả trạng thái" || statusLabel === selectedStatus;
      const matchesBrand = selectedBrand === "Tất cả brand" || item.brand === selectedBrand;
      const matchesCategory = selectedCategory === "Tất cả category" || item.category === selectedCategory;

      return matchesSearch && matchesStore && matchesStatus && matchesBrand && matchesCategory;
    });
  }, [activeInventory, searchTerm, selectedStore, selectedStatus, selectedBrand, selectedCategory]);

  const totalPages = Math.max(1, Math.ceil(filteredInventory.length / PAGE_SIZE));

  const paginatedInventory = useMemo(() => {
    const startIndex = (currentPage - 1) * PAGE_SIZE;
    return filteredInventory.slice(startIndex, startIndex + PAGE_SIZE);
  }, [filteredInventory, currentPage]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, selectedStore, selectedStatus, selectedBrand, selectedCategory]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const handleResetFilters = () => {
    setSearchTerm("");
    setSelectedStore(stores[0]);
    setSelectedStatus(stockStatuses[0]);
    setSelectedBrand(brands[0]);
    setSelectedCategory(categories[0]);
  };

  const handleSoftDelete = (id) => {
    setInventoryData((prev) => prev.map((item) => (item.id === id ? { ...item, isDeleted: true } : item)));
    if (selectedItem?.id === id) {
      setSelectedItem(null);
    }
  };

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 p-4 md:p-6 lg:p-8">
      <div className="mx-auto max-w-[1600px] space-y-6">
        <section>
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
            <div>
              <h1 className="text-2xl md:text-3xl font-bold tracking-tight">Inventory Management</h1>
              <p className="text-zinc-400 mt-1">Theo dõi tồn kho realtime theo SKU, biến thể và kho hàng.</p>
            </div>
            <div className="inline-flex items-center gap-2 rounded-2xl border border-zinc-800 bg-zinc-900 px-4 py-2 text-sm text-zinc-300">
              <TrendingUp size={16} className="text-emerald-300" />
              Last sync: 2026-05-22 11:20
            </div>
          </div>
        </section>

        <section className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          {statsConfig.map((stat) => {
            const Icon = stat.icon;
            return (
              <div
                key={stat.key}
                className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5 shadow-[0_16px_40px_rgba(0,0,0,0.3)]"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-sm text-zinc-400">{stat.label}</p>
                    <p className="mt-2 text-3xl font-semibold text-white">{statistics[stat.key].toLocaleString("vi-VN")}</p>
                  </div>
                  <div className="h-11 w-11 rounded-2xl bg-zinc-800 flex items-center justify-center">
                    <Icon size={20} className={stat.accent} />
                  </div>
                </div>
                <p className={`mt-4 text-xs ${stat.growth >= 0 ? "text-emerald-300" : "text-rose-300"}`}>{formatGrowth(stat.growth)}</p>
              </div>
            );
          })}
        </section>

        <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-4 md:p-5">
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-5 gap-3">
            <input
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm tên sản phẩm, SKU hoặc barcode..."
              className="xl:col-span-2 h-11 rounded-2xl border border-zinc-700 bg-zinc-800 px-4 text-sm outline-none focus:border-amber-400 focus:ring-2 focus:ring-amber-400/30"
            />

            <select
              value={selectedStore}
              onChange={(e) => setSelectedStore(e.target.value)}
              className="h-11 rounded-2xl border border-zinc-700 bg-zinc-800 px-3 text-sm outline-none focus:border-amber-400 focus:ring-2 focus:ring-amber-400/30"
            >
              {stores.map((store) => (
                <option key={store} value={store}>
                  {store}
                </option>
              ))}
            </select>

            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="h-11 rounded-2xl border border-zinc-700 bg-zinc-800 px-3 text-sm outline-none focus:border-amber-400 focus:ring-2 focus:ring-amber-400/30"
            >
              {stockStatuses.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>

            <select
              value={selectedBrand}
              onChange={(e) => setSelectedBrand(e.target.value)}
              className="h-11 rounded-2xl border border-zinc-700 bg-zinc-800 px-3 text-sm outline-none focus:border-amber-400 focus:ring-2 focus:ring-amber-400/30"
            >
              {brands.map((brand) => (
                <option key={brand} value={brand}>
                  {brand}
                </option>
              ))}
            </select>

            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="h-11 rounded-2xl border border-zinc-700 bg-zinc-800 px-3 text-sm outline-none focus:border-amber-400 focus:ring-2 focus:ring-amber-400/30"
            >
              {categories.map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
          </div>

          <div className="mt-4 flex flex-wrap gap-3">
            <button
              type="button"
              onClick={handleResetFilters}
              className="h-11 rounded-2xl border border-zinc-700 bg-zinc-800 px-4 text-sm font-medium hover:bg-zinc-700 transition-colors inline-flex items-center gap-2"
            >
              <FilterX size={16} />
              Reset filter
            </button>
            <button
              type="button"
              className="h-11 rounded-2xl border border-emerald-400/30 bg-emerald-500/10 px-4 text-sm font-medium text-emerald-200 hover:bg-emerald-500/20 transition-colors inline-flex items-center gap-2"
            >
              <FileSpreadsheet size={16} />
              Export Excel
            </button>
            <button
              type="button"
              className="h-11 rounded-2xl border border-amber-400/30 bg-amber-500/10 px-4 text-sm font-medium text-amber-200 hover:bg-amber-500/20 transition-colors inline-flex items-center gap-2"
            >
              <Settings size={16} />
              Điều chỉnh tồn kho
            </button>
          </div>
        </section>

        <section className="rounded-3xl border border-zinc-800 bg-zinc-900 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-[1080px] w-full text-sm">
              <thead className="bg-zinc-800/70 text-zinc-300">
                <tr>
                  {[
                    "Ảnh sản phẩm",
                    "Tên sản phẩm",
                    "SKU",
                    "Variant",
                    "Kho hàng",
                    "Available Quantity",
                    "Total Stock",
                    "Stock Status",
                    "Actions",
                  ].map((head) => (
                    <th key={head} className="px-4 py-3 text-left font-semibold whitespace-nowrap">
                      {head}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {paginatedInventory.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="py-16 text-center text-zinc-400">
                      Không có dữ liệu tồn kho phù hợp bộ lọc.
                    </td>
                  </tr>
                ) : (
                  paginatedInventory.map((item) => {
                    const status = getStockStatus(item.available);
                    const total = item.available + item.reserved + item.damaged;
                    return (
                      <tr key={item.id} className="border-t border-zinc-800 hover:bg-zinc-800/40 transition-colors">
                        <td className="px-4 py-3">
                          <img src={item.image} alt={item.productName} className="h-14 w-14 rounded-xl object-cover border border-zinc-700" />
                        </td>
                        <td className="px-4 py-3 font-medium text-zinc-100">{item.productName}</td>
                        <td className="px-4 py-3 text-zinc-200">{item.sku}</td>
                        <td className="px-4 py-3 text-zinc-300 whitespace-nowrap">{item.variant.color} / {item.variant.size}</td>
                        <td className="px-4 py-3 text-zinc-300">{item.store}</td>
                        <td className="px-4 py-3 text-zinc-100 font-semibold">{item.available}</td>
                        <td className="px-4 py-3 text-zinc-100">{total}</td>
                        <td className="px-4 py-3">
                          <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${status.className}`}>
                            {status.label}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-center gap-3">
                            <button
                              type="button"
                              onClick={() => setSelectedItem(item)}
                              className="text-blue-400 hover:text-blue-300 transition-colors"
                              title="Xem chi tiết"
                            >
                              <Eye size={18} />
                            </button>
                            <button
                              type="button"
                              onClick={() => setSelectedItem(item)}
                              className="text-emerald-400 hover:text-emerald-300 transition-colors"
                              title="Xem transaction"
                            >
                              <History size={18} />
                            </button>
                            <button
                              type="button"
                              className="text-orange-400 hover:text-orange-300 transition-colors"
                              title="Điều chỉnh tồn kho"
                            >
                              <Settings size={18} />
                            </button>
                            <button
                              type="button"
                              onClick={() => handleSoftDelete(item.id)}
                              className="text-red-400 hover:text-red-300 transition-colors"
                              title="Xóa mềm"
                            >
                              <Trash2 size={18} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} />
        </section>
      </div>

      {selectedItem && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm p-4 md:p-8 overflow-y-auto">
          <div className="mx-auto max-w-6xl rounded-3xl border border-zinc-700 bg-zinc-900 shadow-2xl">
            <div className="p-5 md:p-7 border-b border-zinc-800 flex items-start justify-between gap-4">
              <div>
                <h2 className="text-2xl font-semibold">Inventory Detail</h2>
                <p className="text-zinc-400 mt-1">Chi tiết tồn kho theo sản phẩm và kho vận hành.</p>
              </div>
              <button
                type="button"
                onClick={() => setSelectedItem(null)}
                className="h-10 px-4 rounded-2xl bg-zinc-800 border border-zinc-700 hover:bg-zinc-700 transition-colors"
              >
                Đóng
              </button>
            </div>

            <div className="p-5 md:p-7 space-y-6">
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
                <div className="lg:col-span-2 rounded-3xl border border-zinc-800 bg-zinc-950/50 p-5">
                  <h3 className="text-lg font-semibold mb-4">Product Info</h3>
                  <div className="flex flex-col sm:flex-row gap-4">
                    <img src={selectedItem.image} alt={selectedItem.productName} className="h-28 w-28 rounded-2xl object-cover border border-zinc-700" />
                    <div className="space-y-2 text-sm">
                      <p className="text-zinc-100 font-semibold text-base">{selectedItem.productName}</p>
                      <p className="text-zinc-300">SKU: {selectedItem.sku}</p>
                      <p className="text-zinc-300 inline-flex items-center gap-2"><Barcode size={14} />{selectedItem.barcode}</p>
                      <p className="text-zinc-300">Màu: {selectedItem.variant.color}</p>
                      <p className="text-zinc-300">Size: {selectedItem.variant.size}</p>
                    </div>
                  </div>
                </div>

                <div className="rounded-3xl border border-zinc-800 bg-zinc-950/50 p-5">
                  <h3 className="text-lg font-semibold mb-4">Inventory Summary</h3>
                  <div className="space-y-3 text-sm">
                    <div className="rounded-2xl bg-zinc-800/70 p-3 flex items-center justify-between">
                      <span className="text-zinc-400">Available</span>
                      <span className="text-emerald-300 font-semibold">{selectedItem.available}</span>
                    </div>
                    <div className="rounded-2xl bg-zinc-800/70 p-3 flex items-center justify-between">
                      <span className="text-zinc-400">Reserved</span>
                      <span className="text-amber-300 font-semibold">{selectedItem.reserved}</span>
                    </div>
                    <div className="rounded-2xl bg-zinc-800/70 p-3 flex items-center justify-between">
                      <span className="text-zinc-400">Damaged</span>
                      <span className="text-rose-300 font-semibold">{selectedItem.damaged}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="rounded-3xl border border-zinc-800 bg-zinc-950/50 p-5 overflow-x-auto">
                <h3 className="text-lg font-semibold mb-4">Inventory By Store</h3>
                <table className="min-w-[640px] w-full text-sm">
                  <thead className="text-zinc-400">
                    <tr>
                      <th className="text-left py-2">Kho</th>
                      <th className="text-left py-2">Available</th>
                      <th className="text-left py-2">Reserved</th>
                      <th className="text-left py-2">Damaged</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedItem.inventoryByStore.map((storeRow) => (
                      <tr key={`${selectedItem.id}-${storeRow.store}`} className="border-t border-zinc-800">
                        <td className="py-3 text-zinc-200">{storeRow.store}</td>
                        <td className="py-3 text-zinc-300">{storeRow.available}</td>
                        <td className="py-3 text-zinc-300">{storeRow.reserved}</td>
                        <td className="py-3 text-zinc-300">{storeRow.damaged}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="rounded-3xl border border-zinc-800 bg-zinc-950/50 p-5 overflow-x-auto">
                <h3 className="text-lg font-semibold mb-4">Transaction History</h3>
                <table className="min-w-[760px] w-full text-sm">
                  <thead className="text-zinc-400">
                    <tr>
                      <th className="text-left py-2">Type</th>
                      <th className="text-left py-2">Quantity</th>
                      <th className="text-left py-2">Balance After</th>
                      <th className="text-left py-2">Created At</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedItem.transactions.map((tx) => (
                      <tr key={tx.id} className="border-t border-zinc-800">
                        <td className="py-3">
                          <span className="inline-flex rounded-full px-2.5 py-1 text-xs bg-zinc-800 border border-zinc-700 text-zinc-200">
                            {tx.type}
                          </span>
                        </td>
                        <td className={`py-3 font-semibold ${tx.quantity >= 0 ? "text-emerald-300" : "text-rose-300"}`}>
                          {tx.quantity >= 0 ? `+${tx.quantity}` : tx.quantity}
                        </td>
                        <td className="py-3 text-zinc-200">{tx.balanceAfter}</td>
                        <td className="py-3 text-zinc-400">{tx.createdAt}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
