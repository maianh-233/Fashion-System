import { useSearchParams } from "react-router-dom";
import { Store } from "lucide-react";
import BrandHero from "../../components/customer/Brand/BrandHero";
import BrandInfo from "../../components/customer/Brand/BrandInfo";
import CollectionSection from "../../components/customer/Brand/CollectionSection";
import ProductBrowser from "../../components/customer/Product/ProductBrowser";
import DetailState from "../../components/customer/DetailState";
import { storefrontApi } from "../../api/storefrontApi";
import { useStorefrontResource } from "../../hooks/useStorefrontResource";

/** Chi tiết thương hiệu: {@code /branddetail?id=<uuid>} — số liệu, bộ sưu tập và sản phẩm của thương hiệu. */
export default function BrandDetail() {
  const [searchParams] = useSearchParams();
  const id = searchParams.get("id");
  const { data: brand, loading, error } = useStorefrontResource(id, storefrontApi.brand,
    "Thương hiệu không tồn tại hoặc đã ngừng hợp tác.");

  return (
    <div className="customer-page customer-detail-page min-h-screen w-full text-gray-100">
      <DetailState loading={loading} error={error} icon={Store} backTo="/brand" backLabel="Xem thương hiệu khác" />
      {brand && (
        <>
          <BrandHero brand={brand} />
          <div className="customer-page__wide px-4 py-8 sm:px-6 sm:py-10">
            <BrandInfo brand={brand} />
            <CollectionSection brandId={brand.id} />
          </div>
          <section id="brand-products" className="pb-10">
            <ProductBrowser
              scope={{ brandId: brand.id }}
              intro={{ eyebrow: brand.name, title: "Sản phẩm của thương hiệu", description: "Tất cả thiết kế đang bán của thương hiệu này." }}
            />
          </section>
        </>
      )}
    </div>
  );
}
