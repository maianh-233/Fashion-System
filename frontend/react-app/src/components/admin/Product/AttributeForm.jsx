import Button from "../../common/Button";
import { useEffect, useState } from "react";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";

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
    <AdminDialog open onClose={onClose} size="sm">
      <AdminDialogHeader
        title={mode === "add" ? "Thêm thuộc tính" : "Chỉnh sửa thuộc tính"}
        onClose={onClose}
      />
      <AdminDialogBody className="space-y-4">

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

      </AdminDialogBody>
      <AdminDialogFooter>
          <Button
            onClick={onClose}
            className="px-4 py-2 rounded bg-gray-700 hover:bg-gray-600"
          >
            Đóng
          </Button>

          <Button
            onClick={handleSubmit}
            className="px-4 py-2 rounded bg-orange-500 hover:bg-orange-600"
          >
            {mode === "add"
              ? "Thêm"
              : "Lưu thay đổi"}
          </Button>

      </AdminDialogFooter>
    </AdminDialog>
  );
}
