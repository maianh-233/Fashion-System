import CollectionCard from "../Collection/CollectionCard";
import Pagination from "../../common/Pagination";
import { storefrontApi } from "../../../api/storefrontApi";
import { useServerPages } from "../../../hooks/useServerPages";

const PAGE_SIZE = 4;

/** Bộ sưu tập đang phát hành của một thương hiệu (API storefront). */
export default function CollectionSection({ brandId }) {
  const list = useServerPages(`brand=${brandId}`, (page, options) => storefrontApi.collections(
    new URLSearchParams({ brandId, page: String(page - 1), size: String(PAGE_SIZE) }), options));

  if (list.loaded && list.totalElements === 0) return null;
  return (
    <section id="collections" className="customer-brand-collections mb-16">
      <div className="mb-8 flex items-end justify-between">
        <h2 className="customer-page-title">Bộ sưu tập nổi bật</h2>
      </div>

      {list.error && <p className="text-sm text-zinc-400">{list.error}</p>}
      <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4 lg:gap-5" aria-busy={list.loading || undefined}>
        {list.loading && !list.items.length
          ? Array.from({ length: PAGE_SIZE }, (_, index) => <div key={index} className="customer-card customer-card--skeleton" aria-hidden="true" />)
          : list.items.map((collection) => <CollectionCard key={collection.id} collection={collection} />)}
      </div>

      <Pagination currentPage={list.page} totalPages={list.totalPages} onPageChange={list.setPage} />
    </section>
  );
}
