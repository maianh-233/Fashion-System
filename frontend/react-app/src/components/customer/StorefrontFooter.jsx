import { ArrowUpRight, Mail, MapPin, Phone } from "lucide-react";
import { Link } from "react-router-dom";

const footerColumns = [
  {
    title: "Khám phá",
    links: [
      { label: "Sản phẩm", to: "/products" },
      { label: "Bộ sưu tập", to: "/collections" },
      { label: "Thương hiệu", to: "/brand" },
      { label: "Ưu đãi", to: "/promotions" },
      { label: "Câu chuyện Lunaria", to: "/about#history" },
    ],
  },
  {
    title: "Dịch vụ",
    links: [
      { label: "Tài khoản của tôi", to: "/profile" },
      { label: "Theo dõi đơn hàng", to: "/orders" },
      { label: "Giỏ hàng", to: "/carts" },
      { label: "Chính sách đổi trả", to: "/about#returns" },
      { label: "Quy định cửa hàng", to: "/about#regulations" },
    ],
  },
];

function BrandBlock() {
  return (
    <div className="storefront-footer__brand">
      <div className="storefront-footer__wordmark">
        <span>L</span>
        <strong>LUNARIA</strong>
      </div>
      <p>
        Thời trang tuyển chọn dành cho nhịp sống hiện đại — thanh lịch trong
        đường nét, tinh tế trong từng chất liệu.
      </p>
    </div>
  );
}

export default function StorefrontFooter() {
  return (
    <footer className="storefront-footer">
      <div className="storefront-footer__inner">
        <div className="storefront-footer__statement">
          <div>
            <p>Curated fashion · Thoughtful service</p>
            <h2>Phong cách bền vững hơn những xu hướng chóng qua.</h2>
          </div>
          <Link to="/collections">
            Khám phá bộ sưu tập <ArrowUpRight size={15} />
          </Link>
        </div>

        <div className="storefront-footer__grid">
          <BrandBlock />

          {footerColumns.map((column) => (
            <div className="storefront-footer__column" key={column.title}>
              <h4>{column.title}</h4>
              <ul>
                {column.links.map((link) => (
                  <li key={link.label}>
                    <Link to={link.to}>{link.label}</Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}

          <div className="storefront-footer__column storefront-footer__contact">
            <h4>Liên hệ</h4>
            <a href="tel:1800123456"><Phone size={14} /> 1800 123 456</a>
            <a href="mailto:hello@lunaria.vn"><Mail size={14} /> hello@lunaria.vn</a>
            <p><MapPin size={14} /> Thành phố Hồ Chí Minh, Việt Nam</p>
          </div>
        </div>

        <div className="storefront-footer__bottom">
          <span>© 2026 LUNARIA</span>
          <span>Thanh lịch · Tinh giản · Bền vững</span>
        </div>
      </div>
    </footer>
  );
}
