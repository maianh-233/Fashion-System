import {Trash} from 'lucide-react';
export default function CartItem({
  item,
  onToggleCheck,
  onChangeQty,
  onRemove,
}) {
  return (
    <div
      className="
        bg-zinc-900 border border-zinc-800
        rounded-2xl
        p-3 sm:p-4
        flex gap-3 sm:gap-4
      "
    >
      {/* CHECKBOX */}
      <input
        type="checkbox"
        checked={item.checked}
        onChange={onToggleCheck}
        className="mt-1 w-4 h-4 sm:w-5 sm:h-5 accent-amber-400"
      />

      {/* IMAGE */}
      <img
        src={item.image}
        className="
          w-20 h-20
          sm:w-28 sm:h-28
          rounded-xl
          object-cover
          shrink-0
        "
      />

      {/* CONTENT */}
      <div className="flex-1 flex flex-col">
        {/* TOP */}
        <div className="flex justify-between gap-2">
          <div>
            <h3 className="text-sm sm:text-lg font-medium leading-tight">
              {item.name}
            </h3>
            <p className="text-zinc-400 text-xs sm:text-sm">
              {item.brand} • {item.color} • {item.size}
            </p>
          </div>

          <button
            onClick={onRemove}
            className="text-red-400 hover:text-red-500 text-sm"
          >
            <Trash size={20}/>
          </button>
        </div>

        {/* BOTTOM */}
        <div className="flex justify-between items-center mt-3 sm:mt-5">
          {/* QTY */}
          <div className="flex border border-zinc-700 rounded-xl overflow-hidden">
            <button
              onClick={() => onChangeQty(-1)}
              className="w-8 h-8 sm:w-10 sm:h-10 hover:bg-zinc-800"
            >
              -
            </button>
            <span className="px-3 sm:px-5 flex items-center text-sm">
              {item.quantity}
            </span>
            <button
              onClick={() => onChangeQty(1)}
              className="w-8 h-8 sm:w-10 sm:h-10 hover:bg-zinc-800"
            >
              +
            </button>
          </div>

          {/* PRICE */}
          <p className="text-amber-400 font-semibold text-sm sm:text-xl">
            {(item.price * item.quantity).toLocaleString("vi-VN")} ₫
          </p>
        </div>
      </div>
    </div>
  );
}