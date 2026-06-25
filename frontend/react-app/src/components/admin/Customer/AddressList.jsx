// AddressList.jsx
export default function AddressList({ addresses }) {
  return (
    <div className="mt-12">
      <h3 className="text-lg font-semibold mb-4">📍 Danh sách địa chỉ</h3>

      <div className="space-y-4 max-h-80 overflow-auto">
        {addresses.map((a, i) => (
          <div key={i} className="bg-zinc-800 p-6 rounded-3xl">
            <div className="font-medium">{a.label}</div>
            <div className="text-zinc-400 text-sm mt-1">{a.address}</div>
            {a.isDefault && (
              <div className="text-emerald-400 text-xs mt-2">
                ● Mặc định
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}