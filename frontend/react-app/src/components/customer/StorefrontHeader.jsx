import Button from "../common/Button";
import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Link, NavLink, useNavigate } from "react-router-dom";
import {
  ChevronDown,
  Heart,
  LogIn,
  LogOut,
  Menu,
  MoonStar,
  Package,
  TicketPercent,
  ShoppingBag,
  Star,
  User,
  X,
} from "lucide-react";
import ThemeToggle from "../common/ThemeToggle";
import { logoutCustomer } from "../../api/auth";
import { clearCustomerSession, getCustomerToken } from "../../api/http/customerSession";
import { useSystemNotification } from "../common/SystemNotification";
import { useCustomerSession } from "../../hooks/useCustomerSession";
import CustomerNotificationBell from "./CustomerNotificationBell";


function BrandMark() {
  return (
    <Link to="/" className="storefront-brand" aria-label="Lunaria - Trang chủ">
      <span className="storefront-brand__mark">L</span>
      <span className="storefront-brand__wordmark">LUNARIA</span>
    </Link>
  );
}

/** Chữ viết tắt từ họ tên (2 chữ cái đầu của 2 từ cuối). */
function initialsOf(name) {
  const words = String(name || "").trim().split(/\s+/).filter(Boolean);
  return (words.length > 1 ? words.slice(-2) : words).map((w) => w[0]).join("").toUpperCase() || "K";
}

export default function StorefrontHeader({ navLinks = [], cartCount = 0, wishlistCount = 0 }) {
  const { profile, loggedIn } = useCustomerSession();
  const displayName = profile?.fullName || profile?.username || "Tài khoản";
  const [openUser, setOpenUser] = useState(false);
  const [openMobileMenu, setOpenMobileMenu] = useState(false);
  const userRef = useRef(null);
  const navigate = useNavigate();
  const notification = useSystemNotification();

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

  const handleLogout = async () => {
    setOpenUser(false);
    const token = getCustomerToken();
    try {
      await logoutCustomer(token);
    } catch (error) {
      notification.warning(error?.message || "Phiên máy chủ đã hết hạn; dữ liệu đăng nhập cục bộ vẫn được xóa.");
    } finally {
      clearCustomerSession();
      navigate("/customerlogin", { replace: true });
    }
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
            {loggedIn && <CustomerNotificationBell />}
            <Link
              to="/wishlist"
              onClick={() => setOpenMobileMenu(false)}
              className="storefront-header__icon storefront-cart"
              aria-label={`Sản phẩm yêu thích, ${wishlistCount} sản phẩm`}
            >
              <Heart size={18} />
              {wishlistCount > 0 && (
                <span className="storefront-cart__count">{wishlistCount}</span>
              )}
            </Link>
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

            {!loggedIn && (
              <Link to="/customerlogin" className="storefront-user-trigger hidden sm:inline-flex" aria-label="Đăng nhập">
                <LogIn size={15} /> <span>Đăng nhập</span>
              </Link>
            )}
            {loggedIn && <div ref={userRef} className="storefront-user hidden sm:block">
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
                <span>{displayName}</span>
                <span className="storefront-user-trigger__avatar">{initialsOf(displayName)}</span>
                <ChevronDown size={13} className={openUser ? "rotate-180" : ""} />
              </button>

              {openUser && (
                <div className="storefront-user-menu" onClick={(event) => event.stopPropagation()}>
                  <DropdownItem icon={<User size={15} />} label="Thông tin cá nhân" to="/profile" onClick={() => setOpenUser(false)} />
                  <DropdownItem icon={<Package size={15} />} label="Đơn hàng của tôi" to="/orders" onClick={() => setOpenUser(false)} />
                  <DropdownItem icon={<TicketPercent size={15} />} label="Ví voucher" to="/vouchers" onClick={() => setOpenUser(false)} />
                  <DropdownItem icon={<Star size={15} />} label="Đánh giá của tôi" to="/reviews" onClick={() => setOpenUser(false)} />
                  <div className="storefront-user-menu__divider" />
                  <DropdownItem icon={<LogOut size={15} />} label="Đăng xuất" danger onClick={handleLogout} />
                </div>
              )}
            </div>}
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
              {loggedIn ? <>
                <NavLink to="/profile" onClick={() => setOpenMobileMenu(false)} className="storefront-mobile-nav__link"><User size={18} /> {displayName}</NavLink>
                <NavLink to="/orders" onClick={() => setOpenMobileMenu(false)} className="storefront-mobile-nav__link"><Package size={18} /> Đơn hàng của tôi</NavLink>
                <NavLink to="/vouchers" onClick={() => setOpenMobileMenu(false)} className="storefront-mobile-nav__link"><TicketPercent size={18} /> Ví voucher</NavLink>
                <NavLink to="/reviews" onClick={() => setOpenMobileMenu(false)} className="storefront-mobile-nav__link"><Star size={18} /> Đánh giá của tôi</NavLink>
                <button type="button" onClick={() => { setOpenMobileMenu(false); handleLogout(); }} className="storefront-mobile-nav__link"><LogOut size={18} /> Đăng xuất</button>
              </> : (
                <NavLink to="/customerlogin" onClick={() => setOpenMobileMenu(false)} className="storefront-mobile-nav__link"><LogIn size={18} /> Đăng nhập</NavLink>
              )}
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
