export default function StoreMapView({ latitude, longitude }) {
  if (!latitude || !longitude) return null;

  return (
    <div>
      <p className="text-sm text-gray-400 mb-2">Vị trí cửa hàng</p>
      <iframe
        title="store-map"
        className="w-full h-64 rounded-xl border border-zinc-700"
        src={`https://maps.google.com/maps?q=${latitude},${longitude}&z=15&output=embed`}
      />
    </div>
  );
}