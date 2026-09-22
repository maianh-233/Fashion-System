import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowLeft, ArrowRight, KeyRound, LockKeyhole, Mail, ShieldCheck, Sparkles } from "lucide-react";
import Button from "../../components/common/Button";
import ThemeToggle from "../../components/common/ThemeToggle";
import { useSystemNotification } from "../../components/common/SystemNotification";
import {
  requestPasswordResetOtp,
  resendPasswordResetOtp,
  resetPassword,
  verifyPasswordResetOtp,
} from "../../api/auth";
import {
  buildEmployeeOtpPayload,
  normalizeOtp,
  validateNewPassword,
} from "../../utils/auth/passwordResetFlow";

const steps = {
  EMAIL: "EMAIL",
  OTP: "OTP",
  PASSWORD: "PASSWORD",
};

export default function AdminForgotPassword() {
  const [step, setStep] = useState(steps.EMAIL);
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [resetToken, setResetToken] = useState("");
  const [loading, setLoading] = useState(false);
  const notification = useSystemNotification();
  const navigate = useNavigate();

  const run = async (operation, errorTitle) => {
    if (loading) return;
    setLoading(true);
    try {
      await operation();
    } catch (requestError) {
      if (requestError?.name === "Error") notification.warning(requestError.message);
      else notification.error(requestError, { title: errorTitle });
    } finally {
      setLoading(false);
    }
  };

  const requestOtp = (event) => {
    event.preventDefault();
    run(async () => {
      const payload = buildEmployeeOtpPayload(email);
      await requestPasswordResetOtp(payload);
      setEmail(payload.email);
      setStep(steps.OTP);
      notification.success("Nếu email tồn tại, mã OTP đã được gửi đến hộp thư của bạn.");
    }, "Không thể gửi OTP");
  };

  const verifyOtp = (event) => {
    event.preventDefault();
    run(async () => {
      const payload = buildEmployeeOtpPayload(email);
      const response = await verifyPasswordResetOtp({ ...payload, otp: normalizeOtp(otp) });
      if (!response?.resetToken) throw new Error("Backend không trả về reset token hợp lệ.");
      setResetToken(response.resetToken);
      setOtp("");
      setStep(steps.PASSWORD);
      notification.success("OTP hợp lệ. Bạn có thể đặt mật khẩu mới.");
    }, "Xác minh OTP thất bại");
  };

  const resendOtp = () => run(async () => {
    await resendPasswordResetOtp(buildEmployeeOtpPayload(email));
    notification.success("Nếu email tồn tại, một mã OTP mới đã được gửi.");
  }, "Không thể gửi lại OTP");

  const submitPassword = (event) => {
    event.preventDefault();
    run(async () => {
      if (!resetToken) throw new Error("Phiên đặt lại mật khẩu không hợp lệ. Vui lòng yêu cầu OTP mới.");
      const newPassword = validateNewPassword(password, confirmation);
      await resetPassword({ resetToken, newPassword });
      setResetToken("");
      setPassword("");
      setConfirmation("");
      notification.success("Đổi mật khẩu thành công. Hãy đăng nhập bằng mật khẩu mới.");
      navigate("/adminlogin", { replace: true });
    }, "Đổi mật khẩu thất bại");
  };

  return <main className="customer-auth admin-auth">
    <div className="customer-auth__topbar">
      <Link className="customer-auth__home-link" to="/adminlogin"><ArrowLeft size={17} /> Quay lại đăng nhập</Link>
      <ThemeToggle />
    </div>

    <section className="customer-auth__card admin-auth__card" aria-labelledby="admin-reset-title">
      <div className="customer-auth__visual admin-auth__visual">
        <img src="https://images.unsplash.com/photo-1445205170230-053b83016050?q=80&w=1600&auto=format&fit=crop" alt="Không gian thời trang Lunaria" />
        <div className="customer-auth__visual-overlay" />
        <div className="customer-auth__brand">
          <span className="customer-auth__eyebrow"><Sparkles size={15} /> Lunaria Administration</span>
          <h1>Khôi phục truy cập an toàn.</h1>
          <p>OTP chỉ được gửi đến email của tài khoản nhân viên đang hoạt động.</p>
        </div>
      </div>

      <div className="customer-auth__panel admin-auth__panel">
        <header className="customer-auth__heading">
          <span className="admin-auth__badge"><ShieldCheck size={15} /> Xác minh nhân viên</span>
          <h2 id="admin-reset-title">Quên mật khẩu</h2>
          <p>{step === steps.EMAIL ? "Nhập email nhân viên để nhận OTP." : step === steps.OTP ? `Nhập mã OTP 6 số đã gửi tới ${email}.` : "Tạo mật khẩu mới cho tài khoản nội bộ."}</p>
        </header>

        {step === steps.EMAIL && <form onSubmit={requestOtp} className="customer-auth__form" noValidate>
          <div className="customer-auth__field"><label htmlFor="admin-reset-email">Email nhân viên</label><div className="customer-auth__control"><Mail size={17} /><input id="admin-reset-email" type="email" autoComplete="email" maxLength={255} required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="employee@lunaria.vn" /></div></div>
          <Button type="submit" variant="unstyled" loading={loading} className="customer-auth__submit"><span>Gửi mã OTP</span>{!loading && <ArrowRight size={17} />}</Button>
        </form>}

        {step === steps.OTP && <form onSubmit={verifyOtp} className="customer-auth__form" noValidate>
          <div className="customer-auth__field"><label htmlFor="admin-reset-otp">Mã OTP</label><div className="customer-auth__control"><KeyRound size={17} /><input id="admin-reset-otp" inputMode="numeric" autoComplete="one-time-code" maxLength={6} required value={otp} onChange={(event) => setOtp(event.target.value.replace(/\D/g, "").slice(0, 6))} placeholder="000000" /></div></div>
          <Button type="submit" variant="unstyled" loading={loading} className="customer-auth__submit"><span>Xác minh OTP</span>{!loading && <ArrowRight size={17} />}</Button>
          <Button type="button" disabled={loading} onClick={resendOtp}>Gửi lại OTP</Button>
          <Button type="button" disabled={loading} onClick={() => { setOtp(""); setStep(steps.EMAIL); }}>Đổi email</Button>
        </form>}

        {step === steps.PASSWORD && <form onSubmit={submitPassword} className="customer-auth__form" noValidate>
          <div className="customer-auth__field"><label htmlFor="admin-reset-password">Mật khẩu mới</label><div className="customer-auth__control"><LockKeyhole size={17} /><input id="admin-reset-password" type="password" autoComplete="new-password" minLength={8} maxLength={72} required value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Từ 8 đến 72 ký tự" /></div></div>
          <div className="customer-auth__field"><label htmlFor="admin-reset-confirmation">Xác nhận mật khẩu</label><div className="customer-auth__control"><LockKeyhole size={17} /><input id="admin-reset-confirmation" type="password" autoComplete="new-password" minLength={8} maxLength={72} required value={confirmation} onChange={(event) => setConfirmation(event.target.value)} placeholder="Nhập lại mật khẩu mới" /></div></div>
          <Button type="submit" variant="unstyled" loading={loading} className="customer-auth__submit"><span>Đặt mật khẩu mới</span>{!loading && <ArrowRight size={17} />}</Button>
        </form>}

        <p className="customer-auth__legal">Reset token chỉ tồn tại trong phiên hiện tại và chỉ dùng được một lần.</p>
      </div>
    </section>
  </main>;
}
