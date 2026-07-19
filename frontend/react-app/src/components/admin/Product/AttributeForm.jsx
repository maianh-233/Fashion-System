import { useEffect, useState } from "react";

export default function AttributeForm({
  mode,
  attribute,
  onClose,
  onSubmit,
}) {
  const [attributeName, setAttributeName] = useState("");
  const [attributeValue, setAttributeValue] = useState("");

  useEffect(() => {
    if (mode === "edit" && attribute) {
      setAttributeName(attribute.attributeName);
      setAttributeValue(attribute.attributeValue);
    } else {
      setAttributeName("");
      setAttributeValue("");
    }
  }, [mode, attribute]);

  const handleSubmit = () => {
    onSubmit({
      id: attribute?.id,
      attributeName,
      attributeValue,
    });
  };

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50">

      <div className="w-[450px] bg-[#1b1b1b] rounded-lg p-6">

        <h3 className="text-xl font-semibold text-orange-400 mb-6">
          {mode === "add"
            ? "Thêm thuộc tính"
            : "Chỉnh sửa thuộc tính"}
        </h3>

        <div className="space-y-4">

          <div>
            <label className="block mb-1">
              Tên thuộc tính
            </label>

            <input
              className="w-full rounded bg-[#2a2a2a] p-2 outline-none"
              value={attributeName}
              onChange={(e) =>
                setAttributeName(e.target.value)
              }
            />
          </div>

          <div>
            <label className="block mb-1">
              Giá trị thuộc tính
            </label>

            <input
              className="w-full rounded bg-[#2a2a2a] p-2 outline-none"
              value={attributeValue}
              onChange={(e) =>
                setAttributeValue(e.target.value)
              }
            />
          </div>

        </div>

        <div className="flex justify-end gap-3 mt-6">

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