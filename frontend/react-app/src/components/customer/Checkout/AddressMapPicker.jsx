import { useEffect, useMemo, useState } from "react";
import { LocateFixed } from "lucide-react";
import L from "leaflet";
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from "react-leaflet";
import "leaflet/dist/leaflet.css";

// Trung tâm TP.HCM khi khách chưa chọn vị trí.
const DEFAULT_CENTER = [10.7769, 106.7009];

const PIN = L.divIcon({
  className: "address-map-pin",
  html: "<span></span>",
  iconSize: [28, 28],
  iconAnchor: [14, 28],
});

function ClickToPlace({ onPick }) {
  useMapEvents({ click: (event) => onPick([event.latlng.lat, event.latlng.lng]) });
  return null;
}

function Recenter({ position }) {
  const map = useMap();
  useEffect(() => {
    if (position) map.setView(position, Math.max(map.getZoom(), 16), { animate: true });
  }, [map, position]);
  return null;
}

/**
 * Chọn tọa độ giao hàng trên bản đồ OpenStreetMap: bấm vào bản đồ hoặc kéo ghim; nút "Dùng vị trí hiện tại" lấy
 * GPS của trình duyệt. Tọa độ dùng để phân đơn về cửa hàng gần nhất còn hàng.
 */
export default function AddressMapPicker({ value, onChange }) {
  const position = value?.latitude != null && value?.longitude != null ? [Number(value.latitude), Number(value.longitude)] : null;
  const [locating, setLocating] = useState(false);
  const [error, setError] = useState("");
  const pick = (latLng) => onChange({ latitude: Number(latLng[0].toFixed(6)), longitude: Number(latLng[1].toFixed(6)) });

  const handlers = useMemo(() => ({
    dragend: (event) => {
      const { lat, lng } = event.target.getLatLng();
      pick([lat, lng]);
    },
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }), [onChange]);

  const locate = () => {
    if (!navigator.geolocation) {
      setError("Trình duyệt không hỗ trợ định vị.");
      return;
    }
    setLocating(true);
    setError("");
    navigator.geolocation.getCurrentPosition(
      (result) => {
        setLocating(false);
        pick([result.coords.latitude, result.coords.longitude]);
      },
      () => {
        setLocating(false);
        setError("Không lấy được vị trí. Hãy cho phép truy cập vị trí hoặc chọn trực tiếp trên bản đồ.");
      },
      { enableHighAccuracy: true, timeout: 10000 },
    );
  };

  return (
    <div className="address-map">
      <div className="address-map__toolbar">
        <span>{position ? `Đã chọn: ${position[0].toFixed(5)}, ${position[1].toFixed(5)}` : "Bấm vào bản đồ để đặt ghim tại nơi nhận hàng"}</span>
        <button type="button" onClick={locate} disabled={locating}>
          <LocateFixed size={14} /> {locating ? "Đang định vị..." : "Dùng vị trí hiện tại"}
        </button>
      </div>
      <MapContainer center={position || DEFAULT_CENTER} zoom={position ? 16 : 12} scrollWheelZoom className="address-map__canvas">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <ClickToPlace onPick={pick} />
        <Recenter position={position} />
        {position && <Marker position={position} draggable icon={PIN} eventHandlers={handlers} />}
      </MapContainer>
      {error && <p className="address-map__error">{error}</p>}
    </div>
  );
}
