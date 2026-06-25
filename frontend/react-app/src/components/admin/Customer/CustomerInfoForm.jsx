// CustomerInfoForm.jsx
import { useState } from "react";

export default function CustomerInfoForm({ mode, customer, onSubmit }) {
  const [form, setForm] = useState({
    name: customer?.name || "",
    email: customer?.email || "",
    phone: customer?.phone || "",
  });

  return (
    <form
      className="flex-1 space-y-5"
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit?.(form);
      }}
    >
      <Input
        label="Tên khách hàng"
        value={form.name}
        onChange={(v) => setForm({ ...form, name: v })}
      />

      <Input
        label="Email"
        value={form.email}
        onChange={(v) => setForm({ ...form, email: v })}
      />

      <Input
        label="Số điện thoại"
        value={form.phone}
        onChange={(v) => setForm({ ...form, phone: v })}
      />

      <button className="w-full bg-[#FFB300] py-3 rounded-2xl font-semibold text-black">
        {mode === "create" ? "Thêm khách hàng" : "Lưu thay đổi"}
      </button>
    </form>
  );
}

function Input({ label, value, onChange }) {
  return (
    <div>
      <label className="text-zinc-400 text-sm">{label}</label>
      <input
        className="w-full mt-1 px-4 py-3 bg-zinc-800 rounded-xl outline-none"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  );
}