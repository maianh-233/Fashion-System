import { Gift, Plus, Trash2 } from "lucide-react";

export default function OrderPromotionSection({
  mode = "view",
  promotions = [],
  onChange,
  onAddPromotion,
}) {
  const isView = mode === "view";

  const handleRemove = (index) => {
    if (isView) return;

    const newPromotions = promotions.filter((_, i) => i !== index);

    onChange?.(newPromotions);
  };

  const formatMoney = (value) =>
    Number(value || 0).toLocaleString("vi-VN") + " ₫";

  const totalDiscount = promotions.reduce(
    (sum, item) => sum + Number(item.discountAmount || 0),
    0
  );

  return (
    <section className="rounded-xl border border-zinc-700 bg-[#1b1b1b]">

      {/* Header */}

      <div className="flex items-center justify-between border-b border-zinc-700 px-6 py-4">

        <div className="flex items-center gap-3">

          <Gift
            size={22}
            className="text-orange-400"
          />

          <div>

            <h3 className="text-lg font-semibold text-orange-400">
              Khuyến mãi
            </h3>

            <p className="text-sm text-zinc-400">
              Danh sách promotion áp dụng cho đơn hàng
            </p>

          </div>

        </div>

        {!isView && (
          <button
            onClick={onAddPromotion}
            className="flex items-center gap-2 rounded-lg bg-orange-500 px-4 py-2 text-white hover:bg-orange-600"
          >
            <Plus size={18} />
            Thêm Promotion
          </button>
        )}

      </div>

      {/* Body */}

      <div className="overflow-x-auto">

        <table className="min-w-full">

          <thead className="bg-zinc-800">

            <tr className="text-zinc-300 text-sm">

              <th className="px-4 py-3 text-left">
                Mã
              </th>

              <th className="px-4 py-3 text-left">
                Tên chương trình
              </th>

              <th className="px-4 py-3 text-right">
                Giảm
              </th>

              {!isView && (
                <th className="px-4 py-3 text-center">
                  Thao tác
                </th>
              )}

            </tr>

          </thead>

          <tbody>

            {promotions.length === 0 && (

              <tr>

                <td
                  colSpan={isView ? 3 : 4}
                  className="py-10 text-center text-zinc-500"
                >
                  Chưa áp dụng promotion.
                </td>

              </tr>

            )}

            {promotions.map((promotion, index) => (

              <tr
                key={promotion.id || index}
                className="border-t border-zinc-700"
              >

                <td className="px-4 py-4 text-white">
                  {promotion.promotionCode}
                </td>

                <td className="px-4 py-4 text-zinc-300">
                  {promotion.name || "--"}
                </td>

                <td className="px-4 py-4 text-right font-semibold text-green-400">
                  - {formatMoney(promotion.discountAmount)}
                </td>

                {!isView && (

                  <td className="px-4 py-4 text-center">

                    <button
                      onClick={() => handleRemove(index)}
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

      {/* Footer */}

      <div className="border-t border-zinc-700 px-6 py-4">

        <div className="flex justify-between">

          <span className="text-zinc-400">
            Tổng giảm giá
          </span>

          <span className="font-bold text-green-400 text-lg">
            - {formatMoney(totalDiscount)}
          </span>

        </div>

      </div>

    </section>
  );
}