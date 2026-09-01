import { useState } from "react";
import { Heart, Minus, Plus, ShieldCheck, ShoppingBag, Star, Truck } from "lucide-react";
import Button from "../../common/Button";

const formatPrice = (value) => `${Number(value).toLocaleString("vi-VN")} ₫`;

export default function ProductInfo({ product }) {
  const [color, setColor] = useState(product.colors[0].id);
  const [size, setSize] = useState("M");
  const [quantity, setQuantity] = useState(1);

  const discount = Math.round((1 - product.price / product.originalPrice) * 100);

  return (
    <section className="product-detail-info">
      <div className="product-detail-info__brand">
        <span>{product.brand}</span>
        <small>SKU · {product.sku}</small>
      </div>

      <h1>{product.name}</h1>
      <p className="product-detail-info__collection">{product.collection}</p>

      <div className="product-detail-info__rating">
        <span><Star size={14} fill="currentColor" /> {product.rating}</span>
        <small>{product.reviewCount} đánh giá</small>
        <small>{product.stock} sản phẩm có sẵn</small>
      </div>

      <div className="product-detail-info__price">
        <strong>{formatPrice(product.price)}</strong>
        <del>{formatPrice(product.originalPrice)}</del>
        <span>-{discount}%</span>
      </div>

      <p className="product-detail-info__description">{product.description}</p>

      <div className="product-detail-options-grid">
        <fieldset className="product-detail-option">
          <legend>Màu sắc <span>· {product.colors.find((item) => item.id === color)?.label}</span></legend>
          <div className="product-detail-colors">
            {product.colors.map((item) => (
              <button
                key={item.id}
                type="button"
                className={color === item.id ? "is-active" : ""}
                onClick={() => setColor(item.id)}
                aria-label={`Chọn màu ${item.label}`}
                title={item.label}
                style={{ "--swatch": item.hex }}
              />
            ))}
          </div>
        </fieldset>

        <fieldset className="product-detail-option">
          <legend>Kích thước <button type="button">Hướng dẫn chọn size</button></legend>
          <div className="product-detail-sizes">
            {product.sizes.map((item) => (
              <button key={item} type="button" className={size === item ? "is-active" : ""} onClick={() => setSize(item)} aria-label={`Chọn kích thước ${item}`}>{item}</button>
            ))}
          </div>
        </fieldset>
      </div>

      <div className="product-detail-actions">
        <div className="product-detail-quantity">
          <button type="button" onClick={() => setQuantity((value) => Math.max(1, value - 1))} aria-label="Giảm số lượng"><Minus size={15} /></button>
          <span>{quantity}</span>
          <button type="button" onClick={() => setQuantity((value) => value + 1)} aria-label="Tăng số lượng"><Plus size={15} /></button>
        </div>
        <Button variant="unstyled" className="product-detail-add"><ShoppingBag size={18} /> Thêm vào giỏ hàng</Button>
        <Button variant="unstyled" className="product-detail-wishlist" aria-label="Thêm vào yêu thích"><Heart size={17} /><span>Yêu thích</span></Button>
      </div>

      <dl className="product-detail-specs">
        {product.details.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}
      </dl>

      <div className="product-detail-assurance">
        <span><ShieldCheck size={17} /> Đổi size trong 14 ngày</span>
        <span><Truck size={17} /> Miễn phí giao từ 1.500.000 ₫</span>
      </div>

      <div className="product-detail-tags">
        {product.tags.map((tag) => <span key={tag}>#{tag}</span>)}
      </div>
    </section>
  );
}
