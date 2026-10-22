import { PackageCheck } from "lucide-react";
import { formatPrice, productImage } from "../../../pages/customer/storefrontLogic";

/** Danh sách sản phẩm: {name, image, color, size, price (đơn giá sau giảm), quantity, total?}. */
export default function OrderItems({ items, eyebrow = "Bước 01", title = "Sản phẩm đặt mua" }) {
  return (
    <section className="checkout-section checkout-items">
      <div className="checkout-section__heading">
        <span><PackageCheck size={17} /></span>
        <div><small>{eyebrow}</small><h2>{title}</h2></div>
        <strong>{items.length}</strong>
      </div>

      <div className="checkout-items__list">
        {items.map((item, index) => (
          <article key={item.key || `${item.name}-${index}`}>
            <div className="checkout-items__media">
              <img src={productImage(item.image, item.name)} alt={item.name} loading="lazy" />
              <span>{item.quantity}</span>
            </div>
            <div className="checkout-items__copy">
              <h3>{item.name}</h3>
              <p>{[item.color && `Màu · ${item.color}`, item.size && `Size · ${item.size}`].filter(Boolean).join("  ·  ")}</p>
              <small>{formatPrice(item.price)} / sản phẩm{item.note ? ` · ${item.note}` : ""}</small>
            </div>
            <strong>{formatPrice(item.total ?? item.price * item.quantity)}</strong>
          </article>
        ))}
      </div>
    </section>
  );
}
