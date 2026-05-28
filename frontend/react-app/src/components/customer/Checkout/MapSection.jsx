import { useEffect, useRef } from "react";
import { Map } from "lucide-react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

export default function MapSection({ address, store }) {
  const mapRef = useRef(null);

  useEffect(() => {
    if (!mapRef.current) {
      mapRef.current = L.map("map").setView(
        [address.latitude, address.longitude],
        13,
      );

      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png").addTo(
        mapRef.current,
      );
    }

    mapRef.current.eachLayer((layer) => {
      if (layer instanceof L.Marker) mapRef.current.removeLayer(layer);
    });

    const customerMarker = L.marker([
      address.latitude,
      address.longitude,
    ]).addTo(mapRef.current);
    const storeMarker = L.marker([store.latitude, store.longitude]).addTo(
      mapRef.current,
    );

    const group = L.featureGroup([customerMarker, storeMarker]);
    mapRef.current.fitBounds(group.getBounds(), { padding: [50, 50] });
  }, [address, store]);

  return (
    <div className="bg-zinc-900 rounded-2xl p-6">
      <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
        <Map size={26} />
        <span>Vị trí giao hàng & Cửa hàng</span>
      </h2>

      <div id="map" className="w-full h-96 rounded-xl border border-zinc-700" />
    </div>
  );
}
