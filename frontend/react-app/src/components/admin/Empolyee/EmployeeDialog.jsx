import DialogHeader from "./DialogHeader";
import DialogFooter from "./DialogFooter";
import GeneralInfoSection from "./GeneralInfoSection";
import AdminDialog, { AdminDialogBody } from "../common/AdminDialog";


export default function EmployeeDialog({
  mode = "view",
  employee,
  onClose,
  onSave,
  onDelete,
}) {
  const isView = mode === "view";

  return (
    <AdminDialog open onClose={onClose} size="md">
        <DialogHeader mode={mode} onClose={onClose} />

        <AdminDialogBody className="space-y-10">
          <GeneralInfoSection
            employee={employee}
            readOnly={isView}
          />

        </AdminDialogBody>

        <DialogFooter
          mode={mode}
          onClose={onClose}
          onSave={onSave}
          onDelete={onDelete}
        />
    </AdminDialog>
  );
}
