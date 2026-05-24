import { useState } from "react";
import { X, MapPin } from "lucide-react";

export default function AddAddressModal({ userId, onAdd, onClose }) {
  const [form, setForm] = useState({
    receiver_name: "",
    receiver_phone: "",
    province: "",
    district: "",
    ward: "",
    address_line: "",
    postal_code: "",
    address_type: "HOME",
  });

  const handleChange = (e) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    });
  };

  const submit = () => {
    if (!form.receiver_name.trim() || !form.receiver_phone.trim()) return;

    onAdd({
      id: crypto.randomUUID(),
      user_id: userId,
      ...form,
      is_default: false,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    });

    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/90 flex items-center justify-center z-50 p-4">
      <div className="bg-zinc-900 rounded-3xl w-full max-w-2xl">

        {/* Header */}
        <div className="p-6 border-b border-zinc-700 flex justify-between items-center">
          <h3 className="text-2xl font-bold text-amber-400">
            Thêm địa chỉ giao hàng
          </h3>
          <X onClick={onClose} className="cursor-pointer" />
        </div>

        {/* Form */}
        <div className="p-6 grid grid-cols-2 gap-4">

          <input
            name="receiver_name"
            placeholder="Tên người nhận"
            value={form.receiver_name}
            onChange={handleChange}
            className="col-span-2 bg-zinc-800 rounded-2xl px-5 py-4"
          />

          <input
            name="receiver_phone"
            placeholder="Số điện thoại"
            value={form.receiver_phone}
            onChange={handleChange}
            className="col-span-2 bg-zinc-800 rounded-2xl px-5 py-4"
          />

          <input
            name="province"
            placeholder="Tỉnh / Thành phố"
            value={form.province}
            onChange={handleChange}
            className="bg-zinc-800 rounded-2xl px-5 py-4"
          />

          <input
            name="district"
            placeholder="Quận / Huyện"
            value={form.district}
            onChange={handleChange}
            className="bg-zinc-800 rounded-2xl px-5 py-4"
          />

          <input
            name="ward"
            placeholder="Phường / Xã"
            value={form.ward}
            onChange={handleChange}
            className="bg-zinc-800 rounded-2xl px-5 py-4"
          />

          <input
            name="postal_code"
            placeholder="Mã bưu điện (nếu có)"
            value={form.postal_code}
            onChange={handleChange}
            className="bg-zinc-800 rounded-2xl px-5 py-4"
          />

          <textarea
            name="address_line"
            placeholder="Địa chỉ chi tiết (số nhà, tên đường...)"
            value={form.address_line}
            onChange={handleChange}
            className="col-span-2 bg-zinc-800 rounded-2xl px-5 py-4 h-28 resize-none"
          />

          <select
            name="address_type"
            value={form.address_type}
            onChange={handleChange}
            className="col-span-2 bg-zinc-800 rounded-2xl px-5 py-4"
          >
            <option value="HOME">Nhà riêng</option>
            <option value="WORK">Công ty</option>
            <option value="OTHER">Khác</option>
          </select>

          {/* Mock map */}
          <div className="col-span-2 h-56 bg-zinc-800 rounded-2xl flex flex-col items-center justify-center">
            <MapPin className="text-amber-400" size={48} />
            <p className="text-zinc-400">Bản đồ (tích hợp sau)</p>
          </div>

          {/* Submit */}
          <button
            onClick={submit}
            className="col-span-2 bg-amber-500 hover:bg-amber-600 py-4 rounded-2xl font-bold text-black"
          >
            Lưu địa chỉ
          </button>
        </div>
      </div>
    </div>
  );
}