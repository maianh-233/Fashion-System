import { AdminDialogHeader } from "../common/AdminDialog";
export default function DialogHeader({ mode, onClose }) {
  const titles = {
    view: "Chi Tiết Khách Hàng",
    create: "Thêm Khách Hàng",
    edit: "Chỉnh Sửa Khách Hàng",
  };

  return <AdminDialogHeader title={titles[mode]} onClose={onClose} />;
}
