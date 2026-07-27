export default function PromotionMetaInfo({
    createdAt,
    updatedAt
}){

return(

<section>

<h3 className="text-lg font-semibold text-orange-400 mb-4">
Thông tin hệ thống
</h3>

<div className="grid grid-cols-2 gap-4">

<div>

<label>Ngày tạo</label>

<input
disabled
value={createdAt}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
/>

</div>

<div>

<label>Cập nhật lần cuối</label>

<input
disabled
value={updatedAt}
className="w-full mt-1 p-2 bg-[#1e1e1e] rounded"
/>

</div>

</div>

</section>

)

}