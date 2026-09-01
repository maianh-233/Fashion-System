import { useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  CalendarDays,
  Eye,
  EyeOff,
  LockKeyhole,
  Mail,
  Phone,
  Sparkles,
  UserRound,
} from "lucide-react";
import Button from "../../components/common/Button";
import ThemeToggle from "../../components/common/ThemeToggle";
import AuthSocialOptions from "../../components/customer/AuthSocialOptions";

export default function CustomerRegister() {
  const [showPassword, setShowPassword] = useState(false);

  const handleSubmit = (event) => {
    event.preventDefault();
    alert("Đăng ký tài khoản thành công! 🎉\nChào mừng bạn đến với LUNARIA BOUTIQUE.");
  };

  return (
    <main className="customer-auth customer-auth--register">
      <div className="customer-auth__topbar">
        <Link className="customer-auth__home-link" to="/" aria-label="Về trang chủ Lunaria">
          <Sparkles size={17} aria-hidden="true" />
          <span>Lunaria</span>
        </Link>
        <ThemeToggle />
      </div>

      <section className="customer-auth__card" aria-labelledby="register-title">
        <div className="customer-auth__visual">
          <img
            src="https://images.unsplash.com/photo-1445205170230-053b83016050?q=80&w=1600&auto=format&fit=crop"
            alt="Không gian thời trang của Lunaria Boutique"
          />
          <div className="customer-auth__visual-overlay" />
          <div className="customer-auth__brand">
            <span className="customer-auth__eyebrow">Lunaria Boutique</span>
            <h1>Phong cách bắt đầu từ chính bạn.</h1>
            <p>Trở thành thành viên để lưu lựa chọn và nhận những ưu đãi riêng.</p>
          </div>
        </div>

        <div className="customer-auth__panel">
          <header className="customer-auth__heading">
            <span className="customer-auth__mobile-mark" aria-hidden="true">
              <Sparkles size={16} /> Lunaria Boutique
            </span>
            <h2 id="register-title">Tạo tài khoản</h2>
            <p>Chỉ mất một phút để bắt đầu cùng Lunaria.</p>
          </header>

          <form onSubmit={handleSubmit} className="customer-auth__form customer-auth__form--register">
            <div className="customer-auth__field customer-auth__field--wide">
              <label htmlFor="register-name">Họ và tên</label>
              <div className="customer-auth__control">
                <UserRound size={17} aria-hidden="true" />
                <input id="register-name" name="name" type="text" autoComplete="name" required placeholder="Nguyễn Thị Hoa" />
              </div>
            </div>

            <div className="customer-auth__field">
              <label htmlFor="register-email">Email</label>
              <div className="customer-auth__control">
                <Mail size={17} aria-hidden="true" />
                <input id="register-email" name="email" type="email" autoComplete="email" required placeholder="you@example.com" />
              </div>
            </div>

            <div className="customer-auth__field">
              <label htmlFor="register-phone">Số điện thoại</label>
              <div className="customer-auth__control">
                <Phone size={17} aria-hidden="true" />
                <input id="register-phone" name="phone" type="tel" autoComplete="tel" inputMode="tel" required placeholder="0123 456 789" />
              </div>
            </div>

            <div className="customer-auth__field">
              <label htmlFor="register-gender">Giới tính</label>
              <div className="customer-auth__control customer-auth__control--select">
                <UserRound size={17} aria-hidden="true" />
                <select id="register-gender" name="gender" required defaultValue="">
                  <option value="" disabled>Chọn giới tính</option>
                  <option value="nam">Nam</option>
                  <option value="nu">Nữ</option>
                  <option value="khac">Khác</option>
                </select>
              </div>
            </div>

            <div className="customer-auth__field">
              <label htmlFor="register-birthday">Ngày sinh</label>
              <div className="customer-auth__control">
                <CalendarDays size={17} aria-hidden="true" />
                <input id="register-birthday" name="birthday" type="date" autoComplete="bday" required />
              </div>
            </div>

            <div className="customer-auth__field customer-auth__field--wide">
              <label htmlFor="register-password">Mật khẩu</label>
              <div className="customer-auth__control">
                <LockKeyhole size={17} aria-hidden="true" />
                <input
                  id="register-password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="new-password"
                  minLength={8}
                  required
                  placeholder="Tối thiểu 8 ký tự"
                />
                <button
                  type="button"
                  className="customer-auth__password-toggle"
                  onClick={() => setShowPassword((value) => !value)}
                  aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                  aria-pressed={showPassword}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <Button type="submit" variant="unstyled" className="customer-auth__submit customer-auth__field--wide">
              <span>Tạo tài khoản</span>
              <ArrowRight size={17} aria-hidden="true" />
            </Button>
          </form>

          <AuthSocialOptions mode="Đăng ký" />

          <p className="customer-auth__switch">
            Đã có tài khoản? <Link to="/customerlogin">Đăng nhập</Link>
          </p>
          <p className="customer-auth__legal">© 2026 Lunaria Boutique</p>
        </div>
      </section>
    </main>
  );
}
