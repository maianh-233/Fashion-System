import { InfoIcon } from "lucide-react";

export default function ShippingForm({ form, setForm, readOnly = false }) {
  if (!form) return null;

  const change = (e) => {
    if (readOnly) return;
    const { name, value } = e.target;
    setForm?.((prev) => ({ ...prev, [name]: value }));
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
    ${readOnly ? "opacity-70 cursor-not-allowed" : ""}
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
          value={form.receiver_name}
          onChange={change}
          readOnly={readOnly}
          className={inputClass}
        />

        <input
          name="receiver_phone"
          value={form.receiver_phone}
          readOnly={readOnly}
          onChange={change}
          className={inputClass}
        />
      </div>

      {/* ĐỊA CHỈ */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mt-4">
        <input
          name="province"
          value={form.province}
          readOnly={readOnly}
          onChange={change}
          className={inputClass}
        />
        <input
          name="district"
          value={form.district}
          readOnly={readOnly}
          onChange={change}
          className={inputClass}
        />
        <input
          name="ward"
          value={form.ward}
          readOnly={readOnly}
          onChange={change}
          className={inputClass}
        />
      </div>

      {/* ĐỊA CHỈ CHI TIẾT */}
      <textarea
        name="address_line"
        value={form.address_line}
        readOnly={readOnly}
        onChange={change}
        className="
          mt-4 w-full bg-zinc-800 rounded-xl
          px-4 py-3 text-white resize-none
          placeholder:text-gray-400
          focus:outline-none focus:ring-2
          focus:ring-blue-500/60
          opacity-70
        "
      />
    </div>
  );
}