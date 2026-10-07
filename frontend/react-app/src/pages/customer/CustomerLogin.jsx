import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowRight, Eye, EyeOff, KeyRound, LockKeyhole, Mail, Sparkles, X } from "lucide-react";
import Button from "../../components/common/Button";
import ThemeToggle from "../../components/common/ThemeToggle";
import { useSystemNotification } from "../../components/common/SystemNotification";
import {
  loginCustomer,
  loginSocialCustomer,
  requestCustomerPasswordResetOtp,
  resendCustomerPasswordResetOtp,
  resetCustomerPassword,
  SOCIAL_PROVIDERS,
  verifyCustomerPasswordResetOtp,
} from "../../api/auth";
import { formatLoginLock, getLoginRetrySeconds } from "../../utils/auth/loginLock";

const CUSTOMER_TOKEN_KEY = "lunaria_customer_access_token";
const CUSTOMER_PROFILE_KEY = "lunaria_customer_profile";
const GOOGLE_SCRIPT_ID = "google-identity-services";
const resetSteps = Object.freeze({ EMAIL: "EMAIL", OTP: "OTP", PASSWORD: "PASSWORD" });

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

function customerErrorMessage(error, fallback) {
  if (error?.status === 401) return "Tên đăng nhập hoặc mật khẩu không đúng.";
  if (error?.status === 403) return "Tài khoản đang bị khóa hoặc chưa được phép đăng nhập.";
  if (error?.status === 429) {
    const seconds = getLoginRetrySeconds(error);
    return seconds > 0 ? `Bạn thao tác quá nhiều lần. Vui lòng thử lại sau ${formatLoginLock(seconds)}.` : "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau.";
  }
  return error?.message || fallback;
}

export default function CustomerLogin() {
  const [isVisible, setIsVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [loginError, setLoginError] = useState("");
  const [loginLockSeconds, setLoginLockSeconds] = useState(0);
  const [forgotOpen, setForgotOpen] = useState(false);
  const [resetStep, setResetStep] = useState(resetSteps.EMAIL);
  const [resetEmail, setResetEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [resetToken, setResetToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [resetLoading, setResetLoading] = useState(false);
  const [resetError, setResetError] = useState("");
  const [resendSeconds, setResendSeconds] = useState(0);
  const googleButtonRef = useRef(null);
  const notification = useSystemNotification();
  const navigate = useNavigate();

  useEffect(() => {
    if (loginLockSeconds <= 0) return undefined;
    const timer = window.setTimeout(() => setLoginLockSeconds((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearTimeout(timer);
  }, [loginLockSeconds]);

  useEffect(() => {
    if (resendSeconds <= 0) return undefined;
    const timer = window.setTimeout(() => setResendSeconds((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearTimeout(timer);
  }, [resendSeconds]);

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
          setLoginError("");
          try {
            const response = await loginSocialCustomer({ provider: SOCIAL_PROVIDERS.GOOGLE, token: credential });
            persistCustomerSession(response);
            notification.success("Đăng nhập Google thành công.");
            navigate("/", { replace: true });
          } catch (error) {
            const message = customerErrorMessage(error, "Không thể đăng nhập bằng Google.");
            setLoginError(message);
            notification.error(message);
          }
        },
      });
      google.accounts.id.renderButton(container, { theme: "outline", size: "large", text: "signin_with", width: 320 });
    }).catch((error) => setLoginError(error.message));
    return () => {
      active = false;
      container.replaceChildren();
    };
  }, [navigate, notification]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (loading || loginLockSeconds > 0) return;
    const form = new FormData(event.currentTarget);
    const username = String(form.get("username") || "").trim();
    const password = String(form.get("password") || "");
    if (!username || !password) return setLoginError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
    setLoading(true);
    setLoginError("");
    try {
      const response = await loginCustomer({ username, password });
      persistCustomerSession(response);
      notification.success("Đăng nhập thành công.");
      navigate("/", { replace: true });
    } catch (error) {
      const retrySeconds = getLoginRetrySeconds(error);
      if (retrySeconds > 0) setLoginLockSeconds(retrySeconds);
      setLoginError(customerErrorMessage(error, "Không thể đăng nhập lúc này."));
    } finally {
      setLoading(false);
    }
  };

  const closeForgot = () => {
    if (resetLoading) return;
    setForgotOpen(false);
    setResetStep(resetSteps.EMAIL);
    setOtp("");
    setResetToken("");
    setNewPassword("");
    setConfirmation("");
    setResetError("");
    setResendSeconds(0);
  };

  const requestOtp = async (event) => {
    event.preventDefault();
    const email = resetEmail.trim().toLowerCase();
    if (!email) return setResetError("Vui lòng nhập email tài khoản customer.");
    setResetLoading(true);
    setResetError("");
    try {
      await requestCustomerPasswordResetOtp({ email });
      setResetEmail(email);
      setResetStep(resetSteps.OTP);
      setResendSeconds(60);
      notification.success("Nếu email tồn tại, OTP đã được gửi và có hiệu lực trong 1 phút.");
    } catch (error) {
      setResetError(customerErrorMessage(error, "Không thể gửi OTP."));
    } finally {
      setResetLoading(false);
    }
  };

  const verifyOtp = async (event) => {
    event.preventDefault();
    if (!/^\d{6}$/.test(otp)) return setResetError("OTP phải gồm đúng 6 chữ số.");
    setResetLoading(true);
    setResetError("");
    try {
      const response = await verifyCustomerPasswordResetOtp({ email: resetEmail, otp });
      if (!response?.resetToken) throw new Error("Phiên xác minh OTP không hợp lệ.");
      setResetToken(response.resetToken);
      setOtp("");
      setResetStep(resetSteps.PASSWORD);
    } catch (error) {
      setResetError(error?.status === 400 ? "OTP không đúng, đã hết hạn hoặc đã được sử dụng." : customerErrorMessage(error, "Không thể xác minh OTP."));
    } finally {
      setResetLoading(false);
    }
  };

  const resendOtp = async () => {
    if (resetLoading || resendSeconds > 0) return;
    setResetLoading(true);
    setResetError("");
    try {
      await resendCustomerPasswordResetOtp({ email: resetEmail });
      setResendSeconds(60);
      notification.success("Nếu email tồn tại, một OTP mới đã được gửi.");
    } catch (error) {
      const retrySeconds = getLoginRetrySeconds(error);
      if (retrySeconds > 0) setResendSeconds(retrySeconds);
      setResetError(customerErrorMessage(error, "Không thể gửi lại OTP."));
    } finally {
      setResetLoading(false);
    }
  };

  const submitNewPassword = async (event) => {
    event.preventDefault();
    if (newPassword.length < 8 || newPassword.length > 72) return setResetError("Mật khẩu phải từ 8 đến 72 ký tự.");
    if (newPassword !== confirmation) return setResetError("Mật khẩu xác nhận không khớp.");
    setResetLoading(true);
    setResetError("");
    try {
      await resetCustomerPassword({ resetToken, newPassword });
      setResetLoading(false);
      notification.success("Đổi mật khẩu thành công. Bạn có thể đăng nhập ngay.");
      closeForgot();
    } catch (error) {
      setResetError(error?.status === 400 ? "Phiên đổi mật khẩu đã hết hạn hoặc đã được sử dụng." : customerErrorMessage(error, "Không thể đổi mật khẩu."));
      setResetLoading(false);
    }
  };

  return (
    <main className="customer-auth">
      <div className="customer-auth__topbar"><Link className="customer-auth__home-link" to="/" aria-label="Về trang chủ Lunaria"><Sparkles size={17} aria-hidden="true" /><span>Lunaria</span></Link><ThemeToggle /></div>
      <section className="customer-auth__card" aria-labelledby="login-title">
        <div className="customer-auth__visual"><img src="https://images.unsplash.com/photo-1445205170230-053b83016050?q=80&w=1600&auto=format&fit=crop" alt="Không gian thời trang của Lunaria Boutique" /><div className="customer-auth__visual-overlay" /><div className="customer-auth__brand"><span className="customer-auth__eyebrow">Lunaria Boutique</span><h1>Vẻ đẹp trong từng lựa chọn.</h1><p>Thời trang thanh lịch, được tuyển chọn dành riêng cho bạn.</p></div></div>
        <div className="customer-auth__panel">
          <header className="customer-auth__heading"><span className="customer-auth__mobile-mark" aria-hidden="true"><Sparkles size={16} /> Lunaria Boutique</span><h2 id="login-title">Chào mừng trở lại</h2><p>Đăng nhập để tiếp tục trải nghiệm mua sắm.</p></header>
          <form onSubmit={handleSubmit} className="customer-auth__form" noValidate>
            <div className="customer-auth__field"><label htmlFor="login-username">Tên đăng nhập</label><div className="customer-auth__control"><Mail size={17} aria-hidden="true" /><input id="login-username" name="username" type="text" autoComplete="username" maxLength={50} required placeholder="Tên đăng nhập" /></div></div>
            <div className="customer-auth__field"><div className="customer-auth__label-row"><label htmlFor="login-password">Mật khẩu</label><button type="button" className="customer-auth__password-toggle" onClick={() => setForgotOpen(true)}>Quên mật khẩu?</button></div><div className="customer-auth__control"><LockKeyhole size={17} aria-hidden="true" /><input id="login-password" name="password" type={isVisible ? "text" : "password"} autoComplete="current-password" maxLength={72} required placeholder="Nhập mật khẩu" /><button type="button" className="customer-auth__password-toggle" onClick={() => setIsVisible((value) => !value)} aria-label={isVisible ? "Ẩn mật khẩu" : "Hiện mật khẩu"} aria-pressed={isVisible}>{isVisible ? <EyeOff size={18} /> : <Eye size={18} />}</button></div></div>
            {loginError && <p className="customer-auth__error" role="alert">{loginError}</p>}
            <Button type="submit" variant="unstyled" loading={loading} disabled={loginLockSeconds > 0} className="customer-auth__submit"><span>{loginLockSeconds > 0 ? `Thử lại sau ${formatLoginLock(loginLockSeconds)}` : loading ? "Đang đăng nhập" : "Đăng nhập"}</span>{!loading && loginLockSeconds <= 0 && <ArrowRight size={17} aria-hidden="true" />}</Button>
          </form>
          <div className="customer-auth__social"><div className="customer-auth__divider"><span>hoặc</span></div><p>Đăng nhập nhanh với</p>{import.meta.env.VITE_GOOGLE_CLIENT_ID ? <div className="customer-auth__social-grid" ref={googleButtonRef} /> : <p className="customer-auth__error">Google Sign-In chưa được cấu hình.</p>}</div>
          <p className="customer-auth__switch">Chưa có tài khoản? <Link to="/customerregister">Đăng ký ngay</Link></p><p className="customer-auth__legal">© 2026 Lunaria Boutique</p>
        </div>
      </section>
      {forgotOpen && <div className="customer-profile-modal" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) closeForgot(); }}><section className="customer-profile-modal__dialog" role="dialog" aria-modal="true" aria-labelledby="customer-reset-title"><header className="customer-profile-modal__header"><span><KeyRound size={18} /></span><div><small>Bảo mật tài khoản</small><h3 id="customer-reset-title">Quên mật khẩu</h3></div><button type="button" onClick={closeForgot} aria-label="Đóng"><X size={17} /></button></header><div className="customer-profile-modal__body">
        {resetStep === resetSteps.EMAIL && <form className="customer-auth__form" onSubmit={requestOtp} noValidate><p>Nhập email customer để nhận OTP có hiệu lực trong 1 phút.</p><div className="customer-auth__field"><label htmlFor="customer-reset-email">Email</label><div className="customer-auth__control"><Mail size={17} /><input id="customer-reset-email" type="email" autoComplete="email" maxLength={255} value={resetEmail} onChange={(event) => setResetEmail(event.target.value)} required /></div></div>{resetError && <p className="customer-auth__error" role="alert">{resetError}</p>}<Button type="submit" variant="unstyled" loading={resetLoading} className="customer-auth__submit">Gửi OTP</Button></form>}
        {resetStep === resetSteps.OTP && <form className="customer-auth__form" onSubmit={verifyOtp} noValidate><p>Nhập OTP 6 số đã gửi đến <strong>{resetEmail}</strong>.</p><div className="customer-auth__field"><label htmlFor="customer-reset-otp">OTP</label><div className="customer-auth__control"><KeyRound size={17} /><input id="customer-reset-otp" inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={otp} onChange={(event) => setOtp(event.target.value.replace(/\D/g, "").slice(0, 6))} required /></div></div>{resetError && <p className="customer-auth__error" role="alert">{resetError}</p>}<Button type="submit" variant="unstyled" loading={resetLoading} className="customer-auth__submit">Xác minh OTP</Button><Button type="button" disabled={resetLoading || resendSeconds > 0} onClick={resendOtp}>{resendSeconds > 0 ? `Gửi lại sau ${formatLoginLock(resendSeconds)}` : "Gửi lại OTP"}</Button><Button type="button" disabled={resetLoading} onClick={() => { setResetStep(resetSteps.EMAIL); setOtp(""); setResetError(""); }}>Đổi email</Button></form>}
        {resetStep === resetSteps.PASSWORD && <form className="customer-auth__form" onSubmit={submitNewPassword} noValidate><p>Tạo mật khẩu mới từ 8 đến 72 ký tự.</p><div className="customer-auth__field"><label htmlFor="customer-new-password">Mật khẩu mới</label><div className="customer-auth__control"><LockKeyhole size={17} /><input id="customer-new-password" type="password" autoComplete="new-password" minLength={8} maxLength={72} value={newPassword} onChange={(event) => setNewPassword(event.target.value)} required /></div></div><div className="customer-auth__field"><label htmlFor="customer-confirm-password">Xác nhận mật khẩu</label><div className="customer-auth__control"><LockKeyhole size={17} /><input id="customer-confirm-password" type="password" autoComplete="new-password" minLength={8} maxLength={72} value={confirmation} onChange={(event) => setConfirmation(event.target.value)} required /></div></div>{resetError && <p className="customer-auth__error" role="alert">{resetError}</p>}<Button type="submit" variant="unstyled" loading={resetLoading} className="customer-auth__submit">Đặt mật khẩu mới</Button></form>}
      </div></section></div>}
    </main>
  );
}
