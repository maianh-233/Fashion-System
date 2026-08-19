import Button from "../common/Button";
import { useState, useRef, useEffect } from "react";
import { createPortal } from "react-dom";
import { Link, NavLink } from "react-router-dom";
import {
  ShoppingBag,
  User,
  Package,
  LogOut,
  ChevronDown,
  Menu,
  X,
  MoonStar,
} from "lucide-react";
import ThemeToggle from "../common/ThemeToggle";

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
  useEffect(() => {
    if (!openMobileMenu) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const closeOnEscape = (event) => {
      if (event.key === "Escape") setOpenMobileMenu(false);
    };
    document.addEventListener("keydown", closeOnEscape);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [openMobileMenu]);

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
    <>
    <header className="sticky top-0 z-50 border-b border-zinc-800 bg-zinc-900/90 backdrop-blur">
      <div className="flex min-h-16 w-full items-center justify-between gap-2 px-3 py-2 sm:gap-4 sm:px-6 sm:py-4 lg:px-10 2xl:px-16">
        {/* LEFT */}
        <div className="flex items-center gap-4">
          <Button
            className="flex h-11 w-11 items-center justify-center rounded-xl lg:hidden"
            onClick={() => setOpenMobileMenu(!openMobileMenu)}
            aria-expanded={openMobileMenu}
            aria-controls="customer-mobile-menu"
            aria-label={openMobileMenu ? "Đóng menu" : "Mở menu"}
          >
            {openMobileMenu ? <X size={22} /> : <Menu size={22} />}
          </Button>

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
        <div className="flex items-center gap-2 sm:gap-5">
          <ThemeToggle className="hidden sm:inline-flex" />
          {/* CART */}
          <Link
            to="/carts"
            onClick={() => setOpenMobileMenu(false)}
            className="relative flex h-11 w-11 items-center justify-center rounded-xl transition hover:bg-zinc-800 hover:text-amber-400"
            aria-label={`Giỏ hàng, ${cartCount} sản phẩm`}
          >
            <ShoppingBag size={20} />
            {cartCount > 0 && (
              <span className="absolute -right-2 -top-2 flex h-4 w-4 items-center justify-center rounded-full bg-amber-400 text-[10px] font-semibold text-black">
                {cartCount}
              </span>
            )}
          </Link>

          {/* USER */}
          <div ref={userRef} className="relative hidden sm:block">
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

    </header>

    {openMobileMenu && createPortal(
      <div
        id="customer-mobile-menu"
        className="fixed inset-x-0 bottom-0 top-16 z-40 lg:hidden sm:top-[77px]"
      >
        <button
          type="button"
          className="absolute inset-0 h-full w-full cursor-default bg-black/60 backdrop-blur-sm"
          onClick={() => setOpenMobileMenu(false)}
          aria-label="Đóng menu"
          tabIndex={-1}
        />

        <aside
          className="relative h-full w-full overflow-y-auto border-r border-zinc-800 bg-zinc-950 shadow-2xl sm:max-w-sm"
          role="dialog"
          aria-modal="true"
          aria-label="Menu điều hướng"
        >
          <nav
            className="flex min-h-full flex-col px-4 py-4 pb-[max(1rem,env(safe-area-inset-bottom))] text-base"
            aria-label="Điều hướng mobile"
          >
            {navLinks.map((link) => (
              <NavLink
                key={link.id}
                to={link.href}
                onClick={() => setOpenMobileMenu(false)}
                className={({ isActive }) =>
                  `flex min-h-12 items-center rounded-xl px-4 py-3 font-medium ${
                    isActive
                      ? "bg-amber-400/10 text-amber-400"
                      : "text-white/80 hover:bg-zinc-900"
                  }`
                }
              >
                {link.label}
              </NavLink>
            ))}

            <div className="my-3 h-px bg-zinc-800" />
            <NavLink
              to="/profile"
              onClick={() => setOpenMobileMenu(false)}
              className="flex min-h-12 items-center gap-3 rounded-xl px-4 py-3 text-white/80 hover:bg-zinc-900"
            >
              <User size={20} /> Thông tin cá nhân
            </NavLink>
            <NavLink
              to="/orders"
              onClick={() => setOpenMobileMenu(false)}
              className="flex min-h-12 items-center gap-3 rounded-xl px-4 py-3 text-white/80 hover:bg-zinc-900"
            >
              <Package size={20} /> Đơn hàng của tôi
            </NavLink>
            <div className="mt-3 flex items-center justify-between rounded-xl bg-zinc-900 px-4 py-3">
              <span className="flex items-center gap-3 text-sm text-zinc-300">
                <MoonStar size={20} /> Giao diện
              </span>
              <ThemeToggle />
            </div>
          </nav>
        </aside>
      </div>,
      document.body,
    )}
    </>
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
