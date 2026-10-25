import { compactMoney, count } from "../../pages/admin/dashboardLogic";

/** Sản phẩm bán chạy trong tháng, tính trên đơn hoàn thành. */
export default function BestSellingProducts({ products = [] }) {
  return (
    <div className="bg-zinc-900 rounded-3xl p-6 border border-zinc-800">
      <h3 className="font-semibold mb-4">
        Top sản phẩm bán chạy tháng này
      </h3>

      {products.length === 0 ? (
        <p className="text-sm text-zinc-400">Chưa có đơn hoàn thành trong tháng.</p>
      ) : (
        <div className="space-y-5">
          {products.map((product, index) => (
            <div
              key={product.productId || product.productName}
              className="flex items-center gap-4"
            >
              {product.imageUrl
                ? <img src={product.imageUrl} alt="" className="w-12 h-12 rounded-2xl object-cover bg-zinc-700" />
                : <div className="w-12 h-12 bg-zinc-700 rounded-2xl"></div>}

              <div className="flex-1">
                <p className="font-medium">
                  {product.productName}
                </p>

                <p className="text-sm text-zinc-400">
                  {count(product.quantity)} đã bán • {compactMoney(product.revenue)} đ
                </p>
              </div>

              <span className="text-emerald-400 font-semibold">
                #{index + 1}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
