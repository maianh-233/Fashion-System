import { ShoppingBag } from "lucide-react";
import { Link, NavLink } from "react-router-dom";
import {
  User,
  Package,
  Settings,
  LogOut,
  ChevronDown
} from "lucide-react";
import { useState, useRef, useEffect } from "react";

function BrandMark() {
  return (
    <Link to="/" className="flex items-center gap-3">
      <img 
        src="/LUNARIALOGO.png" 
        alt="Lunaria Logo" 
        className="h-10 w-auto"
        onError={(e) => {
          e.target.style.display = "none";
          e.target.nextElementSibling.style.display = "flex";
        }}
      />
      <div className="flex items-center gap-3 hidden" style={{ display: "flex" }}>
        <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-400 to-yellow-600 text-3xl font-bold text-black">
          L
        </div>
        <span className="text-3xl font-light tracking-[0.3em]">
          LUNARIA
        </span>
      </div>
    </Link>
  );
}

export default function StorefrontHeader({ navLinks, cartCount }) {
  const [open, setOpen] = useState(false);
  const userRef = useRef(null);

  // đóng dropdown khi click ra ngoài
  useEffect(() => {
    const handler = (e) => {
      if (userRef.current && !userRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  return (
    <header className="sticky top-0 z-50 border-b border-zinc-800 bg-zinc-900/90 backdrop-blur">
      <div className="flex w-full items-center justify-between gap-4 px-4 py-5 sm:px-6 lg:px-10 2xl:px-16">
        <BrandMark />

        <nav className="hidden items-center gap-8 text-sm font-medium lg:flex">
          {navLinks.map((link) => (
            <NavLink
              key={link.id}
              to={link.href}
              className={({ isActive }) =>
                `transition ${
                  isActive ? "text-amber-400" : "text-white/80"
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>

        <div className="flex items-center gap-5">
          {/* CART */}
          <Link
            to="/carts"
            className="relative transition hover:text-amber-400"
            aria-label="Cart"
          >
            <ShoppingBag size={20} />
            <span className="absolute -right-2 -top-2 flex h-4 w-4 items-center justify-center rounded-full bg-amber-400 text-[10px] font-semibold text-black">
              {cartCount}
            </span>
          </Link>

          {/* USER DROPDOWN */}
          <div ref={userRef} className="relative">
            {/* Trigger */}
            <div
              onClick={() => setOpen(!open)}
              className="flex cursor-pointer items-center gap-3 rounded-xl px-2 py-1 transition hover:bg-zinc-800"
            >
              <div className="text-right leading-tight">
                <p className="text-sm font-medium">
                  Phạm Văn Minh Trang
                </p>
              </div>

              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-zinc-700 text-sm font-bold">
                DA
              </div>

              <ChevronDown
                size={16}
                className={`transition ${
                  open ? "rotate-180" : ""
                }`}
              />
            </div>

            {/* Dropdown menu */}
            {open && (
              <div className="absolute right-0 mt-3 w-56 overflow-hidden rounded-2xl border border-zinc-700 bg-zinc-900 shadow-xl">
                <DropdownItem icon={<User size={16} />} label="Thông tin cá nhân" />
                <DropdownItem icon={<Package size={16} />} label="Đơn hàng của tôi" />
                <DropdownItem icon={<Settings size={16} />} label="Cài đặt" />

                <div className="my-1 h-px bg-zinc-700" />

                <DropdownItem
                  icon={<LogOut size={16} />}
                  label="Đăng xuất"
                  danger
                />
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}

function DropdownItem({ icon, label, danger }) {
  return (
    <div
      className={`flex cursor-pointer items-center gap-3 px-4 py-3 text-sm transition
        ${
          danger
            ? "text-red-400 hover:bg-red-500/10"
            : "text-white/80 hover:bg-zinc-800"
        }`}
    >
      {icon}
      <span>{label}</span>
    </div>
  );
}