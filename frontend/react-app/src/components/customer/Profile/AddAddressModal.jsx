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
    setForm({ ...form, [e.target.name]: e.target.value });
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
    <div className="fixed inset-0 z-50 bg-black/80 flex items-end sm:items-center justify-center">

      {/* Modal */}
      <div
        className="
          bg-zinc-900 w-full sm:max-w-2xl
          rounded-t-3xl sm:rounded-3xl
          max-h-[90vh] overflow-y-auto
        "
      >

        {/* Header */}
        <div className="sticky top-0 z-10 bg-zinc-900 border-b border-zinc-700 p-4 sm:p-6 flex justify-between items-center">
          <h3 className="text-lg sm:text-2xl font-bold text-amber-400">
            Thêm địa chỉ giao hàng
          </h3>
          <X
            onClick={onClose}
            className="cursor-pointer text-zinc-300 hover:text-white"
          />
        </div>

        {/* Form */}
        <div className="p-4 sm:p-6 grid grid-cols-1 sm:grid-cols-2 gap-4">

          <input
            name="receiver_name"
            placeholder="Tên người nhận"
            value={form.receiver_name}
            onChange={handleChange}
            className="sm:col-span-2 bg-zinc-800 rounded-xl px-4 py-4"
          />

          <input
            name="receiver_phone"
            placeholder="Số điện thoại"
            value={form.receiver_phone}
            onChange={handleChange}
            className="sm:col-span-2 bg-zinc-800 rounded-xl px-4 py-4"
          />

          <input
            name="province"
            placeholder="Tỉnh / Thành phố"
            value={form.province}
            onChange={handleChange}
            className="bg-zinc-800 rounded-xl px-4 py-4"
          />

          <input
            name="district"
            placeholder="Quận / Huyện"
            value={form.district}
            onChange={handleChange}
            className="bg-zinc-800 rounded-xl px-4 py-4"
          />

          <input
            name="ward"
            placeholder="Phường / Xã"
            value={form.ward}
            onChange={handleChange}
            className="bg-zinc-800 rounded-xl px-4 py-4"
          />

          <input
            name="postal_code"
            placeholder="Mã bưu điện"
            value={form.postal_code}
            onChange={handleChange}
            className="bg-zinc-800 rounded-xl px-4 py-4"
          />

          <textarea
            name="address_line"
            placeholder="Địa chỉ chi tiết"
            value={form.address_line}
            onChange={handleChange}
            className="sm:col-span-2 bg-zinc-800 rounded-xl px-4 py-4 h-24 resize-none"
          />

          <select
            name="address_type"
            value={form.address_type}
            onChange={handleChange}
            className="sm:col-span-2 bg-zinc-800 rounded-xl px-4 py-4"
          >
            <option value="HOME">Nhà riêng</option>
            <option value="WORK">Công ty</option>
            <option value="OTHER">Khác</option>
          </select>

          {/* Map mock */}
          <div className="sm:col-span-2 h-44 sm:h-56 bg-zinc-800 rounded-xl flex flex-col items-center justify-center">
            <MapPin className="text-amber-400" size={40} />
            <p className="text-sm text-zinc-400">Bản đồ (tích hợp sau)</p>
          </div>

          {/* Submit */}
          <button
            onClick={submit}
            className="
              sm:col-span-2
              bg-amber-500 hover:bg-amber-600
              py-4 rounded-xl
              font-bold text-black text-lg
              active:scale-[0.98]
            "
          >
            Lưu địa chỉ
          </button>
        </div>
      </div>
    </div>
  );
}