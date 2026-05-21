import { useState } from "react";
import { Link } from "react-router-dom";

export default function AdminLogin() {
  const [isVisible, setIsVisible] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();

    setLoading(true);

    // Demo giả lập API
    setTimeout(() => {
      alert("Đăng nhập thành công! (Demo)");
      setLoading(false);
    }, 1500);
  };

  return (
    <div className="bg-zinc-950 min-h-screen flex items-center justify-center p-4">
      <div className="w-full max-w-5xl bg-zinc-900/80 backdrop-blur-xl rounded-3xl shadow-2xl overflow-hidden border border-white/10 flex flex-col md:flex-row">

        {/* Left Side */}
        <div className="hidden md:flex md:w-1/2 relative bg-zinc-950 items-center justify-center overflow-hidden">
          <img
            src="https://images.unsplash.com/photo-1445205170230-053b83016050?q=80&w=2071"
            alt="Lunaria Boutique"
            className="absolute inset-0 w-full h-full object-cover"
          />

          <div className="absolute inset-0 bg-gradient-to-r from-black/70 via-black/50 to-transparent"></div>

          <div className="relative z-10 text-white text-center px-12">
            <div className="flex items-center justify-center gap-3 mb-6">
              <i className="fa-solid fa-sparkles text-amber-300 text-4xl"></i>

              <h1 className="text-5xl font-serif tracking-widest">
                LUNARIA
              </h1>
            </div>

            <p className="text-2xl font-light text-amber-100">
              BOUTIQUE
            </p>

            <p className="mt-8 text-lg text-zinc-300">
              Nơi ánh sáng dịu dàng chạm vào vẻ đẹp tinh tế
            </p>
          </div>
        </div>

        {/* Right Side */}
        <div className="w-full md:w-1/2 p-8 md:p-12 lg:p-16 flex flex-col justify-center">

          {/* Mobile Logo */}
          <div className="md:hidden flex justify-center mb-8">
            <div className="flex items-center gap-3">
              <i className="fa-solid fa-sparkles text-amber-300 text-3xl"></i>

              <h1 className="text-4xl font-serif tracking-widest text-white">
                LUNARIA
              </h1>
            </div>
          </div>

          <div className="mb-10">
            <h2 className="text-3xl font-semibold text-white">
              Chào mừng trở lại
            </h2>

            <p className="text-zinc-400 mt-2">
              Đăng nhập để quản lý hệ thóng và xem báo cáo doanh thu
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">

            {/* Email */}
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">
                Email
              </label>

              <div className="relative">
                <i className="fa-solid fa-envelope absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"></i>

                <input
                  type="email"
                  required
                  placeholder="you@example.com"
                  className="w-full bg-zinc-800 border border-white/10 focus:border-amber-400 rounded-2xl py-4 pl-12 pr-4 text-white placeholder:text-zinc-500 focus:outline-none transition-all"
                />
              </div>
            </div>

            {/* Password */}
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">
                Mật khẩu
              </label>

              <div className="relative">
                <i className="fa-solid fa-lock absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"></i>

                <input
                  type={isVisible ? "text" : "password"}
                  required
                  placeholder="••••••••"
                  className="w-full bg-zinc-800 border border-white/10 focus:border-amber-400 rounded-2xl py-4 pl-12 pr-12 text-white placeholder:text-zinc-500 focus:outline-none transition-all"
                />

                <button
                  type="button"
                  onClick={() => setIsVisible(!isVisible)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-white"
                >
                  <i
                    className={
                      isVisible
                        ? "fa-solid fa-eye-slash"
                        : "fa-solid fa-eye"
                    }
                  ></i>
                </button>
              </div>
            </div>

            {/* Forgot password */}
            <div className="flex justify-end">
              <Link
                to="/forgot-password"
                className="text-sm text-amber-400 hover:text-amber-300 font-medium"
              >
                Quên mật khẩu?
              </Link>
            </div>

            {/* Submit button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-white font-semibold py-4 rounded-2xl text-lg shadow-xl shadow-amber-500/40 transition-all active:scale-95 flex items-center justify-center gap-3"
            >
              {loading ? (
                <span className="inline-block w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
              ) : (
                "Đăng nhập"
              )}
            </button>

          </form>

          <div className="mt-auto pt-8 text-center">
            <p className="text-xs text-zinc-500">
              © 2026 Lunaria Boutique
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}