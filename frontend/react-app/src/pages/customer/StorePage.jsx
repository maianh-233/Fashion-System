import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowRight,
  Clock3,
  LocateFixed,
  MapPin,
  Navigation,
  Phone,
  Search,
  Store,
} from "lucide-react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { stores as STORES } from "../../mock/stores";

const normalize = (value) =>
  value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();

const distanceInKm = ([lat1, lon1], [lat2, lon2]) => {
  const toRadians = (value) => (value * Math.PI) / 180;
  const earthRadius = 6371;
  const latDelta = toRadians(lat2 - lat1);
  const lonDelta = toRadians(lon2 - lon1);
  const a =
    Math.sin(latDelta / 2) ** 2 +
    Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.sin(lonDelta / 2) ** 2;
  return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
};

const markerIcon = L.divIcon({
  className: "store-map-marker-wrap",
  html: '<span class="store-map-marker"><span></span></span>',
  iconSize: [34, 42],
  iconAnchor: [17, 39],
  popupAnchor: [0, -36],
});

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
      const popup = `
        <article class="store-map-popup">
          <img src="${store.image}" alt="Không gian ${store.name}" />
          <div><small>${store.area}</small><h3>${store.name}</h3>
          <p>${store.address}</p><span>${store.hours} · ${store.phone}</span></div>
        </article>`;
      const marker = L.marker(store.coordinates, { icon: markerIcon })
        .bindPopup(popup, { className: "store-map-leaflet-popup", maxWidth: 290, closeButton: false, offset: [0, -2] })
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

export default function StorePage() {
  const [search, setSearch] = useState("");
  const [selectedId, setSelectedId] = useState(null);
  const [userLocation, setUserLocation] = useState(null);
  const [locationStatus, setLocationStatus] = useState("");

  const filteredStores = useMemo(() => {
    const term = normalize(search.trim());
    if (!term) return STORES;
    return STORES.filter((store) =>
      normalize(`${store.name} ${store.address} ${store.area}`).includes(term),
    );
  }, [search]);

  const storesWithDistance = useMemo(() => {
    if (!userLocation) return filteredStores;
    return [...filteredStores]
      .map((store) => ({ ...store, distance: distanceInKm(userLocation, store.coordinates) }))
      .sort((a, b) => a.distance - b.distance);
  }, [filteredStores, userLocation]);

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
        const nearest = [...STORES].sort(
          (a, b) => distanceInKm(position, a.coordinates) - distanceInKm(position, b.coordinates),
        )[0];
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
          <span><MapPin size={15} /> {filteredStores.length} cửa hàng</span>
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
          <span>Cập nhật hôm nay</span>
        </div>

        {storesWithDistance.length > 0 ? (
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
                  <img src={store.image} alt={`Không gian ${store.name}`} />
                  <span>{userLocation && index === 0 ? "Gần bạn nhất" : store.area}</span>
                </div>
                <div className="store-card__body">
                  <div className="store-card__title"><div><small>{store.area}</small><h3>{store.name}</h3></div><MapPin size={19} /></div>
                  <p>{store.address}</p>
                  <div className="store-card__facts">
                    <span><Clock3 size={14} /> {store.hours}</span>
                    <a href={`tel:${store.phone.replace(/\s/g, "")}`}><Phone size={14} /> {store.phone}</a>
                  </div>
                  <div className="store-card__services">
                    {store.services.map((service) => <span key={service}>{service}</span>)}
                  </div>
                  <div className="store-card__footer">
                    {store.distance != null ? <strong>{store.distance.toFixed(1)} km từ bạn</strong> : <span>Trải nghiệm tại cửa hàng</span>}
                    <a href={`https://www.google.com/maps/dir/?api=1&destination=${store.coordinates.join(",")}`} target="_blank" rel="noreferrer">
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
