import Button from "../../common/Button";
import { AdminDialogFooter } from "../common/AdminDialog";
export default function DialogFooter({ mode, onClose }) {
  if (mode !== "view") return null;

  return (
    <AdminDialogFooter>
      <Button
        onClick={onClose}
        variant="primary"
        className="px-5 py-2 rounded-xl">
        Đóng
      </Button>
    </AdminDialogFooter>
  );
}
