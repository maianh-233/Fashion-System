import { useState } from "react";
import { MapPin, Minus, Plus, ShieldCheck, ShoppingBag, Star, Truck } from "lucide-react";
import Button from "../../common/Button";
import { useSystemNotification } from "../../common/SystemNotification";
import { useCustomerCart } from "../../../contexts/CustomerCartContext";
import StoreStockDialog from "./StoreStockDialog";
import WishlistButton from "../WishlistButton";
import {
  GENDER_LABELS, colorSwatch, findVariant, formatPrice, initialSelection, selectColor, sizeStates, stockLabel, tagLabel,
} from "../../../pages/customer/storefrontLogic";

/** Thông tin và lựa chọn biến thể (màu → size) của sản phẩm từ API storefront. */
export default function ProductInfo({ product, onColorChange = () => {}, reviewSummary = null }) {
  const [selection, setSelection] = useState(() => initialSelection(product.variants));
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);
  const [showStores, setShowStores] = useState(false);
  const cart = useCustomerCart();
  const notification = useSystemNotification();

  const variant = findVariant(product.variants, selection.color, selection.size);
  const available = variant?.available ?? 0;
  const sizes = sizeStates(product.variants, product.sizes, selection.color);
  const onSale = variant?.salePrice != null && Number(variant.salePrice) < Number(variant.price);
  const discount = onSale ? Math.round((1 - variant.salePrice / variant.price) * 100) : 0;

  const inCart = cart.cart.items.find((line) => line.variantId === variant?.id)?.quantity ?? 0;
  const addToCart = async () => {
    if (!variant || available <= 0) return;
    if (inCart + quantity > available) {
      notification.warning(`Chỉ còn ${available} sản phẩm, giỏ của bạn đã có ${inCart}.`);
      return;
    }
    setAdding(true);
    try {
      await cart.add(variant.id, quantity);
      notification.success(`Đã thêm ${quantity} × ${product.name} (${[variant.color, variant.size].filter(Boolean).join(" / ")}) vào giỏ hàng.`);
      setQuantity(1);
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setAdding(false);
    }
  };

  const chooseColor = (color) => {
    const next = selectColor(product.variants, product.sizes, color, selection.size);
    setSelection(next);
    setQuantity(1);
    onColorChange(next.color);
  };
  const chooseSize = (size) => {
    setSelection((current) => ({ ...current, size }));
    setQuantity(1);
  };

  const specs = [
    ["Mã sản phẩm", product.code],
    product.material && ["Chất liệu", product.material],
    product.fit && ["Phom dáng", product.fit],
    product.gender && ["Dành cho", GENDER_LABELS[product.gender] || product.gender],
    ...product.attributes.map((attribute) => [attribute.name, attribute.value]),
  ].filter(Boolean);

  return (
    <section className="product-detail-info">
      <div className="product-detail-info__brand">
        <span>{product.brand?.name || "Lunaria"}</span>
        {variant && <small>SKU · {variant.sku}</small>}
      </div>

      <h1>{product.name}</h1>
      {product.collection && (
        <p className="product-detail-info__collection">
          {product.collection.name}
        </p>
      )}

      <div className="product-detail-info__rating">
        {reviewSummary?.count > 0 && (
          <a href="#reviews" className="product-detail-info__reviews">
            <span><Star size={14} fill="currentColor" aria-hidden="true" /> {Number(reviewSummary.average).toFixed(1)}</span>
            <small>{reviewSummary.count} đánh giá</small>
          </a>
        )}
        <small className={available > 0 ? "" : "is-out"}>{stockLabel(variant)}</small>
      </div>

      <div className="product-detail-info__price">
        <strong>{formatPrice(variant ? variant.unitPrice : product.minPrice)}</strong>
        {onSale && <del>{formatPrice(variant.price)}</del>}
        {onSale && <span>-{discount}%</span>}
      </div>

      {product.description && <p className="product-detail-info__description">{product.description}</p>}

      <div className="product-detail-options-grid">
        {product.colors.length > 0 && (
          <fieldset className="product-detail-option">
            <legend>Màu sắc <span>· {selection.color}</span></legend>
            <div className="product-detail-colors">
              {product.colors.map((color) => (
                <button
                  key={color}
                  type="button"
                  className={selection.color === color ? "is-active" : ""}
                  onClick={() => chooseColor(color)}
                  aria-label={`Chọn màu ${color}`}
                  aria-pressed={selection.color === color}
                  title={color}
                  style={{ "--swatch": colorSwatch(color) }}
                />
              ))}
            </div>
          </fieldset>
        )}

        {product.sizes.length > 0 && (
          <fieldset className="product-detail-option">
            <legend>Kích thước <button type="button">Hướng dẫn chọn size</button></legend>
            <div className="product-detail-sizes">
              {sizes.map((item) => (
                <button
                  key={item.size}
                  type="button"
                  disabled={!item.exists}
                  className={[selection.size === item.size ? "is-active" : "", item.exists && item.available <= 0 ? "is-sold-out" : ""].join(" ")}
                  onClick={() => chooseSize(item.size)}
                  aria-pressed={selection.size === item.size}
                  aria-label={`Chọn kích thước ${item.size}${!item.exists ? " (không có)" : item.available <= 0 ? " (hết hàng)" : ""}`}
                  title={!item.exists ? "Không có size này cho màu đã chọn" : item.available <= 0 ? "Tạm hết hàng" : undefined}
                >
                  {item.size}
                </button>
              ))}
            </div>
          </fieldset>
        )}
      </div>

      <div className="product-detail-actions">
        <div className="product-detail-quantity">
          <button type="button" onClick={() => setQuantity((value) => Math.max(1, value - 1))} aria-label="Giảm số lượng" disabled={quantity <= 1}><Minus size={15} /></button>
          <span>{quantity}</span>
          <button type="button" onClick={() => setQuantity((value) => Math.min(available, value + 1))} aria-label="Tăng số lượng" disabled={quantity >= available}><Plus size={15} /></button>
        </div>
        <Button variant="unstyled" className="product-detail-add" disabled={available <= 0} loading={adding} onClick={addToCart}>
          <ShoppingBag size={18} /> {available > 0 ? "Thêm vào giỏ hàng" : "Tạm hết hàng"}
        </Button>
        <WishlistButton productId={product.id} productName={product.name} className="product-detail-wishlist" showLabel />
      </div>

      {variant && (
        <button type="button" className="product-detail-store-stock" onClick={() => setShowStores(true)}>
          <MapPin size={15} /> Xem cửa hàng còn hàng
        </button>
      )}
      {showStores && variant && <StoreStockDialog product={product} variant={variant} onClose={() => setShowStores(false)} />}

      <dl className="product-detail-specs">
        {specs.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}
      </dl>

      <div className="product-detail-assurance">
        <span><ShieldCheck size={17} /> Đổi size trong 14 ngày</span>
        <span><Truck size={17} /> Miễn phí giao từ 1.500.000 ₫</span>
      </div>

      {product.tags.length > 0 && (
        <div className="product-detail-tags">
          {product.tags.map((tag) => <span key={tag}>#{tagLabel(tag)}</span>)}
        </div>
      )}
    </section>
  );
}
