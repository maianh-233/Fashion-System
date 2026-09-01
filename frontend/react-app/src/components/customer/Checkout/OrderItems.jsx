import { PackageCheck } from "lucide-react";

const formatPrice = (value) => `${Number(value).toLocaleString("vi-VN")} ₫`;

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
          <article key={`${item.name}-${index}`}>
            <div className="checkout-items__media">
              <img src={item.image} alt={item.name} loading="lazy" />
              <span>{item.quantity}</span>
            </div>
            <div className="checkout-items__copy">
              <h3>{item.name}</h3>
              <p>Màu · {item.color}<span />Size · {item.size}</p>
              <small>{formatPrice(item.price)} / sản phẩm</small>
            </div>
            <strong>{formatPrice(item.price * item.quantity)}</strong>
          </article>
        ))}
      </div>
    </section>
  );
}
