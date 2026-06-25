import { Eye, Pencil, Trash2 } from "lucide-react";

export default function AssignmentTable({ assignments, readOnly, onEdit, onDelete }) {
  return (
    <div className="overflow-x-auto bg-zinc-900 rounded-2xl border border-zinc-700">
      <table className="w-full text-sm">
        <thead className="bg-zinc-950 text-zinc-400">
          <tr>
            <th className="px-4 py-3 text-left">Cửa hàng</th>
            <th className="px-4 py-3">Mã</th>
            <th className="px-4 py-3">Trạng thái</th>
            <th className="px-4 py-3 text-center">Thao tác</th>
          </tr>
        </thead>

        <tbody className="divide-y divide-zinc-800">
          {assignments.map((a) => (
            <tr key={a.id} className="hover:bg-zinc-800">
              <td className="px-4 py-3">{a.store}</td>
              <td className="px-4 py-3">{a.code}</td>
              <td className="px-4 py-3">{a.status}</td>

              <td className="px-4 py-3">
                <div className="flex justify-center gap-3">
                  <Eye size={16} />

                  {!readOnly && (
                    <>
                      <button onClick={() => onEdit(a)}>
                        <Pencil size={16} />
                      </button>
                      <button
                        onClick={() => onDelete(a.id)}
                        className="text-red-400"
                      >
                        <Trash2 size={16} />
                      </button>
                    </>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}