import Button from "../../common/Button";
import { useState } from "react";
import { Plus, Pencil, Trash2 } from "lucide-react";
import AttributeForm from "./AttributeForm";

export default function ProductAttributeSection({ mode, attributes }) {
  const isEdit = mode !== "view";

  const [showForm, setShowForm] = useState(false);
  const [formMode, setFormMode] = useState("add"); // add | edit
  const [selectedAttribute, setSelectedAttribute] = useState(null);

  const handleAdd = () => {
    setFormMode("add");
    setSelectedAttribute(null);
    setShowForm(true);
  };

  const handleEdit = (attribute) => {
    setFormMode("edit");
    setSelectedAttribute(attribute);
    setShowForm(true);
  };

  return (
    <>
      <section>
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-lg font-semibold text-orange-400">
            Thuộc tính sản phẩm
          </h3>

          {isEdit && (
            <Button
              onClick={handleAdd}
              className="text-orange-400 hover:text-orange-500"
            >
              <Plus />
            </Button>
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
            {attributes.map((attr) => (
              <tr key={attr.id} className="border-b border-gray-800">
                <td className="p-2">{attr.attributeName}</td>
                <td className="p-2">{attr.attributeValue}</td>
                <td className="p-2">{attr.createdAt}</td>

                {isEdit && (
                  <td className="p-2 flex justify-center gap-2">
                    <Button onClick={() => handleEdit(attr)}>
                      <Pencil
                        size={16}
                        className="cursor-pointer text-blue-400"
                      />
                    </Button>

                    <Trash2
                      size={16}
                      className="cursor-pointer text-red-400"
                    />
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      {showForm && (
        <AttributeForm
          mode={formMode}
          attribute={selectedAttribute}
          onClose={() => setShowForm(false)}
          onSubmit={(data) => {
            console.log(data);

            // call api thêm hoặc sửa

            setShowForm(false);
          }}
        />
      )}
    </>
  );
}