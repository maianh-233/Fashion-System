import { useState } from "react";
import { Search, Store } from "lucide-react";
import BrandCard from "../../components/customer/Brand/BrandCard";
import CustomerPageIntro from "../../components/customer/CustomerPageIntro";
import ServerPagedList from "../../components/customer/ServerPagedList";
import { storefrontApi } from "../../api/storefront/storefrontApi";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { useServerPages } from "../../hooks/useServerPages";

const ITEMS_PER_PAGE = 12;

/** Thương hiệu đang hợp tác (API storefront), nhiều sản phẩm đứng trước. */
export default function BrandPage() {
  const [search, setSearch] = useState("");
  const keyword = useDebouncedValue(search.trim());
  const query = (page) => new URLSearchParams({ keyword, page: String(page - 1), size: String(ITEMS_PER_PAGE) });
  const list = useServerPages(`k=${keyword}`, (page, options) => storefrontApi.brands(query(page), options));

  return (
    <div className="customer-page min-h-screen w-full overflow-x-clip text-zinc-200">
      <div className="customer-page__wide">
        <CustomerPageIntro eyebrow="Curated maisons" title="Thương hiệu" description="Những nhà mốt được chọn lọc bởi ngôn ngữ thiết kế và chất lượng chế tác." meta={list.loaded ? `${list.totalElements} thương hiệu` : "Đang tải..."}>
          <div className="customer-search-control relative w-full">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400" size={18} />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Tìm kiếm thương hiệu, mã code..."
              aria-label="Tìm kiếm thương hiệu"
              className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-800 py-2.5 pl-11 pr-4 focus:border-amber-400 focus:outline-none sm:rounded-2xl"
            />
          </div>
        </CustomerPageIntro>
      </div>

      <main className="customer-page__wide px-4 py-5 sm:px-6 sm:py-7">
        <ServerPagedList
          list={list}
          itemLabel="thương hiệu"
          emptyIcon={Store}
          emptyText="Không tìm thấy thương hiệu phù hợp."
          skeleton={<div className="customer-card customer-card--skeleton" />}
          renderItem={(brand) => <BrandCard key={brand.id} brand={brand} />}
          onPageChange={() => window.scrollTo({ top: 0, behavior: "smooth" })}
        />
      </main>
    </div>
  );
}
