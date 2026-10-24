import { Link } from "react-router-dom";
import { Heart, LoaderCircle } from "lucide-react";
import ProductCard from "../../components/customer/Product/ProductCard";
import ServerPagedList from "../../components/customer/ServerPagedList";
import { storefrontApi } from "../../api/storefront/storefrontApi";
import { useCustomerWishlist } from "../../contexts/CustomerWishlistContext";
import { useCustomerSession } from "../../hooks/useCustomerSession";
import { useServerPages } from "../../hooks/useServerPages";
import { WISHLIST_PAGE_SIZE, orderCards, pageIds } from "./wishlistLogic";

/** Trang của danh sách yêu thích: thẻ sản phẩm lấy theo id, giữ thứ tự mới thêm trước. */
function fetchWishlistPage(ids, page, options) {
  const chunk = pageIds(ids, page);
  const totalPages = Math.max(1, Math.ceil(ids.length / WISHLIST_PAGE_SIZE));
  const pageInfo = { totalElements: ids.length, totalPages };
  if (!chunk.length) return Promise.resolve({ content: [], page: pageInfo });
  const query = new URLSearchParams({ size: String(chunk.length), page: "0" });
  chunk.forEach((id) => query.append("ids", id));
  return storefrontApi.products(query, options).then((data) => ({ content: orderCards(data.content || [], chunk), page: pageInfo }));
}

/** Sản phẩm yêu thích (khách chưa đăng nhập: lưu trên trình duyệt, gộp vào tài khoản khi đăng nhập). */
export default function WishlistPage() {
  const wishlist = useCustomerWishlist();
  const { loggedIn } = useCustomerSession();
  const ids = wishlist?.ids || [];
  const list = useServerPages(ids.join(","), (page, options) => fetchWishlistPage(ids, page, options));

  return (
    <div className="customer-page min-h-screen w-full text-zinc-200">
      <div className="customer-page__wide px-4 py-5 sm:px-6 sm:py-8">
        <div className="mb-6 sm:mb-8">
          <h1 className="customer-page-title">Sản phẩm yêu thích</h1>
          <p className="mt-1 text-sm text-zinc-400">
            {wishlist?.loaded ? `${ids.length} sản phẩm` : "Đang tải..."}
            {!loggedIn && ids.length > 0 && (
              <> · <Link to={`/customerlogin?redirect=${encodeURIComponent("/wishlist")}`} className="text-amber-400 underline">Đăng nhập</Link> để lưu vào tài khoản</>
            )}
          </p>
        </div>

        {!wishlist?.loaded ? (
          <p className="flex items-center gap-2 text-sm text-zinc-400"><LoaderCircle className="animate-spin" size={16} /> Đang tải danh sách...</p>
        ) : ids.length === 0 ? (
          <div className="wishlist-empty">
            <Heart size={30} />
            <h2>Chưa có sản phẩm yêu thích</h2>
            <p>Bấm biểu tượng trái tim trên sản phẩm để lưu lại và xem sau.</p>
            <Link to="/products" className="checkout-primary-button">Khám phá sản phẩm</Link>
          </div>
        ) : (
          <ServerPagedList
            list={list}
            itemLabel="sản phẩm"
            emptyIcon={Heart}
            emptyText="Các sản phẩm yêu thích hiện đã ngừng bán."
            skeleton={<div className="customer-card customer-card--skeleton" />}
            renderItem={(product) => <ProductCard key={product.id} product={product} />}
            onPageChange={() => window.scrollTo({ top: 0, behavior: "smooth" })}
          />
        )}
      </div>
    </div>
  );
}
