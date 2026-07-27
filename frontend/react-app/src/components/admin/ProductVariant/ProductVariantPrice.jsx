export default function ProductVariantPrice({
  mode,
  variant,
}) {

  const isView = mode === "view";

  return (

    <section>

      <h3 className="text-lg font-semibold text-orange-400 mb-4">
        Giá & Trọng lượng
      </h3>

      <div className="grid grid-cols-2 gap-4">

        <div>
          <label>Giá gốc</label>

          <input
            type="number"
            disabled={isView}
            defaultValue={variant?.price}
            className="w-full mt-1 p-2 rounded bg-[#1e1e1e]"
          />
        </div>

        <div>
          <label>Giá khuyến mãi</label>

          <input
            type="number"
            disabled={isView}
            defaultValue={variant?.sale_price}
            className="w-full mt-1 p-2 rounded bg-[#1e1e1e]"
          />
        </div>

        <div>
          <label>Trọng lượng (gram)</label>

          <input
            type="number"
            disabled={isView}
            defaultValue={variant?.weight}
            className="w-full mt-1 p-2 rounded bg-[#1e1e1e]"
          />
        </div>

      </div>

    </section>

  );

}