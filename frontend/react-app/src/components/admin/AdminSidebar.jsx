import { useState } from "react";
import {
  Sparkles,
  House,
  Shirt,
  ShoppingBag,
  Users,
  ChartColumn,
  Tags,
  UserCog,
  ShieldCheck,
  PackagePlus,
  PackageMinus,
  Logs,
  Store,
  Truck,
  ChevronDown,
  ChevronRight,

  // icon submenu
  BadgeCheck,
  Layers3,
  FolderTree,
  Package2,
  Boxes,
  Tag,
} from "lucide-react";

import { useLocation, useNavigate } from "react-router-dom";

export default function AdminSidebar() {
  const navigate = useNavigate();
  const location = useLocation();

  const [openProducts, setOpenProducts] = useState(true);

  const menu = [
    {
      icon: <House size={18} />,
      label: "Trang chủ",
      path: "/admin",
    },

    // ===== SẢN PHẨM =====
    {
      icon: <Shirt size={18} />,
      label: "Sản phẩm",

      children: [
        {
          icon: <BadgeCheck size={16} />,
          label: "Quản lý brand",
          path: "/admin/brands",
        },

        {
          icon: <Layers3 size={16} />,
          label: "Quản lý bộ sưu tập",
          path: "/admin/collections",
        },

        {
          icon: <FolderTree size={16} />,
          label: "Quản lý danh mục",
          path: "/admin/categories",
        },

        {
          icon: <Package2 size={16} />,
          label: "Quản lý sản phẩm",
          path: "/admin/products",
        },

        {
          icon: <Boxes size={16} />,
          label: "Quản lý biến thể",
          path: "/admin/product-variants",
        },
            
        {
          icon: <PackagePlus size={16} />,
          label: "Quản lý kho",
          path: "/admin/inventory",
        },

        {
          icon: <Tag size={16} />,
          label: "Quản lý tag",
          path: "/admin/product-tags",
        },
      ],
    },

    // ===== ĐƠN HÀNG =====
    {
      icon: <ShoppingBag size={18} />,
      label: "Đơn hàng",
      path: "/admin/orders",
    },

    // ===== NHÂN VIÊN =====
    {
      icon: <UserCog size={18} />,
      label: "Nhân viên",
      path: "/admin/employees",
    },

    // ===== PHÂN QUYỀN =====
    {
      icon: <ShieldCheck size={18} />,
      label: "Phân quyền",
      path: "/admin/roles",
    },

    // ===== KHÁCH HÀNG =====
    {
      icon: <Users size={18} />,
      label: "Khách hàng",
      path: "/admin/customers",
    },

    // ===== PHIẾU NHẬP =====
    {
      icon: <PackagePlus size={18} />,
      label: "Phiếu nhập",
      path: "/admin/imports",
    },

    // ===== PHIẾU XUẤT =====
    {
      icon: <PackageMinus size={18} />,
      label: "Phiếu xuất",
      path: "/admin/exports",
    },

    // ===== CỬA HÀNG =====
    {
      icon: <Store size={18} />,
      label: "Cửa hàng",
      path: "/admin/stores",
    },

    // ===== NHÀ CUNG CẤP =====
    {
      icon: <Truck size={18} />,
      label: "Nhà cung cấp",
      path: "/admin/suppliers",
    },

    // ===== THỐNG KÊ =====
    {
      icon: <ChartColumn size={18} />,
      label: "Thống kê",
      path: "/admin/statistics",
    },

    // ===== KHUYẾN MÃI =====
    {
      icon: <Tags size={18} />,
      label: "Khuyến mãi",
      path: "/admin/promotions",
    },

    // ===== LOG =====
    {
      icon: <Logs size={18} />,
      label: "Log hệ thống",
      path: "/admin/logs",
    },
  ];

  return (
    <div className="w-72 bg-zinc-900 border-r border-zinc-800 flex flex-col">
      {/* Logo */}
      <div className="p-6 border-b border-zinc-800">
        <div className="flex items-center gap-3">
          <Sparkles className="text-amber-400" size={32} />

          <div>
            <h1 className="text-2xl tracking-widest text-white font-bold">
              LUNARIA
            </h1>

            <p className="text-xs text-amber-400">
              ADMIN DASHBOARD
            </p>
          </div>
        </div>
      </div>

      {/* Menu */}
      <div className="flex-1 overflow-y-auto overflow-x-hidden scrollbar-hide py-6 px-4">
        <nav className="space-y-2">
          {menu.map((item, index) => {
            // ===== MENU CÓ SUBMENU =====
            if (item.children) {
              return (
                <div key={index}>
                  <button
                    onClick={() => setOpenProducts(!openProducts)}
                    className="w-full flex items-center justify-between px-4 py-3 rounded-2xl transition-all hover:bg-zinc-800 text-zinc-200"
                  >
                    <div className="flex items-center gap-3">
                      {item.icon}
                      <span>{item.label}</span>
                    </div>

                    {openProducts ? (
                      <ChevronDown size={16} />
                    ) : (
                      <ChevronRight size={16} />
                    )}
                  </button>

                  {/* SUB MENU */}
                  <div
                    className={`overflow-hidden transition-all duration-300 ${
                      openProducts
                        ? "max-h-96 opacity-100 mt-2"
                        : "max-h-0 opacity-0"
                    }`}
                  >
                    <div className="ml-5 border-l border-zinc-700 pl-3 space-y-1">
                      {item.children.map((child, childIndex) => (
                        <button
                          key={childIndex}
                          onClick={() => navigate(child.path)}
                          className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm transition-all ${
                            location.pathname === child.path
                              ? "bg-amber-500/20 text-amber-400"
                              : "text-zinc-400 hover:bg-zinc-800 hover:text-white"
                          }`}
                        >
                          {child.icon}

                          <span>{child.label}</span>
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              );
            }

            // ===== MENU THƯỜNG =====
            return (
              <button
                key={index}
                onClick={() => item.path && navigate(item.path)}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-2xl transition-all hover:bg-zinc-800 ${
                  item.path && location.pathname === item.path
                    ? "bg-zinc-800 text-white"
                    : "text-zinc-300"
                }`}
              >
                {item.icon}

                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>
      </div>
    </div>
  );
}