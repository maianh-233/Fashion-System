export default function ProductVariantBasicInfo({ mode, variant }) {

  const isView = mode === "view";

  return (
    <section>

      <h3 className="text-lg font-semibold text-orange-400 mb-4">
        Thông tin biến thể
      </h3>

      <div className="grid grid-cols-2 gap-4">

        <div>
          <label>SKU</label>
          <input
            disabled={isView}
            defaultValue={variant?.sku}
            className="w-full mt-1 p-2 rounded bg-[#1e1e1e]"
          />
        </div>

        <div>
          <label>Barcode</label>
          <input
            disabled={isView}
            defaultValue={variant?.barcode}
            className="w-full mt-1 p-2 rounded bg-[#1e1e1e]"
          />
        </div>

        <div>
          <label>Màu sắc</label>

          <select
            disabled={isView}
            defaultValue={variant?.color}
            className="w-full mt-1 p-2 rounded bg-[#1e1e1e]"
          >
            <option>Đen</option>
            <option>Trắng</option>
            <option>Xanh</option>
            <option>Đỏ</option>
          </select>
        </div>

        <div>
          <label>Size</label>

          <select
            disabled={isView}
            defaultValue={variant?.size}
            className="w-full mt-1 p-2 rounded bg-[#1e1e1e]"
          >
            <option>S</option>
            <option>M</option>
            <option>L</option>
            <option>XL</option>
          </select>

        </div>

      </div>

    </section>
  );

}