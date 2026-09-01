import Button from "../common/Button";
import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Link, NavLink } from "react-router-dom";
import {
  ChevronDown,
  LogOut,
  Menu,
  MoonStar,
  Package,
  ShoppingBag,
  User,
  X,
} from "lucide-react";
import ThemeToggle from "../common/ThemeToggle";

function BrandMark() {
  return (
    <Link to="/" className="storefront-brand" aria-label="Lunaria - Trang chủ">
      <span className="storefront-brand__mark">L</span>
      <span className="storefront-brand__wordmark">LUNARIA</span>
    </Link>
  );
}

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

  useEffect(() => {
    const closeOnOutsideClick = (event) => {
      if (userRef.current && !userRef.current.contains(event.target)) {
        setOpenUser(false);
      }
    };
    document.addEventListener("click", closeOnOutsideClick);
    return () => document.removeEventListener("click", closeOnOutsideClick);
  }, []);

  useEffect(() => {
    const desktopQuery = window.matchMedia("(min-width: 1024px)");
    const closeOnDesktop = (event) => {
      if (event.matches) setOpenMobileMenu(false);
    };
    desktopQuery.addEventListener("change", closeOnDesktop);
    return () => desktopQuery.removeEventListener("change", closeOnDesktop);
  }, []);

  const handleLogout = () => {
    setOpenUser(false);
    // Presentation only until the authentication endpoint is connected.
  };

  return (
    <>
      <header className="storefront-header">
        <div className="storefront-header__inner">
          <div className="storefront-header__left">
            <Button
              type="button"
              variant="unstyled"
              className="storefront-header__icon storefront-menu-trigger"
              onClick={() => setOpenMobileMenu((current) => !current)}
              aria-expanded={openMobileMenu}
              aria-controls="customer-mobile-menu"
              aria-label={openMobileMenu ? "Đóng menu" : "Mở menu"}
            >
              {openMobileMenu ? <X size={19} /> : <Menu size={19} />}
            </Button>
            <BrandMark />
          </div>

          <nav className="storefront-nav hidden lg:flex" aria-label="Điều hướng chính">
            {navLinks.map((link) => (
              <NavLink
                key={link.id}
                to={link.href}
                end={link.href === "/"}
                className={({ isActive }) =>
                  `storefront-nav__link ${isActive ? "is-active" : ""}`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>

          <div className="storefront-header__actions">
            <ThemeToggle className="hidden sm:inline-flex" />
            <Link
              to="/carts"
              onClick={() => setOpenMobileMenu(false)}
              className="storefront-header__icon storefront-cart"
              aria-label={`Giỏ hàng, ${cartCount} sản phẩm`}
            >
              <ShoppingBag size={18} />
              {cartCount > 0 && (
                <span className="storefront-cart__count">{cartCount}</span>
              )}
            </Link>

            <div ref={userRef} className="storefront-user hidden sm:block">
              <button
                type="button"
                className="storefront-user-trigger"
                onClick={(event) => {
                  event.stopPropagation();
                  setOpenUser((current) => !current);
                }}
                aria-expanded={openUser}
                aria-label="Mở menu tài khoản"
              >
                <span>Minh Trang</span>
                <span className="storefront-user-trigger__avatar">MT</span>
                <ChevronDown size={13} className={openUser ? "rotate-180" : ""} />
              </button>

              {openUser && (
                <div className="storefront-user-menu" onClick={(event) => event.stopPropagation()}>
                  <DropdownItem icon={<User size={15} />} label="Thông tin cá nhân" to="/profile" onClick={() => setOpenUser(false)} />
                  <DropdownItem icon={<Package size={15} />} label="Đơn hàng của tôi" to="/orders" onClick={() => setOpenUser(false)} />
                  <div className="storefront-user-menu__divider" />
                  <DropdownItem icon={<LogOut size={15} />} label="Đăng xuất" danger onClick={handleLogout} />
                </div>
              )}
            </div>
          </div>
        </div>
      </header>

      {openMobileMenu && createPortal(
        <div id="customer-mobile-menu" className="storefront-mobile-menu lg:hidden">
          <button type="button" className="storefront-mobile-menu__overlay" onClick={() => setOpenMobileMenu(false)} aria-label="Đóng menu" tabIndex={-1} />
          <aside className="storefront-mobile-menu__panel" role="dialog" aria-modal="true" aria-label="Menu điều hướng">
            <nav className="storefront-mobile-nav" aria-label="Điều hướng mobile">
              {navLinks.map((link) => (
                <NavLink key={link.id} to={link.href} end={link.href === "/"} onClick={() => setOpenMobileMenu(false)} className={({ isActive }) => `storefront-mobile-nav__link ${isActive ? "is-active" : ""}`}>
                  {link.label}
                </NavLink>
              ))}
              <div className="storefront-mobile-nav__divider" />
              <NavLink to="/profile" onClick={() => setOpenMobileMenu(false)} className="storefront-mobile-nav__link"><User size={18} /> Thông tin cá nhân</NavLink>
              <NavLink to="/orders" onClick={() => setOpenMobileMenu(false)} className="storefront-mobile-nav__link"><Package size={18} /> Đơn hàng của tôi</NavLink>
              <div className="storefront-mobile-nav__theme">
                <span><MoonStar size={18} /> Giao diện</span>
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

function DropdownItem({ icon, label, danger, to, onClick }) {
  const className = `storefront-user-menu__item ${danger ? "is-danger" : ""}`;

  if (to) {
    return <Link to={to} onClick={onClick} className={className}>{icon}{label}</Link>;
  }

  return <button type="button" className={className} onClick={onClick}>{icon}{label}</button>;
}
