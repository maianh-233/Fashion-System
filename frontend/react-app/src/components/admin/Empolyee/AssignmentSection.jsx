import { useState } from "react";
import { PlusCircle } from "lucide-react";
import AssignmentForm from "./AssignmentForm";
import AssignmentTable from "./AssignmentTable";
  const MOCK_ASSIGNMENTS = [
  {
    id: 1,
    store: "Cửa hàng Quận 1",
    code: "CH-Q1",
    status: "Đang làm",
    startDate: "2024-01-01",
    endDate: "",
  },
  {
    id: 2,
    store: "Cửa hàng Quận 7",
    code: "CH-Q7",
    status: "Tạm nghỉ",
    startDate: "2023-06-01",
    endDate: "2024-03-31",
  },
];


export default function AssignmentSection({ assignments = [], readOnly }) {

  const [data, setData] = useState(assignments.length ? assignments : MOCK_ASSIGNMENTS);

  const [showForm, setShowForm] = useState(false);
  const [mode, setMode] = useState("add"); // add | edit
  const [selectedAssignment, setSelectedAssignment] = useState(null);

  /* ================= OPEN FORM ================= */
  const openAdd = () => {
    setMode("add");
    setSelectedAssignment(null);
    setShowForm(true);
  };

  const openEdit = (assignment) => {
    setMode("edit");
    setSelectedAssignment(assignment);
    setShowForm(true);
  };

  /* ================= CRUD HANDLERS ================= */
  const handleAdd = (newAssignment) => {
    setData((prev) => [
      ...prev,
      { ...newAssignment, id: Date.now() },
    ]);
    setShowForm(false);
  };

  const handleEdit = (updatedAssignment) => {
    setData((prev) =>
      prev.map((a) =>
        a.id === updatedAssignment.id ? updatedAssignment : a
      )
    );
    setShowForm(false);
  };

  const handleDelete = (id) => {
    if (!confirm("Bạn có chắc muốn xóa phân công này?")) return;
    setData((prev) => prev.filter((a) => a.id !== id));
  };

  return (
    <section>
      {/* HEADER */}
      <div className="flex items-center justify-between mb-5">
        <h3 className="text-lg font-semibold text-white">
          Phân công cửa hàng
        </h3>

        {!readOnly && (
          <button
            onClick={openAdd}
            className="text-amber-400 hover:text-amber-300 transition"
            title="Thêm phân công"
          >
            <PlusCircle size={26} />
          </button>
        )}
      </div>

      {/* FORM */}
      {!readOnly && showForm && (
        <AssignmentForm
          mode={mode}
          assignment={selectedAssignment}
          onClose={() => setShowForm(false)}
          onAdd={handleAdd}
          onEdit={handleEdit}
        />
      )}

      {/* TABLE */}
      <AssignmentTable
        assignments={data}
        readOnly={readOnly}
        onEdit={openEdit}
        onDelete={handleDelete}
      />
    </section>
  );
}