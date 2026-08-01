import {
  Plus,
  Trash2,
} from "lucide-react";

export default function GoodsIssueItemSection({
  mode = "view",
  items = [],
  summary,
  onAddProduct,
  onUpdateItem,
  onRemoveItem,
}) {
  const isView = mode === "view";

  return (
    <div className="rounded-2xl border border-zinc-700 bg-[#1b1b1b] p-6">
      {/* ================= HEADER ================= */}

      <div className="mb-6 flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-white">
            Danh sách sản phẩm xuất
          </h3>

          <p className="text-sm text-zinc-500">
            Chọn các sản phẩm cần xuất kho
          </p>
        </div>

        {!isView && (
          <button
            onClick={onAddProduct}
            className="flex items-center gap-2 rounded-lg bg-orange-500 px-4 py-2 text-white hover:bg-orange-600"
          >
            <Plus size={18} />
            Thêm sản phẩm
          </button>
        )}
      </div>

      {/* ================= EMPTY ================= */}

      {items.length === 0 ? (
        <div className="rounded-xl border border-dashed border-zinc-700 py-12 text-center text-zinc-500">
          Chưa có sản phẩm nào
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full">
            <thead>
              <tr className="border-b border-zinc-700 text-left text-sm text-zinc-400">
                <th className="px-4 py-3">
                  Sản phẩm
                </th>

                <th className="px-4 py-3">
                  SKU
                </th>

                <th className="px-4 py-3">
                  Màu
                </th>

                <th className="px-4 py-3">
                  Size
                </th>

                <th className="px-4 py-3 text-center">
                  Số lượng
                </th>

                {!isView && (
                  <th className="px-4 py-3 text-center">
                    Thao tác
                  </th>
                )}
              </tr>
            </thead>

            <tbody>
              {items.map((item) => (
                <tr
                  key={item.variantId}
                  className="border-b border-zinc-800"
                >
                  {/* PRODUCT */}

                  <td className="px-4 py-4">
                    <div className="flex items-center gap-3">
                      <img
                        src={item.image}
                        alt={item.name}
                        className="h-14 w-14 rounded-lg border border-zinc-700 object-cover"
                      />

                      <span className="font-medium text-white">
                        {item.name}
                      </span>
                    </div>
                  </td>

                  {/* SKU */}

                  <td className="px-4 py-4 text-zinc-300">
                    {item.sku}
                  </td>

                  {/* COLOR */}

                  <td className="px-4 py-4 text-zinc-300">
                    {item.color}
                  </td>

                  {/* SIZE */}

                  <td className="px-4 py-4 text-zinc-300">
                    {item.size}
                  </td>

                  {/* QUANTITY */}

                  <td className="px-4 py-4">
                    {isView ? (
                      <div className="text-center text-white">
                        {item.quantity}
                      </div>
                    ) : (
                      <input
                        type="number"
                        min={1}
                        value={item.quantity}
                        onChange={(e) =>
                          onUpdateItem(
                            item.variantId,
                            "quantity",
                            Number(
                              e.target.value
                            )
                          )
                        }
                        className="w-24 rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2 text-center text-white focus:border-orange-500 focus:outline-none"
                      />
                    )}
                  </td>

                  {/* ACTION */}

                  {!isView && (
                    <td className="px-4 py-4 text-center">
                      <button
                        onClick={() =>
                          onRemoveItem(
                            item.variantId
                          )
                        }
                        className="rounded-lg p-2 text-red-400 hover:bg-red-500/10"
                      >
                        <Trash2 size={18} />
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* ================= SUMMARY ================= */}

      <div className="mt-6 flex justify-end border-t border-zinc-700 pt-4">
        <div className="rounded-xl bg-zinc-900 px-6 py-4">
          <div className="flex items-center gap-10">
            <span className="text-zinc-400">
              Tổng số lượng:
            </span>

            <span className="text-xl font-bold text-orange-400">
              {summary.totalQuantity}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}