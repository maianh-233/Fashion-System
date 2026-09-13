export default function AuthSocialOptions({ mode = "Đăng nhập" }) {
  return (
    <div className="customer-auth__social">
      <div className="customer-auth__divider">
        <span>hoặc</span>
      </div>
      <p>{mode} nhanh với</p>
      <div className="customer-auth__social-grid">
        <button type="button" aria-label={`${mode} bằng Google`}>
          <i className="fa-brands fa-google" aria-hidden="true" />
          <span>Google</span>
        </button>
      </div>
    </div>
  );
}
