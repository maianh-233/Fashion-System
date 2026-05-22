function BrandBlock() {
  return (
    <div>
      <div className="mb-6">
        <img 
          src="/LUNARIALOGO.png" 
          alt="Lunaria Logo" 
          className="h-12 w-auto"
          onError={(e) => {
            // Fallback to text if image doesn't load
            e.target.style.display = 'none';
            e.target.nextElementSibling.style.display = 'flex';
          }}
        />
        <div className="flex items-center gap-3 hidden">
          <div className="flex h-9 w-9 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-400 to-yellow-600 text-2xl font-bold text-black">
            L
          </div>
          <span className="text-2xl font-light">LUNARIA</span>
        </div>
      </div>
      <p className="text-sm leading-relaxed text-zinc-400">
        Thuong hieu thoi trang cao cap Viet Nam, mang den nhung thiet ke thanh lich va tinh te cho nguoi phu nu hien dai.
      </p>
    </div>
  );
}

const footerColumns = [
  {
    title: "Kham Pha",
    links: ["Bo Suu Tap Moi", "San Pham Ban Chay", "Sale Off", "Cau Chuyen Thuong Hieu"],
  },
  {
    title: "Ho Tro",
    links: ["Chinh Sach Doi Tra", "Huong Dan Size", "Cau Hoi Thuong Gap", "Lien He Chung Toi"],
  },
];

export default function StorefrontFooter() {
  return (
    <footer className="border-t border-zinc-900 bg-black py-16">
      <div className="w-full px-4 sm:px-6 lg:px-10 2xl:px-16">
        <div className="grid grid-cols-2 gap-10 md:grid-cols-4">
          <BrandBlock />

          {footerColumns.map((column) => (
            <div key={column.title}>
              <h4 className="mb-4 font-medium">{column.title}</h4>
              <ul className="space-y-3 text-sm text-zinc-400">
                {column.links.map((link) => (
                  <li key={link}>
                    <a href="#" className="transition hover:text-white">
                      {link}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}

          <div>
            <h4 className="mb-4 font-medium">Lien He</h4>
            <p className="text-sm text-zinc-400">
              Hotline: <span className="text-white">1800 123 456</span>
            </p>
            <p className="mt-1 text-sm text-zinc-400">
              Email: <span className="text-white">hello@lunaria.vn</span>
            </p>
            <div className="mt-6 flex gap-4">
              <i className="fa-brands fa-instagram cursor-pointer text-xl transition hover:text-amber-400" />
              <i className="fa-brands fa-facebook cursor-pointer text-xl transition hover:text-amber-400" />
              <i className="fa-brands fa-tiktok cursor-pointer text-xl transition hover:text-amber-400" />
            </div>
          </div>
        </div>

        <div className="mt-12 border-t border-zinc-900 pt-8 text-center text-xs text-zinc-500">
          © 2026 LUNARIA. All Rights Reserved. | Privacy Policy | Terms of Service
        </div>
      </div>
    </footer>
  );
}
