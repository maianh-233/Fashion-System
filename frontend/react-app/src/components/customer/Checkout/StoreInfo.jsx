import { StoreIcon } from "lucide-react";
export default function StoreInfo({ store }) {
  return (
    <div className="p-5 bg-orange-950/40 border border-orange-900 rounded-2xl">
      <h3 className="text-xl font-semibold mb-4 flex items-center gap-2">
        <StoreIcon size={20} />
        <span>Cửa hàng xử lý đơn hàng</span>
      </h3>
      <p className="font-medium">{store.name}</p>
      <p className="text-gray-400">{store.address}</p>
      <p className="text-gray-400">{store.phone}</p>
    </div>
  );
}