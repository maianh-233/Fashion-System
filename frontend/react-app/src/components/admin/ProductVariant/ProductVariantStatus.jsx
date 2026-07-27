export default function ProductVariantStatus({
  mode,
  variant,
}) {

  const isView = mode === "view";

  return (

    <section>

      <h3 className="text-lg font-semibold text-orange-400 mb-4">
        Trạng thái
      </h3>

      <div>

        <label>Đang bán</label>

        <select
          disabled={isView}
          defaultValue={variant?.active ? "true" : "false"}
          className="w-full mt-1 p-2 rounded bg-[#1e1e1e]"
        >
          <option value="true">Đang bán</option>
          <option value="false">Ngừng bán</option>
        </select>

      </div>

    </section>

  );

}