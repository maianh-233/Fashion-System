// CustomerDialog.jsx
import DialogOverlay from "./DialogOverlay";
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
    <DialogOverlay>
      <div className="bg-zinc-900 w-full max-w-3xl mx-4 rounded-3xl border border-[#FFB300]/30 overflow-hidden">
        <DialogHeader mode={mode} onClose={onClose} />

        <div className="p-8">
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
        </div>

        <DialogFooter mode={mode} onClose={onClose} />
      </div>
    </DialogOverlay>
  );
}