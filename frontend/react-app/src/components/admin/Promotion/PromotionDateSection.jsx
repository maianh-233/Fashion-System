export default function PromotionDateSection({
    mode,
    promotion
}){

const isView = mode==="view";

return(

<section>

<h3 className="text-lg font-semibold text-orange-400 mb-4">
Thời gian áp dụng
</h3>

<div className="grid grid-cols-2 gap-4">

<div>

<label>Ngày bắt đầu</label>

<input
type="datetime-local"
disabled={isView}
defaultValue={promotion?.start_date}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
/>

</div>

<div>

<label>Ngày kết thúc</label>

<input
type="datetime-local"
disabled={isView}
defaultValue={promotion?.end_date}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
/>

</div>

</div>

</section>

)

}