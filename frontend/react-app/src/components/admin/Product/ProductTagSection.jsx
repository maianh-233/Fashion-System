import { useState } from "react";
import { Plus, Trash2, Pencil } from "lucide-react";
import ProductTagDialog from "./ProductTagDialog";


export default function ProductTagSection({ mode, tags }) {
  const isEdit = mode !== "view";

  const [open, setOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState("add");
  const [selectedTag, setSelectedTag] = useState(null);

  const handleAdd = () => {
    setDialogMode("add");
    setSelectedTag(null);
    setOpen(true);
  };

  const handleEdit = (tag) => {
    setDialogMode("edit");
    setSelectedTag(tag);
    setOpen(true);
  };

  return (
    <>
      <section>
        <div className="flex justify-between items-center mb-3">
          <h3 className="text-lg font-semibold text-orange-400">
            Tag sản phẩm
          </h3>

          {isEdit && (
            <button
              onClick={handleAdd}
              className="text-orange-400 hover:text-orange-500"
            >
              <Plus />
            </button>
          )}
        </div>

        <table className="w-full text-sm">
          <thead className="bg-[#1f1f1f]">
            <tr>
              <th className="p-2 text-left">Tên tag</th>
              <th className="p-2">Ngày tạo</th>
              {isEdit && <th className="p-2">Thao tác</th>}
            </tr>
          </thead>

          <tbody>
            {tags.map((tag) => (
              <tr
                key={tag.id}
                className="border-b border-gray-800"
              >
                <td className="p-2">{tag.name}</td>
                <td className="p-2">{tag.createdAt}</td>

                {isEdit && (
                  <td className="p-2 flex justify-center gap-3">
                    <button onClick={() => handleEdit(tag)}>
                      <Pencil
                        size={16}
                        className="text-blue-400"
                      />
                    </button>

                    <button>
                      <Trash2
                        size={16}
                        className="text-red-400"
                      />
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <ProductTagDialog
        open={open}
        mode={dialogMode}
        tag={selectedTag}
        onClose={() => setOpen(false)}
        onSubmit={(data) => {
          console.log(data);
          setOpen(false);
        }}
      />
    </>
  );
}