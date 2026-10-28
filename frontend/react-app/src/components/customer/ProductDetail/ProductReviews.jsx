import { useEffect, useState } from "react";
import { BadgeCheck, Camera, LoaderCircle, MessageSquareReply, Star } from "lucide-react";
import Button from "../../common/Button";
import { storefrontApi } from "../../../api/storefront/storefrontApi";
import { formatDate } from "../../../pages/customer/storefrontLogic";
import { REVIEW_PAGE_SIZE, mergeReviewPage, starPercents } from "../../../pages/customer/reviewLogic";

export function Stars({ value, size = 14 }) {
  return (
    <span className="product-reviews__stars" aria-label={`${value} trên 5 sao`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <Star key={star} size={size} fill={star <= Math.round(value) ? "currentColor" : "none"} aria-hidden="true" />
      ))}
    </span>
  );
}

const FILTERS = [
  { key: "all", label: "Tất cả" },
  ...[5, 4, 3, 2, 1].map((rating) => ({ key: String(rating), label: `${rating} sao`, rating })),
  { key: "images", label: "Có ảnh", withImages: true },
];

/** Đánh giá của khách đã mua trên trang chi tiết sản phẩm: tổng quan sao, bộ lọc và danh sách (xem thêm). */
export default function ProductReviews({ productId, summary }) {
  const [filter, setFilter] = useState("all");
  const [state, setState] = useState({ key: null, items: [], total: 0, page: 0, error: "" });
  const [loadingMore, setLoadingMore] = useState(false);
  const active = FILTERS.find((item) => item.key === filter);
  const requestKey = `${productId}:${filter}`;

  useEffect(() => {
    const controller = new AbortController();
    storefrontApi.reviews(productId, { rating: active.rating, withImages: active.withImages, size: REVIEW_PAGE_SIZE },
      { signal: controller.signal })
      .then((page) => setState(mergeReviewPage({ key: requestKey, items: [] }, page, requestKey)))
      .catch((failure) => {
        if (!controller.signal.aborted) setState({ key: requestKey, items: [], total: 0, page: 0, error: failure.message });
      });
    return () => controller.abort();
  }, [productId, requestKey, active.rating, active.withImages]);

  const loadMore = async () => {
    setLoadingMore(true);
    try {
      const page = await storefrontApi.reviews(productId,
        { rating: active.rating, withImages: active.withImages, page: state.page + 1, size: REVIEW_PAGE_SIZE });
      setState((current) => mergeReviewPage(current, page, requestKey));
    } catch (failure) {
      setState((current) => ({ ...current, error: failure.message }));
    } finally {
      setLoadingMore(false);
    }
  };

  const loading = state.key !== requestKey;
  const percents = starPercents(summary);

  return (
    <section id="reviews" className="product-reviews" aria-labelledby="product-reviews-title">
      <header className="product-reviews__head">
        <h2 id="product-reviews-title">Đánh giá từ khách hàng</h2>
        <p>Chỉ khách đã mua và nhận hàng mới có thể đánh giá.</p>
      </header>

      <div className="product-reviews__layout">
        <aside className="product-reviews__summary">
          <strong>{summary?.count ? Number(summary.average).toFixed(1) : "–"}</strong>
          <Stars value={Number(summary?.average || 0)} size={18} />
          <small>{summary?.count ? `${summary.count} đánh giá` : "Chưa có đánh giá"}</small>
          <ol>
            {[5, 4, 3, 2, 1].map((star, index) => (
              <li key={star}>
                <span>{star}<Star size={11} fill="currentColor" aria-hidden="true" /></span>
                <i><b style={{ width: `${percents[index]}%` }} /></i>
                <small>{summary?.stars?.[index] ?? 0}</small>
              </li>
            ))}
          </ol>
        </aside>

        <div className="product-reviews__body">
          <div className="product-reviews__filters" role="group" aria-label="Lọc đánh giá">
            {FILTERS.map((item) => (
              <button key={item.key} type="button" className={item.key === filter ? "is-active" : ""}
                aria-pressed={item.key === filter} onClick={() => setFilter(item.key)}>
                {item.withImages && <Camera size={13} aria-hidden="true" />} {item.label}
              </button>
            ))}
          </div>

          {loading && (
            <p className="product-reviews__status"><LoaderCircle className="animate-spin" size={16} /> Đang tải đánh giá...</p>
          )}
          {!loading && state.error && <p className="product-reviews__status" role="alert">{state.error}</p>}
          {!loading && !state.error && state.items.length === 0 && (
            <p className="product-reviews__status">
              {filter === "all" ? "Sản phẩm chưa có đánh giá nào." : "Không có đánh giá phù hợp bộ lọc."}
            </p>
          )}

          <ul className="product-reviews__list">
            {state.items.map((review) => (
              <li key={review.id} className="product-review">
                <div className="product-review__meta">
                  <strong>{review.reviewerName}</strong>
                  {review.verifiedPurchase && (
                    <span className="product-review__verified"><BadgeCheck size={13} aria-hidden="true" /> Đã mua hàng</span>
                  )}
                  <small>{formatDate(review.createdAt)}</small>
                </div>
                <div className="product-review__rating">
                  <Stars value={review.rating} />
                  {review.variantLabel && <small>Phân loại: {review.variantLabel}</small>}
                </div>
                {review.content && <p className="product-review__content">{review.content}</p>}
                {review.images.length > 0 && (
                  <div className="product-review__images">
                    {review.images.map((url, index) => (
                      <a key={url} href={url} target="_blank" rel="noreferrer" aria-label={`Ảnh ${index + 1} của đánh giá`}>
                        <img src={url} alt="" loading="lazy" />
                      </a>
                    ))}
                  </div>
                )}
                {review.reply && (
                  <div className="product-review__reply">
                    <strong><MessageSquareReply size={14} aria-hidden="true" /> Phản hồi từ Lunaria</strong>
                    <p>{review.reply}</p>
                  </div>
                )}
              </li>
            ))}
          </ul>

          {!loading && state.items.length < state.total && (
            <Button type="button" variant="ghost" className="product-reviews__more" loading={loadingMore} onClick={loadMore}>
              Xem thêm đánh giá ({state.total - state.items.length})
            </Button>
          )}
        </div>
      </div>
    </section>
  );
}
