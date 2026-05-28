export default function Promotions({ promotions }) {
  return (
    <div className="space-y-3">
      {promotions.map(p => (
        <div key={p.code} className="bg-green-950 border border-green-900 rounded-2xl p-4 flex justify-between">
          <div>
            <p className="font-semibold text-green-400">{p.code}</p>
            <p className="text-sm text-gray-400">{p.name}</p>
          </div>
          <span className="text-green-400">
            -{p.discount.toLocaleString()} ₫
          </span>
        </div>
      ))}
    </div>
  );
}