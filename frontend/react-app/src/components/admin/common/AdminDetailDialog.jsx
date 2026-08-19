import Button from "../../common/Button";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "./AdminDialog";

export default function AdminDetailDialog({
  open,
  title,
  description,
  onClose,
  size = "md",
  children,
  showFooter = false,
  closeLabel = "Đóng",
}) {
  return (
    <AdminDialog open={open} onClose={onClose} size={size}>
      <AdminDialogHeader
        title={title}
        description={description}
        onClose={onClose}
      />
      <AdminDialogBody>{children}</AdminDialogBody>
      {showFooter && (
        <AdminDialogFooter>
          <Button type="button" variant="secondary" onClick={onClose} className="rounded-xl px-5 py-2">
            {closeLabel}
          </Button>
        </AdminDialogFooter>
      )}
    </AdminDialog>
  );
}
