import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Button from "../../common/Button";
import ProductMetadataPanel from "./ProductMetadataPanel";

export default function ProductDetailTabs({ product }) {
  const navigate = useNavigate();
  const [tab, setTab] = useState("general");

  const tabs = [
    ["general", "Thông tin chung"], ["images", "Hình ảnh"],
    ["attributes", "Thuộc tính & Tag"], ["variants", "Biến thể"],
  ];
  return <section className="mt-6 border-t border-zinc-800 pt-5">
    <div className="mb-4 flex flex-wrap gap-2" role="tablist">{tabs.map(([key, label]) => <Button key={key} permission={key === "variants" ? "PRODUCT_VARIANT_VIEW" : undefined} type="button" role="tab" aria-selected={tab === key} onClick={() => key === "variants" ? navigate(`/admin/product-variants?productId=${product.id}`) : setTab(key)}>{label}</Button>)}</div>
    {tab === "general" && <div className="space-y-2 text-sm text-zinc-300"><p>{product.code} · {product.name}</p><p>Slug: {product.slug || "—"}</p><p>{product.description || "Chưa có mô tả."}</p></div>}
    {tab === "images" && <div>{product.imageUrl ? <img src={product.imageUrl} alt={product.name} className="max-h-72 rounded-xl object-contain" /> : <p className="text-sm text-zinc-400">Chưa có ảnh sản phẩm.</p>}</div>}
    {tab === "attributes" && <ProductMetadataPanel productId={product.id} />}
  </section>;
}
