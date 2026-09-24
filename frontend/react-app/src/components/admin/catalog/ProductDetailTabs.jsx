import { useNavigate } from "react-router-dom";
import Button from "../../common/Button";
import ProductMetadataPanel from "./ProductMetadataPanel";

export default function ProductDetailTabs({ product, mode }) {
  const navigate = useNavigate();

  return <section className="mt-6 space-y-4 border-t border-zinc-800 pt-5">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h3 className="text-lg font-semibold text-zinc-100">Thuộc tính & tag</h3>
        <p className="text-sm text-zinc-400">Thông tin bổ sung của {product.name}.</p>
      </div>
      <Button permission="PRODUCT_VARIANT_VIEW" type="button" onClick={() => navigate(`/admin/product-variants?productId=${product.id}`)}>Danh sách biến thể</Button>
    </div>
    <ProductMetadataPanel productId={product.id} mode={mode} />
  </section>;
}
