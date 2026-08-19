import Button from "../../common/Button";
import { useMemo, useState } from "react";
import {
  Search,
  Gift,
  Check,
} from "lucide-react";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogFooter,
  AdminDialogHeader,
} from "../common/AdminDialog";

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
    <AdminDialog open={open} onClose={onClose} size="lg">
        <AdminDialogHeader
          title="Chọn khuyến mãi"
          description="Chọn chương trình giảm giá áp dụng"
          onClose={onClose}
        />
        <AdminDialogBody>

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
            <Button
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

            </Button>
          ))}

        </div>

        {/* FOOTER */}

        </AdminDialogBody>
        <AdminDialogFooter>

          <Button
            onClick={onClose}
            className="rounded-lg border border-zinc-700 px-5 py-2 text-white hover:bg-zinc-800"
          >
            Hủy
          </Button>

          <Button
            disabled={!selectedPromotion}
            onClick={handleAdd}
            className="rounded-lg bg-orange-500 px-6 py-2 text-white hover:bg-orange-600 disabled:opacity-40"
          >
            Áp dụng
          </Button>

        </AdminDialogFooter>
    </AdminDialog>
  );
}
