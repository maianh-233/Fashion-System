import Button from "../../common/Button";
import { useEffect, useState } from "react";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";

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
    <AdminDialog open={open} onClose={onClose} size="sm">
      <AdminDialogHeader
        title={mode === "add" ? "Thêm tag" : "Chỉnh sửa tag"}
        onClose={onClose}
      />
      <AdminDialogBody>
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
