export default function PromotionBasicInfo({
    mode,
    promotion
}) {

    const isView = mode === "view";

    return (

<section>

<h3 className="text-lg font-semibold text-orange-400 mb-4">
Thông tin cơ bản
</h3>

<div className="grid grid-cols-2 gap-4">

<div>
<label>Mã Promotion</label>

<input
disabled={isView}
defaultValue={promotion?.code}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
/>

</div>

<div>
<label>Tên Promotion</label>

<input
disabled={isView}
defaultValue={promotion?.name}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
/>

</div>

<div>

<label>Loại giảm giá</label>

<select
disabled={isView}
defaultValue={promotion?.discount_type}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
>

<option value="PERCENT">
Theo %

</option>

<option value="FIXED">
Theo số tiền

</option>

</select>

</div>

<div>

<label>Giá trị giảm</label>

<input
type="number"
disabled={isView}
defaultValue={promotion?.discount_value}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
/>

</div>

</div>

</section>

    );

}