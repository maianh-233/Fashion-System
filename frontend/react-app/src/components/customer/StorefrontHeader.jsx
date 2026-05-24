import { useState, useRef, useEffect } from "react";
import { Link, NavLink } from "react-router-dom";
import {
  ShoppingBag,
  User,
  Package,
  LogOut,
  ChevronDown,
  Menu,
  X,
} from "lucide-react";

/* ================= BRAND ================= */
function BrandMark() {
  return (
    <Link to="/" className="flex items-center gap-3">
      <img
        src="/LUNARIALOGO.png"
        alt="Lunaria Logo"
        className="h-9 w-auto"
        onError={(e) => {
          e.target.style.display = "none";
          e.target.nextElementSibling.style.display = "flex";
        }}
      />
      <div className="hidden items-center gap-3 sm:flex">
        <div className="flex h-9 w-9 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-400 to-yellow-600 text-xl font-bold text-black">
          L
        </div>
        <span className="text-xl font-light tracking-[0.3em]">LUNARIA</span>
      </div>
    </Link>
  );
}

/* ================= HEADER ================= */
export default function StorefrontHeader({ navLinks = [], cartCount = 0 }) {
  const [openUser, setOpenUser] = useState(false);
  const [openMobileMenu, setOpenMobileMenu] = useState(false);
  const userRef = useRef(null);

  // đóng dropdown user khi click ra ngoài
  useEffect(() => {
    const handler = (e) => {
      if (userRef.current && !userRef.current.contains(e.target)) {
        setOpenUser(false);
      }
    };

    document.addEventListener("click", handler);
    return () => document.removeEventListener("click", handler);
  }, []);

  const handleLogout = () => {
    setOpenUser(false);
    // TODO: logout logic ở đây
    console.log("Logout");
  };

  return (
    <header className="sticky top-0 z-50 border-b border-zinc-800 bg-zinc-900/90 backdrop-blur">
      <div className="flex w-full items-center justify-between gap-4 px-4 py-3 sm:px-6 sm:py-4 lg:px-10 2xl:px-16">
        {/* LEFT */}
        <div className="flex items-center gap-4">
          <button
            className="lg:hidden"
            onClick={() => setOpenMobileMenu(!openMobileMenu)}
          >
            {openMobileMenu ? <X size={22} /> : <Menu size={22} />}
          </button>

          <BrandMark />
        </div>

        {/* DESKTOP NAV */}
        <nav className="hidden items-center gap-8 text-sm font-medium lg:flex">
          {navLinks.map((link) => (
            <NavLink
              key={link.id}
              to={link.href}
              className={({ isActive }) =>
                `transition ${
                  isActive ? "text-amber-400" : "text-white/80"
                } hover:text-amber-300`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>

        {/* RIGHT */}
        <div className="flex items-center gap-4 sm:gap-5">
          {/* CART */}
          <Link
            to="/carts"
            className="relative transition hover:text-amber-400"
          >
            <ShoppingBag size={20} />
            {cartCount > 0 && (
              <span className="absolute -right-2 -top-2 flex h-4 w-4 items-center justify-center rounded-full bg-amber-400 text-[10px] font-semibold text-black">
                {cartCount}
              </span>
            )}
          </Link>

          {/* USER */}
          <div ref={userRef} className="relative">
            {/* Trigger */}
            <div
              onClick={(e) => {
                e.stopPropagation(); // ⭐ FIX LỖI CHÍNH
                setOpenUser((prev) => !prev);
              }}
              className="flex cursor-pointer items-center gap-2 rounded-xl px-2 py-1 transition hover:bg-zinc-800"
            >
              <div className="hidden text-right leading-tight sm:block">
                <p className="text-sm font-medium">Phạm Văn Minh Trang</p>
              </div>

              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-zinc-700 text-xs font-bold">
                DA
              </div>

              <ChevronDown
                size={14}
                className={`transition ${openUser ? "rotate-180" : ""}`}
              />
            </div>

            {/* Dropdown */}
            {openUser && (
              <div
                onClick={(e) => e.stopPropagation()}
                className="absolute right-0 mt-3 w-[90vw] sm:w-56 overflow-hidden rounded-2xl border border-zinc-700 bg-zinc-900 shadow-xl"
              >
                <DropdownItem
                  icon={<User size={16} />}
                  label="Thông tin cá nhân"
                  to="/profile"
                  onClick={() => setOpenUser(false)}
                />

                <DropdownItem
                  icon={<Package size={16} />}
                  label="Đơn hàng của tôi"
                  to="/orders"
                  onClick={() => setOpenUser(false)}
                />

                <div className="my-1 h-px bg-zinc-700" />

                <DropdownItem
                  icon={<LogOut size={16} />}
                  label="Đăng xuất"
                  danger
                  onClick={handleLogout}
                />
              </div>
            )}
          </div>
        </div>
      </div>

      {/* MOBILE NAV */}
      {openMobileMenu && (
        <div className="lg:hidden border-t border-zinc-800 bg-zinc-900">
          <nav className="flex flex-col gap-4 px-6 py-5 text-sm">
            {navLinks.map((link) => (
              <NavLink
                key={link.id}
                to={link.href}
                onClick={() => setOpenMobileMenu(false)}
                className={({ isActive }) =>
                  `py-2 ${isActive ? "text-amber-400" : "text-white/80"}`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
        </div>
      )}
    </header>
  );
}

/* ================= DROPDOWN ITEM ================= */
function DropdownItem({ icon, label, danger, to, onClick }) {
  const classes = `flex items-center gap-3 px-5 py-4 text-sm transition cursor-pointer
    ${
      danger
        ? "text-red-400 hover:bg-red-500/10"
        : "text-white/80 hover:bg-zinc-800"
    }`;

  const content = (
    <div className={classes} onClick={onClick}>
      {icon}
      <span>{label}</span>
    </div>
  );

  if (to) {
    return (
      <Link to={to} onClick={onClick}>
        {content}
      </Link>
    );
  }

  return content;
}
