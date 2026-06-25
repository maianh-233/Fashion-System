import { useEffect, useState } from "react";

export default function AssignmentForm({ mode, assignment, onClose, onAdd, onEdit }) {
  const isEdit = mode === "edit";

  const [form, setForm] = useState({
    store: "",
    code: "",
    status: "Đang làm",
    startDate: "",
    endDate: "",
  });

  /* LOAD DATA KHI EDIT */
  useEffect(() => {
    if (assignment) {
      setForm(assignment);
    }
  }, [assignment]);

  const handleChange = (key, value) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleSubmit = () => {
    if (isEdit) {
      onEdit(form);
    } else {
      onAdd(form);
    }
  };

  return (
    <div className="bg-zinc-900 border border-zinc-700 rounded-2xl p-6 mb-6">
      <h4 className="text-white font-semibold mb-4">
        {isEdit ? "Chỉnh sửa phân công" : "Thêm phân công mới"}
      </h4>

      <div className="grid grid-cols-2 gap-4">
        <Input
          label="Cửa hàng"
          value={form.store}
          onChange={(e) => handleChange("store", e.target.value)}
        />

        <Input
          label="Mã cửa hàng"
          value={form.code}
          onChange={(e) => handleChange("code", e.target.value)}
        />

        {isEdit && (
          <>
            <Select
              label="Trạng thái"
              value={form.status}
              onChange={(e) => handleChange("status", e.target.value)}
            />
            <Input
              type="date"
              label="Ngày bắt đầu"
              value={form.startDate}
              onChange={(e) => handleChange("startDate", e.target.value)}
            />
            <Input
              type="date"
              label="Ngày kết thúc"
              value={form.endDate}
              onChange={(e) => handleChange("endDate", e.target.value)}
            />
          </>
        )}
      </div>

      <div className="flex justify-end gap-3 mt-6">
        <button onClick={onClose} className="px-5 py-2 rounded-xl bg-zinc-700">
          Hủy
        </button>

        <button
          onClick={handleSubmit}
          className="px-5 py-2 rounded-xl bg-amber-500 text-black font-medium"
        >
          {isEdit ? "Cập nhật" : "Thêm phân công"}
        </button>
      </div>
    </div>
  );
}

/* ==== INPUT / SELECT ==== */

function Input({ label, ...props }) {
  return (
    <div>
      <label className="block text-sm text-zinc-400 mb-2">{label}</label>
      <input
        {...props}
        className="w-full px-4 py-3 rounded-xl bg-zinc-800 border border-zinc-700 text-white"
      />
    </div>
  );
}

function Select({ label, ...props }) {
  return (
    <div>
      <label className="block text-sm text-zinc-400 mb-2">{label}</label>
      <select
        {...props}
        className="w-full px-4 py-3 rounded-xl bg-zinc-800 border border-zinc-700 text-white"
      >
        <option>Đang làm</option>
        <option>Tạm nghỉ</option>
        <option>Đã kết thúc</option>
      </select>
    </div>
  );
}

