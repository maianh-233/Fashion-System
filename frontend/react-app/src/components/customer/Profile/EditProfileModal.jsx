import { useState, useRef } from "react";
import { X, Calendar } from "lucide-react";

export default function EditProfileModal({ user, onClose, onSave }) {
  const [form, setForm] = useState(user);
  const [password, setPassword] = useState("");
  const dateRef = useRef(null);

  const submit = () => {
    onSave({ ...form, password });
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/90 flex items-center justify-center z-50 p-4">
      <div className="bg-zinc-900 rounded-3xl w-full max-w-xl overflow-hidden">

        {/* HEADER */}
        <div className="p-6 border-b border-zinc-700 flex justify-between items-center">
          <h3 className="text-2xl font-bold text-amber-400">
            Sửa thông tin cá nhân
          </h3>
          <X
            onClick={onClose}
            className="cursor-pointer text-zinc-400 hover:text-white"
          />
        </div>

        {/* BODY */}
        <div className="p-6 space-y-4">

          {/* TÊN */}
          <input
            type="text"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            placeholder="Họ và tên"
            className="w-full bg-zinc-800 rounded-2xl px-5 py-4 text-zinc-200"
          />

          {/* EMAIL */}
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            placeholder="Email"
            className="w-full bg-zinc-800 rounded-2xl px-5 py-4 text-zinc-200"
          />

          {/* PHONE */}
          <input
            type="text"
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
            placeholder="Số điện thoại"
            className="w-full bg-zinc-800 rounded-2xl px-5 py-4 text-zinc-200"
          />

          {/* NGÀY SINH – ICON CUSTOM */}
          <div className="relative">
            <input
              ref={dateRef}
              type="date"
              value={form.birthDate}
              onChange={(e) =>
                setForm({ ...form, birthDate: e.target.value })
              }
              className="
                w-full bg-zinc-800 rounded-2xl
                px-5 py-4 pr-12
                text-zinc-200
                appearance-none
              "
            />

            <Calendar
              size={22}
              onClick={() => dateRef.current?.showPicker()}
              className="
                absolute right-4 top-1/2 -translate-y-1/2
                text-amber-400 cursor-pointer
                hover:text-amber-300 transition
              "
            />
          </div>

          {/* ĐỔI MẬT KHẨU */}
          <input
            type="password"
            placeholder="Mật khẩu mới"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full bg-zinc-800 rounded-2xl px-5 py-4 text-zinc-200"
          />

          {/* ACTION */}
          <button
            onClick={submit}
            className="
              w-full bg-amber-500 hover:bg-amber-600
              py-4 rounded-2xl font-bold text-black
              transition
            "
          >
            Lưu thay đổi
          </button>

        </div>
      </div>
    </div>
  );
}