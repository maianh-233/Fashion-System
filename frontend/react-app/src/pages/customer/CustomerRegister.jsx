import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowRight, Eye, EyeOff, LockKeyhole, Mail, Phone, Sparkles, UserRound } from "lucide-react";
import Button from "../../components/common/Button";
import ThemeToggle from "../../components/common/ThemeToggle";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { loginSocialCustomer, registerCustomer, SOCIAL_PROVIDERS } from "../../api/auth";

const CUSTOMER_TOKEN_KEY = "lunaria_customer_access_token";
const CUSTOMER_PROFILE_KEY = "lunaria_customer_profile";
const GOOGLE_SCRIPT_ID = "google-identity-services";

function loadGoogleIdentityServices() {
  if (window.google?.accounts?.id) return Promise.resolve(window.google);
  const existing = document.getElementById(GOOGLE_SCRIPT_ID);
  if (existing) {
    return new Promise((resolve, reject) => {
      existing.addEventListener("load", () => resolve(window.google), { once: true });
      existing.addEventListener("error", () => reject(new Error("Không thể tải Google Sign-In.")), { once: true });
    });
  }
  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.id = GOOGLE_SCRIPT_ID;
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = () => resolve(window.google);
    script.onerror = () => reject(new Error("Không thể tải Google Sign-In."));
    document.head.appendChild(script);
  });
}

function persistCustomerSession(response) {
  if (!response?.token || !response?.customer) throw new Error("Backend không trả về phiên customer hợp lệ.");
  window.sessionStorage.setItem(CUSTOMER_TOKEN_KEY, response.token);
  window.sessionStorage.setItem(CUSTOMER_PROFILE_KEY, JSON.stringify(response.customer));
}

function registerErrorMessage(error) {
  if (error?.status === 409) return error.message || "Tên đăng nhập hoặc email đã được sử dụng.";
  if (error?.status === 400) return error.message || "Thông tin đăng ký chưa hợp lệ.";
  if (error?.status === 429) return "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau.";
  return error?.message || "Không thể đăng ký lúc này.";
}

export default function CustomerRegister() {
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const googleButtonRef = useRef(null);
  const notification = useSystemNotification();
  const navigate = useNavigate();

  useEffect(() => {
    const container = googleButtonRef.current;
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    if (!container || !clientId) return undefined;
    let active = true;
    loadGoogleIdentityServices().then((google) => {
      if (!active || !google?.accounts?.id) return;
      google.accounts.id.initialize({
        client_id: clientId,
        callback: async ({ credential }) => {
          if (!credential) return;
          setError("");
          try {
            const response = await loginSocialCustomer({ provider: SOCIAL_PROVIDERS.GOOGLE, token: credential });
            persistCustomerSession(response);
            notification.success("Đăng ký bằng Google thành công.");
            navigate("/", { replace: true });
          } catch (requestError) {
            const message = registerErrorMessage(requestError);
            setError(message);
            notification.error(message);
          }
        },
      });
      google.accounts.id.renderButton(container, { theme: "outline", size: "large", text: "signup_with", width: 320 });
    }).catch((requestError) => setError(requestError.message));
    return () => {
      active = false;
      container.replaceChildren();
    };
  }, [navigate, notification]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (loading) return;
    const form = new FormData(event.currentTarget);
    const username = String(form.get("username") || "").trim().toLowerCase();
    const fullName = String(form.get("fullName") || "").trim();
    const email = String(form.get("email") || "").trim().toLowerCase();
    const phone = String(form.get("phone") || "").trim();
    const password = String(form.get("password") || "");
    const confirmation = String(form.get("confirmation") || "");
    if (username.length < 3 || username.length > 50) return setError("Tên đăng nhập phải từ 3 đến 50 ký tự.");
    if (fullName.length < 2 || fullName.length > 255) return setError("Họ và tên phải từ 2 đến 255 ký tự.");
    if (!email || email.length > 255) return setError("Email không hợp lệ.");
    if (!phone || phone.length > 20) return setError("Số điện thoại không hợp lệ.");
    if (password.length < 8 || password.length > 72) return setError("Mật khẩu phải từ 8 đến 72 ký tự.");
    if (password !== confirmation) return setError("Mật khẩu xác nhận không khớp.");

    setLoading(true);
    setError("");
    try {
      const response = await registerCustomer({ username, email, phone, password, fullName });
      persistCustomerSession(response);
      notification.success("Tạo tài khoản thành công.");
      navigate("/", { replace: true });
    } catch (requestError) {
      setError(registerErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="customer-auth customer-auth--register">
      <div className="customer-auth__topbar"><Link className="customer-auth__home-link" to="/" aria-label="Về trang chủ Lunaria"><Sparkles size={17} aria-hidden="true" /><span>Lunaria</span></Link><ThemeToggle /></div>
      <section className="customer-auth__card" aria-labelledby="register-title">
        <div className="customer-auth__visual"><img src="https://images.unsplash.com/photo-1445205170230-053b83016050?q=80&w=1600&auto=format&fit=crop" alt="Không gian thời trang của Lunaria Boutique" /><div className="customer-auth__visual-overlay" /><div className="customer-auth__brand"><span className="customer-auth__eyebrow">Lunaria Boutique</span><h1>Phong cách bắt đầu từ chính bạn.</h1><p>Trở thành thành viên để lưu lựa chọn và nhận những ưu đãi riêng.</p></div></div>
        <div className="customer-auth__panel">
          <header className="customer-auth__heading"><span className="customer-auth__mobile-mark" aria-hidden="true"><Sparkles size={16} /> Lunaria Boutique</span><h2 id="register-title">Tạo tài khoản</h2><p>Đăng ký bằng thông tin khớp với tài khoản customer.</p></header>
          <form onSubmit={handleSubmit} className="customer-auth__form customer-auth__form--register" noValidate>
            <div className="customer-auth__field"><label htmlFor="register-username">Tên đăng nhập</label><div className="customer-auth__control"><UserRound size={17} aria-hidden="true" /><input id="register-username" name="username" type="text" autoComplete="username" minLength={3} maxLength={50} required placeholder="Từ 3 đến 50 ký tự" /></div></div>
            <div className="customer-auth__field"><label htmlFor="register-name">Họ và tên</label><div className="customer-auth__control"><UserRound size={17} aria-hidden="true" /><input id="register-name" name="fullName" type="text" autoComplete="name" minLength={2} maxLength={255} required placeholder="Nguyễn Thị Hoa" /></div></div>
            <div className="customer-auth__field customer-auth__field--wide"><label htmlFor="register-email">Email</label><div className="customer-auth__control"><Mail size={17} aria-hidden="true" /><input id="register-email" name="email" type="email" autoComplete="email" maxLength={255} required placeholder="you@example.com" /></div></div>
            <div className="customer-auth__field customer-auth__field--wide"><label htmlFor="register-phone">Số điện thoại</label><div className="customer-auth__control"><Phone size={17} aria-hidden="true" /><input id="register-phone" name="phone" type="tel" autoComplete="tel" maxLength={20} required placeholder="0901234567" /></div></div>
            <div className="customer-auth__field"><label htmlFor="register-password">Mật khẩu</label><div className="customer-auth__control"><LockKeyhole size={17} aria-hidden="true" /><input id="register-password" name="password" type={showPassword ? "text" : "password"} autoComplete="new-password" minLength={8} maxLength={72} required placeholder="Từ 8 đến 72 ký tự" /><button type="button" className="customer-auth__password-toggle" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"} aria-pressed={showPassword}>{showPassword ? <EyeOff size={18} /> : <Eye size={18} />}</button></div></div>
            <div className="customer-auth__field"><label htmlFor="register-confirmation">Xác nhận mật khẩu</label><div className="customer-auth__control"><LockKeyhole size={17} aria-hidden="true" /><input id="register-confirmation" name="confirmation" type={showPassword ? "text" : "password"} autoComplete="new-password" minLength={8} maxLength={72} required placeholder="Nhập lại mật khẩu" /></div></div>
            {error && <p className="customer-auth__error customer-auth__field--wide" role="alert">{error}</p>}
            <Button type="submit" variant="unstyled" loading={loading} className="customer-auth__submit customer-auth__field--wide"><span>{loading ? "Đang tạo tài khoản" : "Tạo tài khoản"}</span>{!loading && <ArrowRight size={17} aria-hidden="true" />}</Button>
          </form>
          <div className="customer-auth__social"><div className="customer-auth__divider"><span>hoặc</span></div><p>Đăng ký nhanh với</p>{import.meta.env.VITE_GOOGLE_CLIENT_ID ? <div className="customer-auth__social-grid" ref={googleButtonRef} /> : <p className="customer-auth__error">Google Sign-In chưa được cấu hình.</p>}</div>
          <p className="customer-auth__switch">Đã có tài khoản? <Link to="/customerlogin">Đăng nhập</Link></p><p className="customer-auth__legal">© 2026 Lunaria Boutique</p>
        </div>
      </section>
    </main>
  );
}
