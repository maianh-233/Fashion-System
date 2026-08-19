import Button from "../../common/Button";
import {
  Package,
  Plus,
  Trash2,
} from "lucide-react";

export default function GoodsReceiptItemSection({
  mode = "view",
  items = [],
  summary,
  onChange,
  onUpdateItem,
  onRemoveItem,
  onAddProduct,
}) {
  const isView = mode === "view";

  const handleQuantityChange = (variantId, value) => {
    const quantity = Math.max(1, Number(value) || 1);

    onUpdateItem?.(
      variantId,
      "quantity",
      quantity
    );
  };

  const handleCostPriceChange = (
    variantId,
    value
  ) => {
    const costPrice =
      Math.max(0, Number(value) || 0);

    onUpdateItem?.(
      variantId,
      "costPrice",
      costPrice
    );
  };

  return (
    <div className="rounded-2xl border border-zinc-700 bg-[#1b1b1b]">
      {/* ================= HEADER ================= */}

      <div className="flex items-center justify-between border-b border-zinc-700 px-6 py-4">
        <div>
          <h3 className="text-lg font-semibold text-orange-400">
            Sản phẩm nhập kho
          </h3>

          <p className="mt-1 text-sm text-zinc-400">
            Danh sách sản phẩm trong phiếu nhập
          </p>
        </div>

        {!isView && (
          <Button
            onClick={onAddProduct}
            className="flex items-center gap-2 rounded-lg bg-orange-500 px-4 py-2 text-white hover:bg-orange-600"
          >
            <Plus size={18} />
            Thêm sản phẩm
          </Button>
        )}
      </div>

      {/* ================= TABLE ================= */}

      <div className="overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-zinc-900">
            <tr className="text-sm text-zinc-400">
              <th className="px-5 py-3 text-left">
                Sản phẩm
              </th>

              <th className="px-5 py-3 text-left">
                SKU
              </th>

              <th className="px-5 py-3 text-center">
                Giá nhập
              </th>

              <th className="px-5 py-3 text-center">
                SL
              </th>

              <th className="px-5 py-3 text-right">
                Thành tiền
              </th>

              {!isView && (
                <th className="w-20"></th>
              )}
            </tr>
          </thead>

          <tbody>
            {items.length === 0 && (
              <tr>
                <td
                  colSpan={
                    isView ? 5 : 6
                  }
                  className="py-16 text-center"
                >
                  <Package
                    size={48}
                    className="mx-auto text-zinc-600"
                  />

                  <p className="mt-4 text-zinc-500">
                    Chưa có sản phẩm
                  </p>
                </td>
              </tr>
            )}

            {items.map((item) => (
              <tr
                key={item.variantId}
                className="border-t border-zinc-800"
              >
                {/* PRODUCT */}

                <td className="px-5 py-4">
                  <div className="flex items-center gap-4">
                    <img
                      src={item.image}
                      alt={item.name}
                      className="h-16 w-16 rounded-lg border border-zinc-700 object-cover"
                    />

                    <div>
                      <div className="font-medium text-white">
                        {item.name}
                      </div>

                      <div className="mt-1 text-sm text-zinc-500">
                        {item.color} / Size{" "}
                        {item.size}
                      </div>
                    </div>
                  </div>
                </td>

                {/* SKU */}

                <td className="px-5 py-4">
                  <span className="rounded bg-zinc-800 px-2 py-1 text-xs text-zinc-300">
                    {item.sku}
                  </span>
                </td>

                {/* COST PRICE */}

                <td className="px-5 py-4 text-center">
                  {isView ? (
                    <span className="font-medium text-orange-400">
                      {Number(
                        item.costPrice
                      ).toLocaleString("vi-VN")}
                      ₫
                    </span>
                  ) : (
                    <input
                      type="number"
                      min={0}
                      value={item.costPrice}
                      onChange={(e) =>
                        handleCostPriceChange(
                          item.variantId,
                          e.target.value
                        )
                      }
                      className="w-32 rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2 text-center text-white outline-none focus:border-orange-500"
                    />
                  )}
                </td>

                {/* QUANTITY */}

                <td className="px-5 py-4 text-center">
                  {isView ? (
                    <span className="font-semibold text-white">
                      {item.quantity}
                    </span>
                  ) : (
                    <input
                      type="number"
                      min={1}
                      value={item.quantity}
                      onChange={(e) =>
                        handleQuantityChange(
                          item.variantId,
                          e.target.value
                        )
                      }
                      className="w-20 rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2 text-center text-white outline-none focus:border-orange-500"
                    />
                  )}
                </td>

                {/* TOTAL */}

                <td className="px-5 py-4 text-right font-semibold text-orange-400">
                  {Number(
                    item.total
                  ).toLocaleString("vi-VN")}
                  ₫
                </td>

                {/* DELETE */}

                {!isView && (
                  <td className="px-5 py-4 text-center">
                    <Button
                      onClick={() =>
                        onRemoveItem?.(
                          item.variantId
                        )
                      }
                      className="rounded-lg p-2 text-red-400 transition hover:bg-red-500/10"
                    >
                      <Trash2 size={18} />
                    </Button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* ======= PHẦN 4B sẽ bắt đầu từ đây ======= */}
            {/* ================= FOOTER ================= */}

      <div className="border-t border-zinc-700 bg-[#181818] px-6 py-5">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          {/* LEFT */}

          <div className="text-sm text-zinc-400">
            {items.length > 0 ? (
              <>
                Có{" "}
                <span className="font-semibold text-orange-400">
                  {items.length}
                </span>{" "}
                sản phẩm trong phiếu nhập
              </>
            ) : (
              "Chưa có sản phẩm nào được thêm"
            )}
          </div>

          {/* RIGHT */}

          <div className="grid grid-cols-2 gap-4 lg:w-[420px]">
            {/* TOTAL QUANTITY */}

            <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-4">
              <div className="text-sm text-zinc-400">
                Tổng số lượng
              </div>

              <div className="mt-2 text-2xl font-bold text-orange-400">
                {summary?.totalQuantity ?? 0}
              </div>
            </div>

            {/* TOTAL AMOUNT */}

            <div className="rounded-xl border border-zinc-700 bg-zinc-900 p-4">
              <div className="text-sm text-zinc-400">
                Tổng tiền nhập
              </div>

              <div className="mt-2 text-xl font-bold text-orange-400">
                {Number(
                  summary?.totalAmount ?? 0
                ).toLocaleString("vi-VN")}
                ₫
              </div>
            </div>
          </div>
        </div>

        {/* QUICK SUMMARY */}

        {items.length > 0 && (
          <div className="mt-6 rounded-xl border border-orange-500/20 bg-orange-500/5 p-4">
            <div className="flex flex-col gap-3 md:flex-row md:justify-between">
              <div>
                <div className="text-sm text-zinc-400">
                  Số mặt hàng
                </div>

                <div className="mt-1 text-lg font-semibold text-white">
                  {items.length}
                </div>
              </div>

              <div>
                <div className="text-sm text-zinc-400">
                  Tổng số lượng nhập
                </div>

                <div className="mt-1 text-lg font-semibold text-white">
                  {summary?.totalQuantity ?? 0}
                </div>
              </div>

              <div>
                <div className="text-sm text-zinc-400">
                  Giá trị phiếu nhập
                </div>

                <div className="mt-1 text-xl font-bold text-orange-400">
                  {Number(
                    summary?.totalAmount ?? 0
                  ).toLocaleString("vi-VN")}
                  ₫
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}