import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Zap } from "lucide-react";
import { storefrontApi } from "../../api/storefront/storefrontApi";
import { formatPrice } from "../../pages/customer/storefrontLogic";
import { FLASH_PER_CUSTOMER, countdown, percentOff, soldPercent } from "../../pages/customer/flashSaleLogic";

/** Đồng hồ đếm ngược tới {@code endsAt}, cập nhật mỗi giây; gọi {@code onEnd} một lần khi hết giờ. */
export function FlashCountdown({ endsAt, onEnd, className = "" }) {
  const [now, setNow] = useState(() => Date.now());
  const left = countdown(endsAt, now);
  useEffect(() => {
    if (left.done) {
      onEnd?.();
      return undefined;
    }
    const timer = window.setTimeout(() => setNow(Date.now()), 1000);
    return () => window.clearTimeout(timer);
  }, [now, left.done, onEnd]);
  return (
    <span className={`flash-countdown ${className}`} aria-label={`Còn ${left.hours} giờ ${left.minutes} phút ${left.seconds} giây`}>
      {left.text.split(":").map((part, index) => <b key={index}>{part}</b>)}
    </span>
  );
}

/** Flash sale đang chạy trên trang chủ (chương trình kết thúc sớm nhất); ẩn khi không có. */
export default function FlashSaleSection() {
  const [sale, setSale] = useState(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    storefrontApi.flashSales({ signal: controller.signal })
      .then((sales) => setSale(sales.find((s) => s.items.length > 0) || null))
      .catch(() => setSale(null));
    return () => controller.abort();
  }, [version]);

  if (!sale) return null;
  return (
    <section className="home-section flash-sale" aria-labelledby="flash-sale-title">
      <div className="home-section__heading">
        <div>
          <p><Zap size={14} aria-hidden="true" /> Flash sale · tối đa {FLASH_PER_CUSTOMER} món mỗi phân loại</p>
          <h2 id="flash-sale-title">{sale.name}</h2>
        </div>
        <div className="flash-sale__timer">
          <span>Kết thúc sau</span>
          <FlashCountdown endsAt={sale.endsAt} onEnd={() => setVersion((v) => v + 1)} />
        </div>
      </div>
      <div className="flash-sale__grid">
        {sale.items.map((item) => (
          <Link key={item.variantId} to={`/productdetail?id=${item.productId}`}
            className={`flash-sale__item ${item.remaining === 0 ? "is-sold-out" : ""}`}>
            <div className="flash-sale__media">
              {item.imageUrl ? <img src={item.imageUrl} alt="" loading="lazy" /> : <span>{item.productName.slice(0, 2).toUpperCase()}</span>}
              <em>-{percentOff(item.regularPrice, item.salePrice)}%</em>
            </div>
            <strong>{item.productName}</strong>
            <small>{[item.color, item.size].filter(Boolean).join(" / ")}</small>
            <span className="flash-sale__price">
              {formatPrice(item.salePrice)} <del>{formatPrice(item.regularPrice)}</del>
            </span>
            <span className="flash-sale__progress" aria-label={`Còn ${item.remaining} / ${item.quota} suất`}>
              <i style={{ width: `${soldPercent(item)}%` }} />
              <small>{item.remaining === 0 ? "Đã hết suất" : `Còn ${item.remaining} suất`}</small>
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}
