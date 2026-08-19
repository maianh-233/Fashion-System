import { AdminDialogHeader } from "../common/AdminDialog";
export default function DialogHeader({ mode, onClose }) {
  const titleMap = {
    view: "Chi tiết nhân viên",
    edit: "Chỉnh sửa nhân viên",
    create: "Thêm nhân viên mới",
  };

  return <AdminDialogHeader title={titleMap[mode]} onClose={onClose} />;
}
