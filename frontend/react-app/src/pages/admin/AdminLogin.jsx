import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowRight, Eye, EyeOff, LockKeyhole, Mail, Monitor, ShieldCheck, Sparkles } from "lucide-react";
import Button from "../../components/common/Button";
import ThemeToggle from "../../components/common/ThemeToggle";
import { loginEmployee } from "../../hooks/auth";
import { saveAdminSession } from "../../hooks/auth/adminSession";

export default function AdminLogin() {
  const [isVisible, setIsVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    const form = new FormData(event.currentTarget);
    try {
      const response = await loginEmployee({ username: form.get("username"), password: form.get("password") });
      saveAdminSession(response);
      navigate("/admin", { replace: true });
    } catch (requestError) {
      setError(requestError.message || "Không thể đăng nhập hệ thống.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="customer-auth admin-auth">
      <div className="customer-auth__topbar">
        <Link className="customer-auth__home-link" to="/" aria-label="Về trang chủ Lunaria">
          <Sparkles size={17} aria-hidden="true" />
          <span>Lunaria · Cổng quản trị</span>
        </Link>
        <ThemeToggle />
      </div>

      <section className="customer-auth__card admin-auth__card" aria-labelledby="admin-login-title">
        <div className="customer-auth__visual admin-auth__visual">
          <img src="https://images.unsplash.com/photo-1445205170230-053b83016050?q=80&w=1600&auto=format&fit=crop" alt="Không gian thời trang của Lunaria Boutique" />
          <div className="customer-auth__visual-overlay" />
          <div className="customer-auth__brand">
            <span className="customer-auth__eyebrow">Lunaria Administration</span>
            <h1>Vận hành tinh gọn. Trải nghiệm nhất quán.</h1>
            <p>Không gian quản trị dành riêng cho đội ngũ Lunaria Boutique.</p>
          </div>
        </div>

        <div className="customer-auth__panel admin-auth__panel">
          <header className="customer-auth__heading">
            <span className="admin-auth__badge"><ShieldCheck size={15} aria-hidden="true" /> Truy cập nội bộ</span>
            <h2 id="admin-login-title">Chào mừng trở lại</h2>
            <p>Sử dụng tài khoản được hệ thống cấp để tiếp tục.</p>
          </header>

          <form onSubmit={handleSubmit} className="customer-auth__form">
            <div className="customer-auth__field">
              <label htmlFor="admin-username">Tên đăng nhập</label>
              <div className="customer-auth__control">
                <Mail size={17} aria-hidden="true" />
                <input id="admin-username" name="username" type="text" autoComplete="username" required placeholder="Nhập tên đăng nhập được cấp" />
              </div>
            </div>

            <div className="customer-auth__field">
              <div className="customer-auth__label-row">
                <label htmlFor="admin-password">Mật khẩu</label>
                <Link to="/forgot-password">Quên mật khẩu?</Link>
              </div>
              <div className="customer-auth__control">
                <LockKeyhole size={17} aria-hidden="true" />
                <input id="admin-password" name="password" type={isVisible ? "text" : "password"} autoComplete="current-password" required placeholder="Nhập mật khẩu" />
                <button type="button" className="customer-auth__password-toggle" onClick={() => setIsVisible((value) => !value)} aria-label={isVisible ? "Ẩn mật khẩu" : "Hiện mật khẩu"} aria-pressed={isVisible}>
                  {isVisible ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <Button type="submit" variant="unstyled" loading={loading} className="customer-auth__submit">
              <span>{loading ? "Đang xác thực" : "Đăng nhập hệ thống"}</span>
              {!loading && <ArrowRight size={17} aria-hidden="true" />}
            </Button>
            {error && <p className="customer-auth__error" role="alert">{error}</p>}
          </form>

          <div className="admin-auth__support">
            <ShieldCheck size={16} aria-hidden="true" />
            <p>Tài khoản quản trị do hệ thống cấp. Liên hệ quản trị viên nếu bạn chưa có quyền truy cập.</p>
          </div>
          <p className="customer-auth__legal">© 2026 Lunaria Boutique · Hệ thống nội bộ</p>
        </div>
      </section>

      <section className="admin-auth__desktop-notice" aria-labelledby="desktop-only-title">
        <span><Monitor size={28} aria-hidden="true" /></span>
        <h1 id="desktop-only-title">Vui lòng sử dụng máy tính</h1>
        <p>Cổng quản trị Lunaria được thiết kế cho màn hình desktop để bảo đảm thao tác vận hành chính xác.</p>
        <Link to="/">Về trang mua sắm</Link>
      </section>
    </main>
  );
}
