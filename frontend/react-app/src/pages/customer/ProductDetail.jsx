import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { LoaderCircle, PackageSearch } from "lucide-react";
import Breadcrumb from "../../components/customer/ProductDetail/Breadcrumb";
import ProductGallery from "../../components/customer/ProductDetail/ProductGallery";
import ProductInfo from "../../components/customer/ProductDetail/ProductInfo";
import { storefrontApi } from "../../api/storefront/storefrontApi";
import { galleryImages, initialSelection } from "./storefrontLogic";

/** Chi tiết sản phẩm phía khách: {@code /productdetail?id=<id hoặc slug>}, dữ liệu từ API storefront. */
export default function ProductDetail() {
  const [searchParams] = useSearchParams();
  const id = searchParams.get("id") || "";
  const [state, setState] = useState({ id: null, product: null, error: "" });
  const [color, setColor] = useState(null);

  useEffect(() => {
    if (!id) return undefined;
    const controller = new AbortController();
    storefrontApi.detail(id, { signal: controller.signal })
      .then((product) => {
        setState({ id, product, error: "" });
        setColor(initialSelection(product.variants).color);
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setState({ id, product: null, error: failure.status === 404
          ? "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh." : failure.message });
      });
    return () => controller.abort();
  }, [id]);

  const loading = Boolean(id) && state.id !== id;
  const product = loading ? null : state.product;
  const error = !id ? "Không tìm thấy sản phẩm." : loading ? "" : state.error;

  return (
    <div className="customer-page product-detail-page min-h-screen w-full px-4 py-6 text-gray-200 sm:px-6 sm:py-8">
      <div className="customer-page__wide">
        {loading && (
          <div className="flex min-h-96 items-center justify-center gap-2 text-sm text-zinc-400" aria-live="polite">
            <LoaderCircle className="animate-spin" size={18} /> Đang tải sản phẩm...
          </div>
        )}
        {error && (
          <div className="flex min-h-96 flex-col items-center justify-center gap-3 text-center">
            <PackageSearch className="text-zinc-500" size={36} />
            <p className="text-sm text-zinc-400">{error}</p>
            <Link to="/products" className="text-sm text-amber-400 underline">Xem sản phẩm khác</Link>
          </div>
        )}
        {product && (
          <>
            <Breadcrumb product={product} />
            <div className="customer-product-detail-grid">
              <ProductGallery key={`${product.id}-${color}`} name={product.name}
                images={galleryImages(product.images, color, product.name)}
                badge={product.tags.find((tag) => /limited/i.test(tag)) ? "Limited edition" : null} />
              <ProductInfo key={product.id} product={product} onColorChange={setColor} />
            </div>
          </>
        )}
      </div>
    </div>
  );
}
