import { ShoppingBag } from "lucide-react";

function BrandMark() {
  return (
    <div className="flex items-center gap-3">
      <img 
        src="/LUNARIALOGO.png" 
        alt="Lunaria Logo" 
        className="h-10 w-auto"
        onError={(e) => {
          // Fallback to text if image doesn't load
          e.target.style.display = 'none';
          e.target.nextElementSibling.style.display = 'flex';
        }}
      />
      <div className="flex items-center gap-3 hidden" style={{display: 'flex'}}>
        <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-400 to-yellow-600 text-3xl font-bold text-black">
          L
        </div>
        <span className="text-3xl font-light tracking-[0.3em]">LUNARIA</span>
      </div>
    </div>
  );
}

export default function StorefrontHeader({ navLinks, cartCount }) {
  return (
    <header className="sticky top-0 z-50 border-b border-zinc-800 bg-zinc-900/90 backdrop-blur">
      <div className="flex w-full items-center justify-between gap-4 px-4 py-5 sm:px-6 lg:px-10 2xl:px-16">
        <BrandMark />

        <nav className="hidden items-center gap-8 text-sm font-medium lg:flex">
          {navLinks.map((link) => (
            <a key={link.id} href={link.href} className="transition hover:text-amber-400">
              {link.label}
            </a>
          ))}
        </nav>

        <div className="flex items-center gap-5">
          <button type="button" className="relative transition hover:text-amber-400" aria-label="Cart">
            <ShoppingBag size={20} />
            <span className="absolute -right-2 -top-2 flex h-4 w-4 items-center justify-center rounded-full bg-amber-400 text-[10px] font-semibold text-black">
              {cartCount}
            </span>
          </button>

          <div className="flex cursor-pointer items-center gap-3">
            <div className="text-right">
              <p className="text-sm font-medium">Do Anh</p>
            </div>
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-zinc-700 text-sm font-bold">DA</div>
          </div>
        </div>
      </div>
    </header>
  );
}
