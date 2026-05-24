import { useState } from "react";
import { X, MapPin } from "lucide-react";

export default function AddAddressModal({ phone, onAdd, onClose }) {
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");

  const submit = () => {
    if (!name.trim()) return;
    onAdd({
      id: Date.now(),
      name,
      address,
      phone,
      isDefault: false,
    });
  };

  return (
    <div className="fixed inset-0 bg-black/90 flex items-center justify-center z-50 p-4">
      <div className="bg-zinc-900 rounded-3xl w-full max-w-xl">
        <div className="p-6 border-b border-zinc-700 flex justify-between">
          <h3 className="text-2xl font-bold text-amber-400">Thêm địa chỉ</h3>
          <X onClick={onClose} className="cursor-pointer" />
        </div>

        <div className="p-6 space-y-4">
          <input
            placeholder="Tên địa chỉ"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full bg-zinc-800 rounded-2xl px-5 py-4"
          />

          <input
            placeholder="Địa chỉ"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            className="w-full bg-zinc-800 rounded-2xl px-5 py-4"
          />

          <div className="h-56 bg-zinc-800 rounded-2xl flex flex-col items-center justify-center">
            <MapPin className="text-amber-400" size={48} />
            <p>Bản đồ mock</p>
          </div>

          <button
            onClick={submit}
            className="w-full bg-amber-500 hover:bg-amber-600 py-4 rounded-2xl font-bold text-black"
          >
            Thêm địa chỉ
          </button>
        </div>
      </div>
    </div>
  );
}