import { useEffect, useState } from "react";

export default function ProductTagDialog({
  open,
  mode,
  tag,
  onClose,
  onSubmit,
}) {
  const [name, setName] = useState("");

  useEffect(() => {
    if (mode === "edit" && tag) {
      setName(tag.name);
    } else {
      setName("");
    }
  }, [mode, tag]);

  if (!open) return null;

  const handleSubmit = () => {
    onSubmit({
      id: tag?.id,
      name,
    });
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex justify-center items-center z-50">

      <div className="w-[420px] rounded-lg bg-[#1d1d1d] p-6">

        <h2 className="text-xl font-semibold text-orange-400 mb-6">
          {mode === "add"
            ? "Thêm Tag"
            : "Chỉnh sửa Tag"}
        </h2>

        <div>
          <label className="block mb-2 text-sm">
            Tên Tag
          </label>

          <input
            className="w-full rounded border border-gray-700 bg-[#2a2a2a] p-2 outline-none focus:border-orange-400"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Nhập tên tag..."
          />
        </div>

        <div className="flex justify-end gap-3 mt-8">

          <button
            onClick={onClose}
            className="px-4 py-2 rounded bg-gray-700 hover:bg-gray-600"
          >
            Đóng
          </button>

          <button
            onClick={handleSubmit}
            className="px-4 py-2 rounded bg-orange-500 hover:bg-orange-600"
          >
            {mode === "add"
              ? "Thêm"
              : "Lưu thay đổi"}
          </button>

        </div>

      </div>

    </div>
  );
}