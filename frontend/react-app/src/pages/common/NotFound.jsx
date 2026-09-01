import { ArrowLeft, ArrowRight, Home, ShoppingBag, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import Button from "../../components/common/Button";
import ThemeToggle from "../../components/common/ThemeToggle";

export default function NotFound() {
  return (
    <main className="not-found-page">
      <header className="not-found-page__topbar">
        <Link to="/" className="not-found-page__brand" aria-label="Về trang chủ Lunaria">
          <Sparkles size={17} aria-hidden="true" /><span>Lunaria</span>
        </Link>
        <ThemeToggle />
      </header>

      <section className="not-found-page__content" aria-labelledby="not-found-title">
        <div className="not-found-page__copy">
          <span className="not-found-page__eyebrow">Lạc giữa những trang phục?</span>
          <h1 id="not-found-title">Trang này đã rời khỏi bộ sưu tập.</h1>
          <p>Đường dẫn bạn mở không còn tồn tại hoặc đã được chuyển sang một vị trí mới. Mình đưa bạn trở lại nơi những thiết kế đẹp vẫn đang chờ nhé.</p>

          <div className="not-found-page__actions">
            <Link to="/" className="not-found-page__primary-action">
              <Home size={17} aria-hidden="true" /> Về trang chủ <ArrowRight size={16} aria-hidden="true" />
            </Link>
            <Button type="button" variant="unstyled" onClick={() => window.history.back()} className="not-found-page__back-action">
              <ArrowLeft size={17} aria-hidden="true" /> Quay lại trang trước
            </Button>
          </div>

          <Link to="/products" className="not-found-page__products-link">
            <ShoppingBag size={15} aria-hidden="true" /> Hoặc tiếp tục khám phá sản phẩm
          </Link>
        </div>

        <div className="not-found-page__visual" aria-hidden="true">
          <div className="not-found-page__number">404</div>
          <div className="not-found-page__image">
            <img src="https://images.unsplash.com/photo-1490481651871-ab68de25d43d?q=85&w=1200&auto=format&fit=crop" alt="" />
            <span>Lunaria · Since 2026</span>
          </div>
          <Sparkles className="not-found-page__sparkle" size={30} />
        </div>
      </section>

      <p className="not-found-page__footer">LUNARIA BOUTIQUE · HỒ CHÍ MINH</p>
    </main>
  );
}
