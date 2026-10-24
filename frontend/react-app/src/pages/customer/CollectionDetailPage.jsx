import { useSearchParams } from "react-router-dom";
import { Images } from "lucide-react";
import CollectionHero from "../../components/customer/Collection/CollectionHero";
import CollectionProducts from "../../components/customer/Collection/CollectionProducts";
import DetailState from "../../components/customer/DetailState";
import { storefrontApi } from "../../api/storefront/storefrontApi";
import { useStorefrontResource } from "../../hooks/useStorefrontResource";

/** Chi tiết bộ sưu tập: {@code /collectiondetail?id=<uuid>}. */
export default function CollectionDetailPage() {
  const [searchParams] = useSearchParams();
  const id = searchParams.get("id");
  const { data: collection, loading, error } = useStorefrontResource(id, storefrontApi.collection,
    "Bộ sưu tập không tồn tại hoặc chưa phát hành.");

  return (
    <div className="customer-page customer-detail-page min-h-screen w-full text-gray-100">
      <DetailState loading={loading} error={error} icon={Images} backTo="/collections" backLabel="Xem bộ sưu tập khác" />
      {collection && (
        <>
          <CollectionHero collection={collection} />
          <CollectionProducts collection={collection} />
        </>
      )}
    </div>
  );
}
