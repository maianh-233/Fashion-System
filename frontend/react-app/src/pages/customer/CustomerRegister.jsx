import { useState } from "react";
import { Link } from "react-router-dom";
import {
  Sparkles,
  Mail,
  Phone,
  Lock,
  Eye,
  EyeOff,
} from "lucide-react";

export default function CustomerRegister() {
  const [showPassword, setShowPassword] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();

    // TODO: gọi API register ở đây
    alert(
      "Đăng ký tài khoản thành công! 🎉\nChào mừng bạn đến với LUNARIA BOUTIQUE."
    );
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
              <Sparkles className="text-amber-300 w-10 h-10" />
              <h1 className="text-5xl tracking-widest font-serif">
                LUNARIA
              </h1>
            </div>

            <p className="text-2xl font-light text-amber-100">
              BOUTIQUE
            </p>

            <p className="mt-8 text-lg text-zinc-300">
              Trở thành thành viên để nhận ưu đãi đặc biệt và trải nghiệm mua
              sắm tinh tế
            </p>
          </div>
        </div>

        {/* Right Side */}
        <div className="w-full md:w-1/2 p-4 md:p-6 lg:p-6 flex flex-col justify-center">
          
          {/* Mobile Logo */}
          <div className="md:hidden flex justify-center mb-8">
            <div className="flex items-center gap-3">
              <Sparkles className="text-amber-300 w-8 h-8" />
              <h1 className="text-4xl tracking-widest text-white font-serif">
                LUNARIA
              </h1>
            </div>
          </div>

          {/* Header */}
          <div className="mb-8">
            <h2 className="text-3xl font-semibold text-white">
              Tạo tài khoản mới
            </h2>

            <p className="text-zinc-400 mt-2">
              Tham gia LUNARIA BOUTIQUE ngay hôm nay
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-2">
            
            {/* Full Name */}
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">
                Họ và tên
              </label>

              <input
                type="text"
                required
                placeholder="Nguyễn Thị Hoa"
                className="w-full bg-zinc-800 border border-white/10 focus:border-amber-400 rounded-2xl py-4 px-5 text-white placeholder:text-zinc-500 focus:outline-none transition-all"
              />
            </div>

            {/* Email */}
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">
                Email
              </label>

              <div className="relative">
                <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500 w-5 h-5" />

                <input
                  type="email"
                  required
                  placeholder="you@example.com"
                  className="w-full bg-zinc-800 border border-white/10 focus:border-amber-400 rounded-2xl py-4 pl-12 pr-4 text-white placeholder:text-zinc-500 focus:outline-none transition-all"
                />
              </div>
            </div>

            {/* Phone */}
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">
                Số điện thoại
              </label>

              <div className="relative">
                <Phone className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500 w-5 h-5" />

                <input
                  type="tel"
                  required
                  placeholder="0123 456 789"
                  className="w-full bg-zinc-800 border border-white/10 focus:border-amber-400 rounded-2xl py-4 pl-12 pr-4 text-white placeholder:text-zinc-500 focus:outline-none transition-all"
                />
              </div>
            </div>

            {/* Gender + Birthday */}
            <div className="grid grid-cols-2 gap-4">
              
              {/* Gender */}
              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-2">
                  Giới tính
                </label>

                <select
                  required
                  className="w-full bg-zinc-800 border border-white/10 focus:border-amber-400 rounded-2xl py-4 px-5 text-white focus:outline-none transition-all"
                >
                  <option value="">Chọn giới tính</option>
                  <option value="nam">Nam</option>
                  <option value="nu">Nữ</option>
                  <option value="khac">Khác</option>
                </select>
              </div>

              {/* Birthday */}
              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-2">
                  Ngày sinh
                </label>

                <input
                  type="date"
                  required
                  className="w-full bg-zinc-800 border border-white/10 focus:border-amber-400 rounded-2xl py-4 px-5 text-white focus:outline-none transition-all"
                />
              </div>
            </div>

            {/* Password */}
            <div>
              <label className="block text-sm font-medium text-zinc-400 mb-2">
                Mật khẩu
              </label>

              <div className="relative">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500 w-5 h-5" />

                <input
                  type={showPassword ? "text" : "password"}
                  required
                  placeholder="••••••••"
                  className="w-full bg-zinc-800 border border-white/10 focus:border-amber-400 rounded-2xl py-4 pl-12 pr-12 text-white placeholder:text-zinc-500 focus:outline-none transition-all"
                />

                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-white"
                >
                  {showPassword ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
            </div>



            {/* Submit */}
            <button
              type="submit"
              className="w-full mt-6 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-white font-semibold py-4 rounded-2xl text-lg shadow-xl shadow-amber-500/40 transition-all active:scale-95 flex items-center justify-center gap-3"
            >
              Tạo tài khoản
            </button>

            {/* Login */}
            <div className="text-center text-sm text-zinc-400 mt-6">
              Đã có tài khoản?{" "}
              <Link
                to="/customerlogin"
                className="text-amber-400 hover:text-amber-300 font-medium"
              >
                Đăng nhập
              </Link>
            </div>
          </form>

          {/* Footer */}
          <div className="mt-auto pt-8 text-center">
            <p className="text-xs text-zinc-500">
              © 2026 Lunaria Boutique. All rights reserved.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}