import Button from "../../common/Button";
import { Trash } from "lucide-react";
export default function CartItem({
  item,
  onToggleCheck,
  onChangeQty,
  onRemove,
}) {
  return (
    <article className="relative flex gap-3 rounded-2xl border border-zinc-800 bg-zinc-900 p-3 sm:gap-4 sm:p-4">
      {/* CHECKBOX */}
      <input
        type="checkbox"
        checked={item.checked}
        onChange={onToggleCheck}
        aria-label={`Chọn ${item.name}`}
        className="mt-1 h-5 w-5 shrink-0 accent-amber-400"
      />

      {/* IMAGE */}
      <img
        src={item.image}
        alt={item.name}
        loading="lazy"
        className="h-20 w-20 shrink-0 rounded-xl object-cover sm:h-28 sm:w-28"
      />

      {/* CONTENT */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* TOP */}
        <div className="flex min-w-0 justify-between gap-2">
          <div className="min-w-0 pr-1">
            <h2 className="line-clamp-2 text-sm font-medium leading-snug sm:text-lg">
              {item.name}
            </h2>
            <p className="mt-1 line-clamp-2 text-xs text-zinc-400 sm:text-sm">
              {item.brand} • {item.color} • {item.size}
            </p>
          </div>

          <Button
            onClick={onRemove}
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-red-400 hover:bg-red-500/10 hover:text-red-300"
            aria-label={`Xóa ${item.name} khỏi giỏ hàng`}
          >
            <Trash size={18} />
          </Button>
        </div>

        {/* BOTTOM */}
        <div className="mt-3 flex flex-col items-start gap-3 sm:mt-auto sm:flex-row sm:items-end sm:justify-between">
          {/* QTY */}
          <div className="flex overflow-hidden rounded-xl border border-zinc-700">
            <Button
              onClick={() => onChangeQty(-1)}
              className="h-10 w-10 hover:bg-zinc-800"
              aria-label={`Giảm số lượng ${item.name}`}
            >
              -
            </Button>
            <span className="flex min-w-10 items-center justify-center px-2 text-sm">
              {item.quantity}
            </span>
            <Button
              onClick={() => onChangeQty(1)}
              className="h-10 w-10 hover:bg-zinc-800"
              aria-label={`Tăng số lượng ${item.name}`}
            >
              +
            </Button>
          </div>

          {/* PRICE */}
          <p className="text-base font-semibold text-amber-400 sm:text-xl">
            {(item.price * item.quantity).toLocaleString("vi-VN")} ₫
          </p>
        </div>
      </div>
    </article>
  );
}
