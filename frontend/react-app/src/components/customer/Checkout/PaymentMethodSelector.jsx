import { Wallet, CreditCard, Landmark, Smartphone } from "lucide-react";

export default function PaymentMethodSelector({ value, onChange }) {
  return (
    <div className="bg-zinc-900 rounded-2xl p-6 space-y-4">
      <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
        <Landmark size={26} />
        <span>Thanh toán</span>
      </h2>

      <div className="space-y-3">
        {/* COD */}
        <label
          className={`flex items-center gap-3 p-4 rounded-xl border cursor-pointer
            ${
              value === "COD"
                ? "border-blue-500 bg-blue-600/20"
                : "border-zinc-700 hover:border-zinc-500"
            }`}
        >
          <input
            type="radio"
            checked={value === "COD"}
            onChange={() => onChange("COD")}
            className="hidden"
          />
          <Wallet />
          <span>Thanh toán khi nhận hàng (COD)</span>
        </label>

        {/* VNPAY */}
        <label
          className={`flex items-center gap-3 p-4 rounded-xl border cursor-pointer
            ${
              value === "VNPAY"
                ? "border-red-500 bg-red-600/20"
                : "border-zinc-700 hover:border-zinc-500"
            }`}
        >
          <input
            type="radio"
            checked={value === "VNPAY"}
            onChange={() => onChange("VNPAY")}
            className="hidden"
          />
          <CreditCard />
          <span>Thanh toán qua VNPAY</span>
        </label>

        {/* MOMO */}
        <label
          className={`flex items-center gap-3 p-4 rounded-xl border cursor-pointer
            ${
              value === "MOMO"
                ? "border-pink-500 bg-pink-600/20"
                : "border-zinc-700 hover:border-zinc-500"
            }`}
        >
          <input
            type="radio"
            checked={value === "MOMO"}
            onChange={() => onChange("MOMO")}
            className="hidden"
          />
          <Smartphone />
          <span>Thanh toán qua MoMo</span>
        </label>
      </div>
    </div>
  );
}