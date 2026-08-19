// CustomerDialog.jsx
import AdminDialog, { AdminDialogBody } from "../common/AdminDialog";
import DialogHeader from "./DialogHeader";
import DialogFooter from "./DialogFooter";
import CustomerAvatar from "./CustomerAvatar";
import CustomerInfoView from "./CustomerInfoView";
import CustomerInfoForm from "./CustomerInfoForm";
import AddressList from "./AddressList";

export default function CustomerDialog({
  open,
  mode, // "view" | "create" | "edit"
  customer,
  onClose,
  onSubmit,
}) {
  if (!open) return null;

  return (
    <AdminDialog open={open} onClose={onClose} size="md">
        <DialogHeader mode={mode} onClose={onClose} />

        <AdminDialogBody>
          <div className="flex flex-col md:flex-row gap-8">
            <CustomerAvatar customer={customer} />

            {mode === "view" ? (
              <CustomerInfoView customer={customer} />
            ) : (
              <CustomerInfoForm
                mode={mode}
                customer={customer}
                onSubmit={onSubmit}
              />
            )}
          </div>

          {mode === "view" && customer?.addresses && (
            <AddressList addresses={customer.addresses} />
          )}
        </AdminDialogBody>

        <DialogFooter mode={mode} onClose={onClose} />
    </AdminDialog>
  );
}
