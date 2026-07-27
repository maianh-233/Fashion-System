const tiers = [
    "Bronze",
    "Silver",
    "Gold",
    "Platinum"
];

export default function PromotionCustomerTier({
    mode,
    promotion
}){

const isView = mode==="view";

return(

<section>

<h3 className="text-lg font-semibold text-orange-400 mb-4">
Hạng khách hàng áp dụng
</h3>

<select
disabled={isView}
defaultValue={promotion?.tier}
className="w-full p-2 bg-[#1e1e1e] rounded"
>

<option value="">
Tất cả khách hàng
</option>

{
tiers.map(item=>(
<option
key={item}
value={item}
>
{item}
</option>
))
}

</select>

</section>

)

}