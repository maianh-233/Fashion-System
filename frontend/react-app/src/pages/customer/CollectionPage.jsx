import { useState } from "react";
import { Images, Search } from "lucide-react";
import CollectionCard from "../../components/customer/Collection/CollectionCard";
import CustomerPageIntro from "../../components/customer/CustomerPageIntro";
import ServerPagedList from "../../components/customer/ServerPagedList";
import { storefrontApi } from "../../api/storefrontApi";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { useServerPages } from "../../hooks/useServerPages";

const ITEMS_PER_PAGE = 12;

/** Bộ sưu tập đang phát hành (API storefront), tìm theo tên bộ sưu tập hoặc thương hiệu. */
export default function CollectionPage() {
  const [search, setSearch] = useState("");
  const keyword = useDebouncedValue(search.trim());
  const query = (page) => new URLSearchParams({ keyword, page: String(page - 1), size: String(ITEMS_PER_PAGE) });
  const list = useServerPages(`k=${keyword}`, (page, options) => storefrontApi.collections(query(page), options));

  return (
    <div className="customer-page min-h-screen w-full overflow-x-clip text-zinc-200">
      <div className="customer-page__wide">
        <CustomerPageIntro eyebrow="Seasonal stories" title="Bộ sưu tập" description="Mỗi bộ sưu tập là một câu chuyện riêng về chất liệu, màu sắc và tinh thần mùa." meta={list.loaded ? `${list.totalElements} bộ sưu tập` : "Đang tải..."}>
          <div className="customer-search-control relative w-full">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" size={18} />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Tìm kiếm bộ sưu tập hoặc thương hiệu..."
              aria-label="Tìm kiếm bộ sưu tập"
              className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 py-2.5 pl-11 pr-4 focus:border-amber-400 focus:outline-none sm:rounded-2xl"
            />
          </div>
        </CustomerPageIntro>
      </div>

      <main className="customer-page__wide px-4 py-5 sm:px-6 sm:py-7">
        <ServerPagedList
          list={list}
          itemLabel="bộ sưu tập"
          emptyIcon={Images}
          emptyText="Không tìm thấy bộ sưu tập phù hợp."
          skeleton={<div className="customer-card customer-card--skeleton" />}
          renderItem={(collection) => <CollectionCard key={collection.id} collection={collection} />}
          onPageChange={() => window.scrollTo({ top: 0, behavior: "smooth" })}
        />
      </main>
    </div>
  );
}
