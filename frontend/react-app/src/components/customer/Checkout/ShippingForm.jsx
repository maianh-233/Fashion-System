import { InfoIcon } from "lucide-react";
export default function ShippingForm({ form, setForm }) {
  if (!form) return null;

  const change = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const inputClass = `
    bg-zinc-800
    rounded-xl
    px-4 py-3
    text-white
    placeholder:text-gray-400
    focus:outline-none
    focus:ring-2
    focus:ring-blue-500/60
  `;


  return (
    <div className="bg-zinc-900 rounded-2xl shadow-sm p-6">
      <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
        <InfoIcon size={26} />
        <span>Thông tin giao hàng</span>
      </h2>

      {/* TÊN + SĐT */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <input
          name="receiver_name"
          placeholder="Họ tên người nhận"
          value={form.receiver_name}
          onChange={change}
          className={inputClass}
        />

        <input
          name="receiver_phone"
          placeholder="Số điện thoại"
          inputMode="tel"
          value={form.receiver_phone}
          onChange={change}
          className={inputClass}
        />
      </div>

      {/* ĐỊA CHỈ */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mt-4">
        <input
          name="province"
          placeholder="Tỉnh / Thành phố"
          value={form.province}
          onChange={change}
          className={inputClass}
        />

        <input
          name="district"
          placeholder="Quận / Huyện"
          value={form.district}
          onChange={change}
          className={inputClass}
        />

        <input
          name="ward"
          placeholder="Phường / Xã"
          value={form.ward}
          onChange={change}
          className={inputClass}
        />
      </div>

      {/* ĐỊA CHỈ CHI TIẾT */}
      <textarea
        name="address_line"
        placeholder="Địa chỉ chi tiết (số nhà, tên đường...)"
        value={form.address_line}
        onChange={change}
        className="
    mt-4
    w-full
    bg-zinc-800
    rounded-xl
    px-4 py-3
    text-white
    placeholder:text-gray-400
    focus:outline-none
    focus:ring-2
    focus:ring-blue-500/60
    resize-none
    overflow-hidden
    leading-relaxed
    min-h-[48px]
  "
        onInput={(e) => {
          e.target.style.height = "auto";
          e.target.style.height = `${e.target.scrollHeight}px`;
        }}
      />
    </div>
  );
}
