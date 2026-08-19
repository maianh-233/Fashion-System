import Button from "../../common/Button";
import { Package, Plus, Minus, Trash2 } from "lucide-react";

export default function OrderItemSection({
  mode = "view",
  items = [],
  summary,
  onChange,
  onAddProduct,
}) {
  const isView = mode === "view";

  const formatMoney = (value) =>
    Number(value || 0).toLocaleString("vi-VN") + " ₫";

  const updateQuantity = (index, quantity) => {
    if (isView) return;

    const newQty = Math.max(1, quantity);

    const newItems = [...items];

    newItems[index] = {
      ...newItems[index],
      quantity: newQty,
      total: newQty * newItems[index].price,
    };

    onChange?.(newItems);
  };

  const removeItem = (index) => {
    if (isView) return;

    onChange?.(items.filter((_, i) => i !== index));
  };

  return (
    <section className="rounded-xl border border-zinc-700 bg-[#1b1b1b]">

      {/* HEADER */}

      <div className="flex items-center justify-between border-b border-zinc-700 px-6 py-4">

        <div className="flex items-center gap-3">
          <Package
            size={22}
            className="text-orange-400"
          />

          <div>
            <h3 className="text-lg font-semibold text-orange-400">
              Danh sách sản phẩm
            </h3>

            <p className="text-sm text-zinc-400">
              Các sản phẩm trong đơn hàng
            </p>
          </div>
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

      {/* TABLE */}

      <div className="overflow-x-auto">

        <table className="w-full">

          <thead className="bg-zinc-800 text-sm text-zinc-300">

            <tr>
              <th className="px-4 py-3 text-left">Ảnh</th>
              <th className="px-4 py-3 text-left">Tên sản phẩm</th>
              <th className="px-4 py-3 text-center">Màu</th>
              <th className="px-4 py-3 text-center">Size</th>
              <th className="px-4 py-3 text-center">SKU</th>
              <th className="px-4 py-3 text-center">SL</th>
              <th className="px-4 py-3 text-right">Đơn giá</th>
              <th className="px-4 py-3 text-right">Thành tiền</th>

              {!isView && (
                <th className="px-4 py-3 text-center">
                  Xóa
                </th>
              )}
            </tr>

          </thead>

          <tbody>

            {items.length === 0 && (

              <tr>

                <td
                  colSpan={isView ? 8 : 9}
                  className="py-12 text-center text-zinc-500"
                >
                  Chưa có sản phẩm
                </td>

              </tr>

            )}

            {items.map((item, index) => (

              <tr
                key={index}
                className="border-t border-zinc-700"
              >

                <td className="px-4 py-4">

                  <img
                    src={
                      item.image ||
                      "https://placehold.co/70x70"
                    }
                    className="w-16 h-16 rounded-lg object-cover border border-zinc-700"
                  />

                </td>

                <td className="px-4 py-4">

                  <div className="font-medium text-white">
                    {item.name}
                  </div>

                  <div className="text-xs text-zinc-500 mt-1">
                    {item.productId}
                  </div>

                </td>

                <td className="text-center text-white">
                  {item.color}
                </td>

                <td className="text-center text-white">
                  {item.size}
                </td>

                <td className="text-center text-zinc-300">
                  {item.sku}
                </td>

                <td className="text-center">

                  {isView ? (

                    <span className="text-white">
                      {item.quantity}
                    </span>

                  ) : (

                    <div className="flex items-center justify-center gap-2">

                      <Button
                        onClick={() =>
                          updateQuantity(
                            index,
                            item.quantity - 1
                          )
                        }
                        className="rounded border border-zinc-700 p-1 hover:bg-zinc-700"
                      >
                        <Minus
                          size={14}
                          className="text-white"
                        />
                      </Button>

                      <span className="w-8 text-center text-white">
                        {item.quantity}
                      </span>

                      <Button
                        onClick={() =>
                          updateQuantity(
                            index,
                            item.quantity + 1
                          )
                        }
                        className="rounded border border-zinc-700 p-1 hover:bg-zinc-700"
                      >
                        <Plus
                          size={14}
                          className="text-white"
                        />
                      </Button>

                    </div>

                  )}

                </td>

                <td className="text-right px-4 text-white">
                  {formatMoney(item.price)}
                </td>

                <td className="text-right px-4 font-semibold text-orange-400">
                  {formatMoney(item.total)}
                </td>

                {!isView && (

                  <td className="text-center">

                    <Button
                      onClick={() =>
                        removeItem(index)
                      }
                      className="rounded-lg p-2 text-red-400 hover:bg-red-500/10"
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

      {/* SUMMARY */}

      <div className="border-t border-zinc-700 p-6">

        <div className="ml-auto max-w-sm space-y-3">

          <div className="flex justify-between text-zinc-300">
            <span>Tạm tính</span>
            <span>{formatMoney(summary.subtotal)}</span>
          </div>

          <div className="flex justify-between text-zinc-300">
            <span>Giảm giá</span>

            <span className="text-green-400">
              - {formatMoney(summary.discount)}
            </span>
          </div>

          <div className="flex justify-between text-zinc-300">
            <span>Thuế</span>
            <span>{formatMoney(summary.tax)}</span>
          </div>

          <div className="flex justify-between text-zinc-300">
            <span>Phí vận chuyển</span>
            <span>{formatMoney(summary.shippingFee)}</span>
          </div>

          <div className="flex justify-between border-t border-zinc-700 pt-3 text-lg font-bold">

            <span className="text-white">
              Tổng thanh toán
            </span>

            <span className="text-orange-400">
              {formatMoney(summary.total)}
            </span>

          </div>

        </div>

      </div>

    </section>
  );
}