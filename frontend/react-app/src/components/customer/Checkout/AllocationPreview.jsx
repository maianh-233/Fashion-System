import { MapPin, Split, Store } from "lucide-react";

/**
 * Cửa hàng sẽ xử lý đơn (backend chọn cửa hàng gần nhất còn đủ hàng). Nhiều phần nghĩa là đơn được tách cho nhiều
 * cửa hàng, mỗi phần giao riêng.
 */
export default function AllocationPreview({ parts = [], split, loading, eyebrow = "Điều phối đơn hàng" }) {
  return (
    <section className="checkout-section checkout-processing-store">
      <div className="checkout-section__heading">
        <span><Store size={17} /></span>
        <div><small>{eyebrow}</small><h2>{split ? "Đơn giao từ nhiều cửa hàng" : "Cửa hàng phụ trách"}</h2></div>
        {parts.length > 0 && <strong>{parts.length}</strong>}
      </div>

      {loading && !parts.length && <p className="checkout-muted">Đang tìm cửa hàng gần bạn...</p>}
      {!loading && !parts.length && <p className="checkout-muted">Chọn địa chỉ giao hàng để xem cửa hàng phụ trách.</p>}
      {split && (
        <p className="checkout-allocation__notice">
          <Split size={14} /> Không cửa hàng nào có đủ toàn bộ giỏ hàng, đơn sẽ được giao thành {parts.length} phần.
        </p>
      )}
      <div className="checkout-allocation">
        {parts.map((part, index) => (
          <article key={`${part.storeId}-${index}`} className="checkout-allocation__part">
            <header>
              <h3>{split ? `Phần ${index + 1} · ` : ""}{part.storeName}</h3>
              {part.distanceKm != null && <small>Cách bạn khoảng {part.distanceKm} km</small>}
            </header>
            {part.storeAddress && <p><MapPin size={12} /> {part.storeAddress}</p>}
            <ul>
              {part.items.map((item) => (
                <li key={item.variantId}>{item.productName} · {[item.color, item.size].filter(Boolean).join(" / ")} × {item.quantity}</li>
              ))}
            </ul>
          </article>
        ))}
      </div>
    </section>
  );
}
