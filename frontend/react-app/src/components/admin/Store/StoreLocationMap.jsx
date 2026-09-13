import { useEffect, useRef } from "react";
import { Crosshair, MapPin } from "lucide-react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

const DEFAULT_CENTER = [10.7769, 106.7009];

const markerIcon = L.divIcon({
  className: "store-map-marker-wrap",
  html: '<span class="store-map-marker"><span></span></span>',
  iconSize: [34, 42],
  iconAnchor: [17, 39],
});

function getCoordinates(latitude, longitude) {
  const lat = Number(latitude);
  const lng = Number(longitude);

  if (
    latitude === "" ||
    latitude == null ||
    longitude === "" ||
    longitude == null ||
    !Number.isFinite(lat) ||
    !Number.isFinite(lng) ||
    lat < -90 ||
    lat > 90 ||
    lng < -180 ||
    lng > 180
  ) {
    return null;
  }

  return [lat, lng];
}

export default function StoreLocationMap({ latitude, longitude, readOnly = false, onCoordinatesChange }) {
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const readOnlyRef = useRef(readOnly);
  const onCoordinatesChangeRef = useRef(onCoordinatesChange);
  const coordinates = getCoordinates(latitude, longitude);
  const coordinateLatitude = coordinates?.[0] ?? null;
  const coordinateLongitude = coordinates?.[1] ?? null;

  useEffect(() => {
    readOnlyRef.current = readOnly;
    onCoordinatesChangeRef.current = onCoordinatesChange;
  }, [readOnly, onCoordinatesChange]);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return undefined;

    const map = L.map(containerRef.current, {
      center: DEFAULT_CENTER,
      zoom: 12,
      zoomControl: false,
      scrollWheelZoom: false,
    });

    L.control.zoom({ position: "bottomright" }).addTo(map);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19,
    }).addTo(map);

    map.on("click", ({ latlng }) => {
      if (readOnlyRef.current) return;
      onCoordinatesChangeRef.current?.({
        latitude: latlng.lat.toFixed(6),
        longitude: latlng.lng.toFixed(6),
      });
    });

    mapRef.current = map;
    const resizeTimer = window.setTimeout(() => map.invalidateSize(), 80);

    return () => {
      window.clearTimeout(resizeTimer);
      map.remove();
      mapRef.current = null;
      markerRef.current = null;
    };
  }, []);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    if (coordinateLatitude == null || coordinateLongitude == null) {
      markerRef.current?.remove();
      markerRef.current = null;
      return;
    }

    const position = [coordinateLatitude, coordinateLongitude];
    if (markerRef.current) markerRef.current.setLatLng(position);
    else markerRef.current = L.marker(position, { icon: markerIcon }).addTo(map);

    map.setView(position, 15, { animate: false });
  }, [coordinateLatitude, coordinateLongitude]);

  return (
    <section className="overflow-hidden rounded-2xl border border-zinc-700 bg-zinc-900" aria-label="Vị trí cửa hàng trên bản đồ">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-zinc-700 px-4 py-3">
        <div className="flex items-center gap-2 text-sm font-medium text-zinc-200">
          <MapPin size={17} className="text-amber-400" />
          Vị trí trên bản đồ
        </div>
        <span className="font-mono text-xs text-zinc-400">
          {coordinates ? `${coordinates[0].toFixed(6)}, ${coordinates[1].toFixed(6)}` : "Chưa có tọa độ"}
        </span>
      </div>
      <div className="relative">
        <div ref={containerRef} className="h-64 w-full" />
        {!coordinates && (
          <div className="pointer-events-none absolute inset-0 z-[400] grid place-items-center bg-zinc-950/45 p-6 text-center backdrop-blur-[1px]">
            <div className="rounded-2xl border border-zinc-700 bg-zinc-900/95 px-5 py-4 text-sm text-zinc-300 shadow-xl">
              <Crosshair size={22} className="mx-auto mb-2 text-amber-400" />
              {readOnly ? "Cửa hàng chưa có tọa độ." : "Nhập tọa độ hoặc nhấp lên bản đồ để chọn vị trí."}
            </div>
          </div>
        )}
      </div>
      {!readOnly && coordinates && (
        <p className="border-t border-zinc-700 px-4 py-2 text-xs text-zinc-500">
          Nhấp vào vị trí khác trên bản đồ để cập nhật tọa độ.
        </p>
      )}
    </section>
  );
}
