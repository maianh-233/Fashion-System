import { useEffect, useMemo, useState } from "react";
import { ImagePlus, Star, X } from "lucide-react";
import Button from "../../common/Button";
import { reviewApi } from "../../../api/customer/accountApi";
import {
  RATING_LABELS, REVIEW_MAX_CONTENT, REVIEW_MAX_IMAGES, addReviewImages, validateReviewForm,
} from "../../../pages/customer/reviewLogic";

/** Khách đánh giá một sản phẩm trong đơn đã hoàn thành: số sao, nhận xét và tối đa 3 ảnh. */
export default function ReviewFormDialog({ item, onClose, onCreated }) {
  const [rating, setRating] = useState(0);
  const [hover, setHover] = useState(0);
  const [content, setContent] = useState("");
  const [images, setImages] = useState([]);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const previews = useMemo(() => images.map((file) => URL.createObjectURL(file)), [images]);

  useEffect(() => () => previews.forEach((url) => URL.revokeObjectURL(url)), [previews]);
  useEffect(() => {
    const onKey = (event) => { if (event.key === "Escape" && !saving) onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose, saving]);

  const pickImages = (event) => {
    setImages((current) => addReviewImages(current, event.target.files));
    event.target.value = "";
  };

  const submit = async (event) => {
    event.preventDefault();
    const found = validateReviewForm({ rating, content, images });
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setSaving(true);
    try {
      const created = await reviewApi.create({ orderId: item.orderId, productId: item.productId, rating, content, images });
      onCreated(created);
    } catch (failure) {
      setErrors({ submit: failure.message });
    } finally {
      setSaving(false);
    }
  };

  const shown = hover || rating;
  return (
    <div className="customer-dialog" role="presentation"
      onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) onClose(); }}>
      <form className="customer-dialog__panel review-form" role="dialog" aria-modal="true" aria-labelledby="review-form-title"
        onSubmit={submit}>
        <header>
          <div>
            <small>Đơn #{item.orderCode}</small>
            <h2 id="review-form-title">Đánh giá sản phẩm</h2>
          </div>
          <button type="button" onClick={onClose} aria-label="Đóng" disabled={saving}><X size={17} /></button>
        </header>

        <div className="review-form__product">
          {item.imageUrl && <img src={item.imageUrl} alt="" />}
          <span>
            <strong>{item.productName}</strong>
            {item.variantLabel && <small>Phân loại: {item.variantLabel}</small>}
          </span>
        </div>

        <fieldset className="review-form__stars" onMouseLeave={() => setHover(0)}>
          <legend>Chất lượng sản phẩm</legend>
          <div>
            {[1, 2, 3, 4, 5].map((star) => (
              <button key={star} type="button" aria-label={`${star} sao`} aria-pressed={rating === star}
                className={star <= shown ? "is-on" : ""} onMouseEnter={() => setHover(star)}
                onClick={() => { setRating(star); setErrors((current) => ({ ...current, rating: undefined })); }}>
                <Star size={26} fill={star <= shown ? "currentColor" : "none"} />
              </button>
            ))}
            <span>{shown ? RATING_LABELS[shown] : "Chọn số sao"}</span>
          </div>
          {errors.rating && <p className="checkout-error">{errors.rating}</p>}
        </fieldset>

        <label className="customer-dialog__note">
          <span>Nhận xét (không bắt buộc)</span>
          <textarea value={content} rows={4} maxLength={REVIEW_MAX_CONTENT}
            onChange={(event) => setContent(event.target.value)}
            placeholder="Chất liệu, form dáng, màu sắc có đúng mô tả không?" />
          <small className="review-form__count">{content.length}/{REVIEW_MAX_CONTENT}</small>
        </label>
        {errors.content && <p className="checkout-error">{errors.content}</p>}

        <div className="review-form__images">
          <span>Ảnh thực tế ({images.length}/{REVIEW_MAX_IMAGES})</span>
          <div>
            {previews.map((url, index) => (
              <figure key={url}>
                <img src={url} alt={`Ảnh ${index + 1}`} />
                <button type="button" aria-label={`Bỏ ảnh ${index + 1}`}
                  onClick={() => setImages((current) => current.filter((_, i) => i !== index))}><X size={12} /></button>
              </figure>
            ))}
            {images.length < REVIEW_MAX_IMAGES && (
              <label className="review-form__add">
                <ImagePlus size={20} aria-hidden="true" />
                <small>Thêm ảnh</small>
                <input type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={pickImages} />
              </label>
            )}
          </div>
          {errors.images && <p className="checkout-error">{errors.images}</p>}
        </div>

        {errors.submit && <p className="checkout-error" role="alert">{errors.submit}</p>}
        <footer>
          <Button type="button" variant="unstyled" className="checkout-secondary-button" onClick={onClose} disabled={saving}>Để sau</Button>
          <Button type="submit" variant="unstyled" className="checkout-primary-button" loading={saving}>Gửi đánh giá</Button>
        </footer>
      </form>
    </div>
  );
}
