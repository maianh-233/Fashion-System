import { ChevronRight, Home } from "lucide-react";
import { Link } from "react-router-dom";

export default function Breadcrumb({ product }) {
  return (
    <nav className="product-detail-breadcrumb" aria-label="Breadcrumb">
      <Link to="/"><Home size={14} /> Trang chủ</Link>
      <ChevronRight size={13} />
      <Link to="/products">Sản phẩm</Link>
      <ChevronRight size={13} />
      <span>{product.name}</span>
    </nav>
  );
}
