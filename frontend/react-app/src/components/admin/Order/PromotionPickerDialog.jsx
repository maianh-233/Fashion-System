import { useMemo, useState } from "react";
import {
  X,
  Search,
  Gift,
  Check,
} from "lucide-react";

export default function PromotionPickerDialog({
  open,
  promotions = [],
  selectedPromotions = [],
  onClose,
  onAdd,
}) {
  const [keyword, setKeyword] = useState("");
  const [selectedPromotion, setSelectedPromotion] =
    useState(null);

  const filteredPromotions = useMemo(() => {
    const selectedIds = selectedPromotions.map(
      (p) => p.id
    );

    return promotions.filter(
      (p) =>
        !selectedIds.includes(p.id) &&
        (
          p.name
            .toLowerCase()
            .includes(keyword.toLowerCase()) ||
          p.promotionCode
            .toLowerCase()
            .includes(keyword.toLowerCase())
        )
    );
  }, [keyword, promotions, selectedPromotions]);

  if (!open) return null;

  const formatMoney = (value) =>
    Number(value).toLocaleString("vi-VN") + " ₫";

  const handleAdd = () => {
    if (!selectedPromotion) return;

    onAdd?.(selectedPromotion);

    setSelectedPromotion(null);
    setKeyword("");

    onClose();
  };

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/70 backdrop-blur-sm">

      <div className="w-full max-w-4xl rounded-2xl bg-[#1a1a1a] border border-zinc-700 overflow-hidden">

        {/* HEADER */}

        <div className="flex items-center justify-between border-b border-zinc-700 px-6 py-4">

          <div>
            <h2 className="text-xl font-bold text-orange-400">
              Chọn Promotion
            </h2>

            <p className="text-sm text-zinc-400">
              Chọn chương trình giảm giá áp dụng
            </p>
          </div>

          <button
            onClick={onClose}
            className="rounded-lg p-2 hover:bg-zinc-800"
          >
            <X className="text-white" />
          </button>
        </div>

        {/* SEARCH */}

        <div className="p-5">

          <div className="relative">

            <Search
              size={18}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500"
            />

            <input
              value={keyword}
              onChange={(e) =>
                setKeyword(e.target.value)
              }
              placeholder="Tìm promotion..."
              className="w-full rounded-lg border border-zinc-700 bg-zinc-900 py-2 pl-10 pr-4 text-white outline-none focus:ring-2 focus:ring-orange-500"
            />

          </div>

        </div>

        {/* LIST */}

        <div className="max-h-[500px] overflow-y-auto px-5 pb-5 space-y-3">

          {filteredPromotions.length === 0 && (
            <div className="py-16 text-center text-zinc-500">
              Không có promotion phù hợp.
            </div>
          )}

          {filteredPromotions.map((promotion) => (
            <button
              key={promotion.id}
              onClick={() =>
                setSelectedPromotion(promotion)
              }
              className={`w-full rounded-xl border p-4 text-left transition ${
                selectedPromotion?.id === promotion.id
                  ? "border-orange-500 bg-orange-500/10"
                  : "border-zinc-700 hover:border-orange-400"
              }`}
            >
              <div className="flex justify-between">

                <div>

                  <div className="flex items-center gap-2">

                    <Gift
                      size={18}
                      className="text-orange-400"
                    />

                    <span className="font-semibold text-white">
                      {promotion.promotionCode}
                    </span>

                  </div>

                  <p className="mt-2 text-zinc-300">
                    {promotion.name}
                  </p>

                  <p className="mt-1 text-sm text-zinc-500">
                    Đơn tối thiểu{" "}
                    {formatMoney(
                      promotion.minOrderValue
                    )}
                  </p>

                </div>

                <div className="text-right">

                  {selectedPromotion?.id ===
                    promotion.id && (
                    <Check
                      className="ml-auto text-orange-400"
                      size={20}
                    />
                  )}

                  <div className="mt-4 text-lg font-bold text-green-400">
                    - {formatMoney(promotion.discountAmount)}
                  </div>

                </div>

              </div>

            </button>
          ))}

        </div>

        {/* FOOTER */}

        <div className="flex justify-end gap-3 border-t border-zinc-700 px-6 py-4">

          <button
            onClick={onClose}
            className="rounded-lg border border-zinc-700 px-5 py-2 text-white hover:bg-zinc-800"
          >
            Hủy
          </button>

          <button
            disabled={!selectedPromotion}
            onClick={handleAdd}
            className="rounded-lg bg-orange-500 px-6 py-2 text-white hover:bg-orange-600 disabled:opacity-40"
          >
            Áp dụng
          </button>

        </div>

      </div>

    </div>
  );
}