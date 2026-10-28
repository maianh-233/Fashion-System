import { useEffect, useState } from "react";
import { Link, Navigate, useSearchParams } from "react-router-dom";
import { EyeOff, LoaderCircle, MessageSquareReply, PenLine, Star } from "lucide-react";
import Button from "../../components/common/Button";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { Stars } from "../../components/customer/ProductDetail/ProductReviews";
import ReviewFormDialog from "../../components/customer/Review/ReviewFormDialog";
import { reviewApi } from "../../api/customer/accountApi";
import { useCustomerSession } from "../../hooks/useCustomerSession";
import { formatDate } from "./storefrontLogic";

const TABS = [
  { key: "pending", label: "Chờ đánh giá", icon: PenLine },
  { key: "mine", label: "Đã đánh giá", icon: Star },
];

/**
 * Đánh giá của tôi: sản phẩm trong đơn đã hoàn thành chưa đánh giá và các đánh giá đã gửi.
 * {@code ?order=<id>} (từ trang chi tiết đơn) đưa sản phẩm của đơn đó lên đầu.
 */
export default function MyReviewsPage() {
  const { loggedIn } = useCustomerSession();
  const notification = useSystemNotification();
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = searchParams.get("tab") === "mine" ? "mine" : "pending";
  const focusOrder = searchParams.get("order");
  const [data, setData] = useState({ loaded: false, pending: [], mine: [], error: "" });
  const [version, setVersion] = useState(0);
  const [writing, setWriting] = useState(null);

  useEffect(() => {
    if (!loggedIn) return undefined;
    const controller = new AbortController();
    Promise.all([reviewApi.pending({ signal: controller.signal }), reviewApi.mine({ signal: controller.signal })])
      .then(([pending, mine]) => setData({ loaded: true, pending, mine, error: "" }))
      .catch((failure) => {
        if (!controller.signal.aborted) setData((current) => ({ ...current, loaded: true, error: failure.message }));
      });
    return () => controller.abort();
  }, [loggedIn, version]);

  if (!loggedIn) return <Navigate to={`/customerlogin?redirect=${encodeURIComponent("/reviews")}`} replace />;

  const pending = focusOrder
    ? [...data.pending.filter((item) => item.orderId === focusOrder), ...data.pending.filter((item) => item.orderId !== focusOrder)]
    : data.pending;

  const created = () => {
    setWriting(null);
    notification.success("Cảm ơn bạn! Đánh giá đã được đăng trên trang sản phẩm.");
    setVersion((value) => value + 1);
  };

  return (
    <div className="customer-page min-h-screen w-full text-zinc-200">
      <div className="customer-page__wide px-4 py-5 sm:px-6 sm:py-8">
        <div className="mb-6 sm:mb-8">
          <h1 className="customer-page-title">Đánh giá của tôi</h1>
          <p className="mt-1 text-sm text-zinc-400">
            Đánh giá sản phẩm trong các đơn đã hoàn thành để giúp khách hàng khác chọn đúng size và chất liệu.
          </p>
        </div>

        <div className="voucher-wallet__tabs" role="tablist" aria-label="Đánh giá của tôi">
          {TABS.map(({ key, label, icon: Icon }) => (
            <button key={key} type="button" role="tab" aria-selected={tab === key}
              className={`voucher-wallet__tab ${tab === key ? "is-active" : ""}`}
              onClick={() => setSearchParams(key === "pending" ? {} : { tab: key }, { replace: true })}>
              <Icon size={15} /> {label}
              {data.loaded && <span className="my-reviews__count">{key === "pending" ? data.pending.length : data.mine.length}</span>}
            </button>
          ))}
        </div>

        {!data.loaded && (
          <p className="my-reviews__status"><LoaderCircle className="animate-spin" size={16} /> Đang tải...</p>
        )}
        {data.loaded && data.error && <p className="my-reviews__status" role="alert">{data.error}</p>}

        {data.loaded && !data.error && tab === "pending" && (
          pending.length === 0 ? (
            <p className="my-reviews__status">Không có sản phẩm nào chờ đánh giá. Sản phẩm xuất hiện ở đây khi đơn hàng hoàn thành.</p>
          ) : (
            <ul className="my-reviews__list">
              {pending.map((item) => (
                <li key={`${item.orderId}:${item.productId}`} className={`my-review-card ${item.orderId === focusOrder ? "is-focus" : ""}`}>
                  {item.imageUrl ? <img src={item.imageUrl} alt="" /> : <span className="my-review-card__placeholder" />}
                  <div>
                    <Link to={`/productdetail?id=${item.productId}`}><strong>{item.productName}</strong></Link>
                    {item.variantLabel && <small>Phân loại: {item.variantLabel}</small>}
                    <small>Đơn #{item.orderCode} · Hoàn thành {formatDate(item.completedAt)}</small>
                  </div>
                  <Button type="button" variant="unstyled" className="checkout-primary-button my-review-card__write" onClick={() => setWriting(item)}>
                    <PenLine size={14} /> Viết đánh giá
                  </Button>
                </li>
              ))}
            </ul>
          )
        )}

        {data.loaded && !data.error && tab === "mine" && (
          data.mine.length === 0 ? (
            <p className="my-reviews__status">Bạn chưa gửi đánh giá nào.</p>
          ) : (
            <ul className="my-reviews__list">
              {data.mine.map((review) => (
                <li key={review.id} className="my-review-card my-review-card--done">
                  {review.productImage ? <img src={review.productImage} alt="" /> : <span className="my-review-card__placeholder" />}
                  <div>
                    <Link to={`/productdetail?id=${review.productId}`}><strong>{review.productName}</strong></Link>
                    <small>Đơn #{review.orderCode} · {formatDate(review.createdAt)}{review.variantLabel ? ` · ${review.variantLabel}` : ""}</small>
                    <Stars value={review.rating} />
                    {review.content && <p>{review.content}</p>}
                    {review.images.length > 0 && (
                      <div className="product-review__images">
                        {review.images.map((url) => (
                          <a key={url} href={url} target="_blank" rel="noreferrer"><img src={url} alt="" loading="lazy" /></a>
                        ))}
                      </div>
                    )}
                    {review.hidden && (
                      <p className="my-review-card__hidden"><EyeOff size={13} /> Đánh giá đang bị ẩn khỏi trang sản phẩm.</p>
                    )}
                    {review.reply && (
                      <div className="product-review__reply">
                        <strong><MessageSquareReply size={14} aria-hidden="true" /> Phản hồi từ Lunaria</strong>
                        <p>{review.reply}</p>
                      </div>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )
        )}
      </div>
      {writing && <ReviewFormDialog item={writing} onClose={() => setWriting(null)} onCreated={created} />}
    </div>
  );
}
