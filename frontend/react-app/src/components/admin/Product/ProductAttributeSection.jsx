import { Plus, Pencil, Trash2 } from "lucide-react";

export default function ProductAttributeSection({ mode, attributes }) {
  const isEdit = mode !== "view";

  

  return (
    <section>
      <div className="flex justify-between items-center mb-3">
        <h3 className="text-lg font-semibold text-orange-400">
          Thuộc tính sản phẩm
        </h3>
        {isEdit && (
          <button className="text-orange-400 hover:text-orange-500">
            <Plus />
          </button>
        )}
      </div>

      <table className="w-full text-sm">
        <thead className="bg-[#1f1f1f]">
          <tr>
            <th className="p-2 text-left">Tên</th>
            <th className="p-2 text-left">Giá trị</th>
            <th className="p-2">Ngày tạo</th>
            {isEdit && <th className="p-2">Thao tác</th>}
          </tr>
        </thead>
        <tbody>
          {attributes.map(attr => (
            <tr key={attr.id} className="border-b border-gray-800">
              <td className="p-2">{attr.attributeName}</td>
              <td className="p-2">{attr.attributeValue}</td>
              <td className="p-2">{attr.createdAt}</td>
              {isEdit && (
                <td className="p-2 flex gap-2 justify-center">
                  <Pencil size={16} className="cursor-pointer text-blue-400" />
                  <Trash2 size={16} className="cursor-pointer text-red-400" />
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}