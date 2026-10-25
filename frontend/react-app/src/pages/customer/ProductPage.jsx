import { Link, useSearchParams } from "react-router-dom";
import { TicketPercent, X } from "lucide-react";
import ProductBrowser from "../../components/customer/Product/ProductBrowser";

/**
 * Danh sách sản phẩm phía khách (API storefront). {@code ?promotionId=&promotion=CODE} lọc các sản phẩm được một
 * khuyến mãi theo sản phẩm áp dụng (mở từ trang khuyến mãi); {@code ?categoryId=} chọn sẵn một danh mục (trang chủ).
 */
export default function ProductPage() {
  const [searchParams] = useSearchParams();
  const promotionId = searchParams.get("promotionId");
  const promotionCode = searchParams.get("promotion");
  const categoryId = searchParams.get("categoryId");

  return (
    <ProductBrowser
      key={`${promotionId || "all"}|${categoryId || ""}`}
      scope={{ promotionId }}
      initialCategoryIds={categoryId ? [categoryId] : []}
      intro={promotionId ? {
        eyebrow: "Ưu đãi theo sản phẩm",
        title: "Sản phẩm áp dụng",
        description: `Sản phẩm được giảm giá khi dùng mã ${promotionCode || "khuyến mãi"} lúc thanh toán.`,
      } : undefined}
      notice={promotionId && (
        <div className="customer-scope-chip">
          <TicketPercent size={15} /> Đang xem sản phẩm của mã <strong>{promotionCode || "khuyến mãi"}</strong>
          <Link to="/products" aria-label="Bỏ lọc theo khuyến mãi"><X size={14} /></Link>
        </div>
      )}
    />
  );
}
