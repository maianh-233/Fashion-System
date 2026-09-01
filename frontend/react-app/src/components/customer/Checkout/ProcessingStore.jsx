import { Clock3, MapPin, Navigation, Phone, Store } from "lucide-react";

export default function ProcessingStore({
  stores = [],
  selectedStore,
  onSelect,
  readOnly = false,
  pickup = false,
}) {
  if (!selectedStore) return null;

  const directionsUrl = `https://www.google.com/maps/dir/?api=1&destination=${selectedStore.coordinates.join(",")}`;

  return (
    <section className={`checkout-section checkout-processing-store ${readOnly ? "order-detail-card" : ""}`}>
      <div className="checkout-section__heading">
        <span><Store size={17} /></span>
        <div>
          <small>{readOnly ? "Order fulfillment" : "Điều phối đơn hàng"}</small>
          <h2>{pickup ? "Cửa hàng nhận & xử lý" : "Cửa hàng phụ trách"}</h2>
        </div>
        {!readOnly && <strong>{stores.length}</strong>}
      </div>

      {!readOnly && (
        <label className="checkout-processing-store__select">
          <span>Chọn chi nhánh</span>
          <select
            value={selectedStore.id}
            onChange={(event) => onSelect?.(stores.find((store) => store.id === event.target.value))}
          >
            {stores.map((store) => (
              <option key={store.id} value={store.id}>{store.name} · {store.area}</option>
            ))}
          </select>
          <small>
            {pickup
              ? "Đơn hàng sẽ được chuẩn bị và nhận tại chi nhánh này."
              : "Chi nhánh này sẽ xác nhận, đóng gói và bàn giao đơn vị vận chuyển."}
          </small>
        </label>
      )}

      <article className="checkout-processing-store__card">
        <img src={selectedStore.image} alt={`Không gian ${selectedStore.name}`} />
        <div className="checkout-processing-store__info">
          <small><MapPin size={12} /> {selectedStore.area}</small>
          <h3>{selectedStore.name}</h3>
          <p>{selectedStore.address}</p>
          <div>
            <span><Clock3 size={13} /> {selectedStore.hours}</span>
            <a href={`tel:${selectedStore.phone.replace(/\s/g, "")}`}><Phone size={13} /> {selectedStore.phone}</a>
          </div>
        </div>
        <a className="checkout-processing-store__directions" href={directionsUrl} target="_blank" rel="noreferrer">
          <Navigation size={15} /><span>Chỉ đường</span>
        </a>
      </article>
    </section>
  );
}
