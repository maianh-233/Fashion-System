import DialogHeader from "./DialogHeader";
import DialogFooter from "./DialogFooter";
import GeneralInfoSection from "./GeneralInfoSection";
import AssignmentSection from "./AssignmentSection";

export default function EmployeeDialog({
  mode = "view",
  employee,
  onClose,
  onSave,
  onDelete,
}) {
  const isView = mode === "view";
  const isCreate = mode === "create";

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center p-4 z-50">
      <div className="bg-zinc-900 border border-zinc-700 rounded-3xl shadow-2xl w-full max-w-3xl overflow-hidden">

        <DialogHeader mode={mode} onClose={onClose} />

        <div className="p-8 space-y-10 max-h-[75vh] overflow-y-auto scrollbar-hide">
          <GeneralInfoSection
            employee={employee}
            readOnly={isView}
          />

          <AssignmentSection
            mode={mode}
            assignments={employee?.assignments || []}
          />
        </div>

        <DialogFooter
          mode={mode}
          onClose={onClose}
          onSave={onSave}
          onDelete={onDelete}
        />
      </div>
    </div>
  );
}