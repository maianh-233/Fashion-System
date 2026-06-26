import Pagination from "../../common/Pagination";

export default function StoreStaffList({ staffData }) {
  const { items = [], page, totalPages, onPageChange } = staffData || {};

  return (
    <div className="border border-zinc-700 rounded-xl overflow-hidden">
      <div className="px-4 py-3 bg-zinc-800 font-medium">
        Nhân viên cửa hàng
      </div>

      <table className="w-full text-sm">
        <thead className="bg-zinc-900 text-gray-400">
          <tr>
            <th className="p-3 text-left">Tên</th>
            <th className="p-3">Vai trò</th>
            <th className="p-3">Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          {items.map((s) => (
            <tr key={s.id} className="border-t border-zinc-700">
              <td className="p-3">{s.name}</td>
              <td className="p-3 text-center">{s.staff_role}</td>
              <td className="p-3 text-center">
                {s.active ? "Đang làm" : "Nghỉ"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <Pagination
        currentPage={page}
        totalPages={totalPages}
        onPageChange={onPageChange}
      />
    </div>
  );
}