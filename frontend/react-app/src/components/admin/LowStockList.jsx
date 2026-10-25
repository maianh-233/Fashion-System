import { TriangleAlert } from "lucide-react";
import { variantLabel } from "../../pages/admin/dashboardLogic";

/** Biến thể còn 1–5 sản phẩm (tồn quầy + tồn online); {@code showStore} khi xem toàn hệ thống. */
export default function LowStockList({ items = [], showStore = false }) {
  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <h3 className="font-semibold mb-4 flex items-center gap-2">
        <TriangleAlert className="text-amber-400" size={18} />
        Sản phẩm sắp hết hàng
      </h3>

      {items.length === 0 ? (
        <p className="text-sm text-zinc-400">Chưa có sản phẩm nào sắp hết hàng.</p>
      ) : (
        <div className="space-y-4">
          {items.map((item) => (
            <div
              key={`${item.storeId}-${item.productVariantId}`}
              className="flex justify-between items-center gap-3 bg-zinc-800/50 p-3 rounded-2xl"
            >
              <div className="min-w-0">
                <p className="font-medium">{variantLabel(item)}</p>

                <p className="text-xs text-zinc-500 break-all">
                  Còn {item.quantity} sản phẩm{item.sku ? ` · ${item.sku}` : ""}
                  {showStore ? ` · ${item.storeName}` : ""}
                </p>
              </div>

              <span className="shrink-0 whitespace-nowrap text-amber-400 text-sm font-medium">
                Cảnh báo
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
