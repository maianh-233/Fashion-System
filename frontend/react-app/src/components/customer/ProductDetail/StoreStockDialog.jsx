import { useEffect, useMemo, useState } from "react";
import { LoaderCircle, LocateFixed, MapPin, Navigation, Phone, X } from "lucide-react";
import { storefrontApi } from "../../../api/storefrontApi";
import { STOCK_LABELS, directionsUrl, sortByDistance, telHref } from "../../../pages/customer/storeLogic";

const coordinatesOf = (item) => [Number(item.store.latitude), Number(item.store.longitude)];

/**
 * Cửa hàng còn hàng tại quầy của biến thể đang chọn, để khách ghé mua trực tiếp. Chỉ báo "còn hàng" hoặc "sắp hết",
 * không hiện số lượng; có thể sắp xếp theo khoảng cách khi khách cho phép định vị.
 */
export default function StoreStockDialog({ product, variant, onClose }) {
  const [result, setResult] = useState({ variantId: null, items: [], error: "" });
  const [location, setLocation] = useState(null);
  const [locating, setLocating] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    storefrontApi.variantStores(variant.id, { signal: controller.signal })
      .then((items) => setResult({ variantId: variant.id, items: items || [], error: "" }))
      .catch((failure) => {
        if (!controller.signal.aborted) setResult({ variantId: variant.id, items: [], error: failure.message });
      });
    return () => controller.abort();
  }, [variant.id]);

  useEffect(() => {
    const onKey = (event) => { if (event.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const loading = result.variantId !== variant.id;
  const items = useMemo(() => sortByDistance(result.items, location, coordinatesOf), [result.items, location]);

  const locate = () => {
    if (!navigator.geolocation) {
      setLocating("Trình duyệt không hỗ trợ định vị.");
      return;
    }
    setLocating("Đang xác định vị trí...");
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setLocation([coords.latitude, coords.longitude]);
        setLocating("");
      },
      () => setLocating("Không thể truy cập vị trí. Hãy kiểm tra quyền định vị."),
      { enableHighAccuracy: true, timeout: 10000 },
    );
  };

  const variantLabel = [variant.color, variant.size].filter(Boolean).join(" / ");

  return (
    <div className="customer-dialog" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="customer-dialog__panel store-stock" role="dialog" aria-modal="true" aria-labelledby="store-stock-title">
        <header>
          <div>
            <small>{product.name}{variantLabel ? ` · ${variantLabel}` : ""}</small>
            <h2 id="store-stock-title">Cửa hàng còn hàng</h2>
          </div>
          <button type="button" onClick={onClose} aria-label="Đóng"><X size={17} /></button>
        </header>
        <p className="customer-dialog__hint">
          Tình trạng tại quầy có thể thay đổi trong ngày. Bạn nên gọi trước cho cửa hàng để được giữ size.
        </p>

        {!loading && !result.error && items.length > 1 && (
          <button type="button" className="store-stock__locate" onClick={locate} disabled={Boolean(location)}>
            <LocateFixed size={14} /> {location ? "Đã sắp xếp theo khoảng cách" : "Sắp xếp gần tôi"}
          </button>
        )}
        {locating && <small className="store-stock__status">{locating}</small>}

        {loading ? (
          <p className="store-stock__empty"><LoaderCircle className="animate-spin" size={16} /> Đang kiểm tra cửa hàng...</p>
        ) : result.error ? (
          <p className="checkout-error">{result.error}</p>
        ) : items.length === 0 ? (
          <p className="store-stock__empty"><MapPin size={16} /> Hiện không cửa hàng nào còn {variantLabel || "sản phẩm này"} tại quầy.</p>
        ) : (
          <ul className="store-stock__list">
            {items.map(({ store, stock, distance }) => (
              <li key={store.id}>
                <div className="store-stock__head">
                  <strong>{store.name}</strong>
                  <span className={`store-stock__badge is-${stock === "LOW_STOCK" ? "low" : "in"}`}>{STOCK_LABELS[stock] || stock}</span>
                </div>
                <p>{store.address}</p>
                <div className="store-stock__meta">
                  {distance != null && <span>{distance.toFixed(1)} km</span>}
                  {store.openingHours && <span>{store.openingHours}</span>}
                  {store.phone && <a href={telHref(store.phone)}><Phone size={12} /> {store.phone}</a>}
                  <a href={directionsUrl(store.latitude, store.longitude)} target="_blank" rel="noreferrer">
                    Chỉ đường <Navigation size={12} />
                  </a>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
