import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowRight,
  Clock3,
  LoaderCircle,
  LocateFixed,
  MapPin,
  Navigation,
  Phone,
  RefreshCw,
  Search,
  Store,
} from "lucide-react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { storefrontApi } from "../../api/storefront/storefrontApi";
import {
  directionsUrl, escapeHtml, filterStores, safeImageUrl, sortByDistance, telHref, toMapStores,
} from "./storeLogic";

const markerIcon = L.divIcon({
  className: "store-map-marker-wrap",
  html: '<span class="store-map-marker"><span></span></span>',
  iconSize: [34, 42],
  iconAnchor: [17, 39],
  popupAnchor: [0, -36],
});

function storePopup(store) {
  const facts = [store.openingHours, store.phone].filter(Boolean).map(escapeHtml).join(" · ");
  return `
    <article class="store-map-popup">
      <img src="${escapeHtml(safeImageUrl(store.imageUrl))}" alt="Không gian ${escapeHtml(store.name)}" />
      <div><small>${escapeHtml(store.area)}</small><h3>${escapeHtml(store.name)}</h3>
      <p>${escapeHtml(store.address)}</p>${facts ? `<span>${facts}</span>` : ""}</div>
    </article>`;
}

function StoreMap({ stores, selectedId, onSelect, userLocation }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const layerRef = useRef(null);
  const markerRefs = useRef(new Map());
  const userMarkerRef = useRef(null);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return undefined;

    const map = L.map(containerRef.current, {
      center: [10.7868, 106.6841],
      zoom: 12,
      zoomControl: false,
      scrollWheelZoom: false,
    });
    L.control.zoom({ position: "bottomright" }).addTo(map);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19,
    }).addTo(map);
    layerRef.current = L.layerGroup().addTo(map);
    mapRef.current = map;

    const resize = window.setTimeout(() => map.invalidateSize(), 50);
    return () => {
      window.clearTimeout(resize);
      map.remove();
      mapRef.current = null;
    };
  }, []);

  useEffect(() => {
    const map = mapRef.current;
    const layer = layerRef.current;
    if (!map || !layer) return;

    layer.clearLayers();
    markerRefs.current.clear();
    stores.forEach((store) => {
      const marker = L.marker(store.coordinates, { icon: markerIcon })
        .bindPopup(storePopup(store), { className: "store-map-leaflet-popup", maxWidth: 290, closeButton: false, offset: [0, -2] })
        .on("mouseover", function handleMouseOver() {
          this.openPopup();
          onSelect(store.id);
        })
        .on("click", () => onSelect(store.id))
        .addTo(layer);
      markerRefs.current.set(store.id, marker);
    });

    if (stores.length === 1) map.flyTo(stores[0].coordinates, 15, { duration: 0.6 });
    else if (stores.length > 1) {
      const bounds = L.latLngBounds(stores.map((store) => store.coordinates));
      map.fitBounds(bounds, { padding: [45, 45], maxZoom: 13 });
    }
  }, [stores, onSelect]);

  useEffect(() => {
    if (!selectedId) return;
    const marker = markerRefs.current.get(selectedId);
    if (marker) {
      marker.openPopup();
      mapRef.current?.panTo(marker.getLatLng(), { animate: true, duration: 0.45 });
    }
  }, [selectedId, stores]);

  useEffect(() => {
    if (!userLocation || !mapRef.current) return;
    const userIcon = L.divIcon({
      className: "store-user-marker-wrap",
      html: '<span class="store-user-marker"><span></span></span>',
      iconSize: [24, 24],
      iconAnchor: [12, 12],
    });
    userMarkerRef.current?.remove();
    userMarkerRef.current = L.marker(userLocation, { icon: userIcon, interactive: false }).addTo(mapRef.current);
    mapRef.current.flyTo(userLocation, 13, { duration: 0.8 });
  }, [userLocation]);

  return <div ref={containerRef} className="store-map" aria-label="Bản đồ vị trí các cửa hàng Lunaria" />;
}

/** Hệ thống cửa hàng (API storefront): bản đồ, tìm kiếm không dấu, gần tôi, chỉ đường. */
export default function StorePage() {
  const [data, setData] = useState({ stores: [], loaded: false, error: "" });
  const [retry, setRetry] = useState(0);
  const [search, setSearch] = useState("");
  const [selectedId, setSelectedId] = useState(null);
  const [userLocation, setUserLocation] = useState(null);
  const [locationStatus, setLocationStatus] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    storefrontApi.stores({ signal: controller.signal })
      .then((stores) => setData({ stores: toMapStores(stores || []), loaded: true, error: "" }))
      .catch((failure) => {
        if (!controller.signal.aborted) setData({ stores: [], loaded: true, error: failure.message });
      });
    return () => controller.abort();
  }, [retry]);

  const filteredStores = useMemo(() => filterStores(data.stores, search), [data.stores, search]);
  const storesWithDistance = useMemo(() => sortByDistance(filteredStores, userLocation), [filteredStores, userLocation]);

  const selectStore = useCallback((id) => setSelectedId(id), []);

  const locateCustomer = () => {
    if (!navigator.geolocation) {
      setLocationStatus("Trình duyệt không hỗ trợ định vị.");
      return;
    }
    setLocationStatus("Đang xác định vị trí...");
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        const position = [coords.latitude, coords.longitude];
        setUserLocation(position);
        const nearest = sortByDistance(data.stores, position)[0];
        if (!nearest) {
          setLocationStatus("Chưa có cửa hàng để so sánh khoảng cách.");
          return;
        }
        setSelectedId(nearest.id);
        setLocationStatus(`Gần bạn nhất: ${nearest.name}`);
      },
      () => setLocationStatus("Không thể truy cập vị trí. Hãy kiểm tra quyền định vị."),
      { enableHighAccuracy: true, timeout: 10000 },
    );
  };

  return (
    <div className="customer-page store-page">
      <section className="store-hero">
        <div className="store-hero__glow" />
        <div className="store-hero__inner">
          <p><Store size={14} /> Hệ thống cửa hàng Lunaria</p>
          <h1>Chạm gần hơn vào<br /><em>phong cách của bạn.</em></h1>
          <span>Khám phá không gian Lunaria gần nhất và trải nghiệm dịch vụ tư vấn phong cách riêng.</span>

          <div className="store-search">
            <Search size={20} />
            <input
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setSelectedId(null);
              }}
              placeholder="Tìm theo tên cửa hàng, quận hoặc địa chỉ..."
              aria-label="Tìm kiếm cửa hàng"
            />
            <button type="button" onClick={locateCustomer}><LocateFixed size={17} /> <span>Gần tôi</span></button>
          </div>
          {locationStatus && <small className="store-location-status">{locationStatus}</small>}
        </div>
      </section>

      <section className="store-map-section" aria-labelledby="store-map-heading">
        <div className="store-section-heading">
          <div><p>Khám phá trên bản đồ</p><h2 id="store-map-heading">Tìm Lunaria gần bạn</h2></div>
          <span><MapPin size={15} /> {data.loaded ? `${filteredStores.length} cửa hàng` : "Đang tải..."}</span>
        </div>
        <div className="store-map-shell">
          <StoreMap
            stores={filteredStores}
            selectedId={selectedId}
            onSelect={selectStore}
            userLocation={userLocation}
          />
          <div className="store-map-hint"><span><MapPin size={14} /></span> Di chuột qua điểm đánh dấu để xem nhanh thông tin</div>
        </div>
      </section>

      <section className="store-list-section" aria-labelledby="store-list-heading">
        <div className="store-section-heading">
          <div><p>Ghé thăm chúng tôi</p><h2 id="store-list-heading">Các cửa hàng trong hệ thống</h2></div>
        </div>

        {!data.loaded ? (
          <div className="store-empty" aria-live="polite">
            <LoaderCircle className="animate-spin" size={26} />
            <p>Đang tải danh sách cửa hàng...</p>
          </div>
        ) : data.error ? (
          <div className="store-empty" role="alert">
            <MapPin size={29} />
            <h3>Không tải được danh sách cửa hàng</h3>
            <p>{data.error}</p>
            <button type="button" onClick={() => { setData((current) => ({ ...current, loaded: false })); setRetry((value) => value + 1); }}>
              Thử lại <RefreshCw size={15} />
            </button>
          </div>
        ) : storesWithDistance.length > 0 ? (
          <div className="store-grid">
            {storesWithDistance.map((store, index) => (
              <article
                key={store.id}
                className={`store-card ${selectedId === store.id ? "is-selected" : ""}`}
                onMouseEnter={() => selectStore(store.id)}
                onFocus={() => selectStore(store.id)}
                tabIndex="0"
              >
                <div className="store-card__image">
                  <img src={safeImageUrl(store.imageUrl)} alt={`Không gian ${store.name}`} loading="lazy" />
                  <span>{userLocation && index === 0 ? "Gần bạn nhất" : store.area}</span>
                </div>
                <div className="store-card__body">
                  <div className="store-card__title"><div><small>{store.area}</small><h3>{store.name}</h3></div><MapPin size={19} /></div>
                  <p>{store.address}</p>
                  <div className="store-card__facts">
                    {store.openingHours && <span><Clock3 size={14} /> {store.openingHours}</span>}
                    {store.phone && <a href={telHref(store.phone)}><Phone size={14} /> {store.phone}</a>}
                  </div>
                  {store.services?.length > 0 && (
                    <div className="store-card__services">
                      {store.services.map((service) => <span key={service}>{service}</span>)}
                    </div>
                  )}
                  <div className="store-card__footer">
                    {store.distance != null ? <strong>{store.distance.toFixed(1)} km từ bạn</strong> : <span>Trải nghiệm tại cửa hàng</span>}
                    <a href={directionsUrl(store.latitude, store.longitude)} target="_blank" rel="noreferrer">
                      Chỉ đường <Navigation size={14} />
                    </a>
                  </div>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="store-empty">
            <MapPin size={29} />
            <h3>Chưa tìm thấy cửa hàng phù hợp</h3>
            <p>Thử tìm bằng tên quận, đường hoặc tên cửa hàng khác.</p>
            <button type="button" onClick={() => setSearch("")}>Xem tất cả cửa hàng <ArrowRight size={15} /></button>
          </div>
        )}
      </section>
    </div>
  );
}
