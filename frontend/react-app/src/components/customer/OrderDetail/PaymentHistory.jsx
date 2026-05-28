const STATUS_COLOR = {
  PAID: "text-green-400",
  PENDING: "text-yellow-400",
  FAILED: "text-red-400",
  REFUNDED: "text-blue-400",
};

export default function PaymentHistory({ payments }) {
  return (
    <div className="bg-zinc-900 rounded-2xl p-6">
      <h2 className="text-xl font-semibold mb-4">
        Lịch sử thanh toán
      </h2>

      <div className="space-y-4">
        {payments.map(p => (
          <div
            key={p.id}
            className="border border-zinc-800 rounded-xl p-4"
          >
            <div className="flex justify-between">
              <span className="font-medium">
                {p.payment_code}
              </span>
              <span className={STATUS_COLOR[p.status]}>
                {p.status}
              </span>
            </div>

            <div className="text-sm text-zinc-400 mt-1">
              <p>Phương thức: {p.method}</p>
              <p>Số tiền: {p.amount.toLocaleString()} ₫</p>
              {p.paid_at && (
                <p>
                  Thanh toán lúc:{" "}
                  {new Date(p.paid_at).toLocaleString()}
                </p>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}