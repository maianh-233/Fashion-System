import { useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, Eye, EyeOff, LockKeyhole, Mail, Sparkles } from "lucide-react";
import Button from "../../components/common/Button";
import ThemeToggle from "../../components/common/ThemeToggle";
import AuthSocialOptions from "../../components/customer/AuthSocialOptions";

export default function CustomerLogin() {
  const [isVisible, setIsVisible] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = (event) => {
    event.preventDefault();
    setLoading(true);

    window.setTimeout(() => {
      alert("Đăng nhập thành công! (Demo)");
      setLoading(false);
    }, 1500);
  };

  return (
    <main className="customer-auth">
      <div className="customer-auth__topbar">
        <Link className="customer-auth__home-link" to="/" aria-label="Về trang chủ Lunaria">
          <Sparkles size={17} aria-hidden="true" />
          <span>Lunaria</span>
        </Link>
        <ThemeToggle />
      </div>

      <section className="customer-auth__card" aria-labelledby="login-title">
        <div className="customer-auth__visual">
          <img
            src="https://images.unsplash.com/photo-1445205170230-053b83016050?q=80&w=1600&auto=format&fit=crop"
            alt="Không gian thời trang của Lunaria Boutique"
          />
          <div className="customer-auth__visual-overlay" />
          <div className="customer-auth__brand">
            <span className="customer-auth__eyebrow">Lunaria Boutique</span>
            <h1>Vẻ đẹp trong từng lựa chọn.</h1>
            <p>Thời trang thanh lịch, được tuyển chọn dành riêng cho bạn.</p>
          </div>
        </div>

        <div className="customer-auth__panel">
          <header className="customer-auth__heading">
            <span className="customer-auth__mobile-mark" aria-hidden="true">
              <Sparkles size={16} /> Lunaria Boutique
            </span>
            <h2 id="login-title">Chào mừng trở lại</h2>
            <p>Đăng nhập để tiếp tục trải nghiệm mua sắm.</p>
          </header>

          <form onSubmit={handleSubmit} className="customer-auth__form">
            <div className="customer-auth__field">
              <label htmlFor="login-email">Email</label>
              <div className="customer-auth__control">
                <Mail size={17} aria-hidden="true" />
                <input id="login-email" name="email" type="email" autoComplete="email" required placeholder="you@example.com" />
              </div>
            </div>

            <div className="customer-auth__field">
              <div className="customer-auth__label-row">
                <label htmlFor="login-password">Mật khẩu</label>
                <Link to="/forgot-password">Quên mật khẩu?</Link>
              </div>
              <div className="customer-auth__control">
                <LockKeyhole size={17} aria-hidden="true" />
                <input
                  id="login-password"
                  name="password"
                  type={isVisible ? "text" : "password"}
                  autoComplete="current-password"
                  required
                  placeholder="Nhập mật khẩu"
                />
                <button
                  type="button"
                  className="customer-auth__password-toggle"
                  onClick={() => setIsVisible((value) => !value)}
                  aria-label={isVisible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                  aria-pressed={isVisible}
                >
                  {isVisible ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <Button type="submit" variant="unstyled" loading={loading} className="customer-auth__submit">
              <span>{loading ? "Đang đăng nhập" : "Đăng nhập"}</span>
              {!loading && <ArrowRight size={17} aria-hidden="true" />}
            </Button>
          </form>

          <AuthSocialOptions />

          <p className="customer-auth__switch">
            Chưa có tài khoản? <Link to="/customerregister">Đăng ký ngay</Link>
          </p>
          <p className="customer-auth__legal">© 2026 Lunaria Boutique</p>
        </div>
      </section>
    </main>
  );
}
