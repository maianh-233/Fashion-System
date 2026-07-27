export default function PromotionLimitSection({
    mode,
    promotion
}){

const isView = mode==="view";

return(

<section>

<h3 className="text-lg font-semibold text-orange-400 mb-4">
Điều kiện áp dụng
</h3>

<div className="grid grid-cols-2 gap-4">

<div>

<label>Đơn tối thiểu</label>

<input
type="number"
disabled={isView}
defaultValue={promotion?.min_order_value}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
/>

</div>

<div>

<label>Giảm tối đa</label>

<input
type="number"
disabled={isView}
defaultValue={promotion?.max_discount}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
/>

</div>

<div>

<label>Giới hạn lượt dùng</label>

<input
type="number"
disabled={isView}
defaultValue={promotion?.usage_limit}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
/>

</div>

<div>

<label>Lượt dùng / người</label>

<input
type="number"
disabled={isView}
defaultValue={promotion?.usage_per_user}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
/>

</div>

</div>

</section>

)

}