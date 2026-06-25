export default function UserInfoCard({ employee }) {
  return (
    <div className="col-span-2">
      <label className="block text-sm text-zinc-400 mb-2">
        Người dùng hệ thống
      </label>

      <div className="bg-zinc-800 border border-zinc-700 rounded-2xl p-4 flex items-center gap-4">
        <div className="w-12 h-12 bg-zinc-700 rounded-full flex items-center justify-center text-2xl">
          👤
        </div>

        <div>
          <p className="text-white font-medium">
            {employee?.name || "—"}
          </p>
          <p className="text-orange-400 text-sm">
            {employee?.email} • {employee?.phone}
          </p>
        </div>
      </div>
    </div>
  );
}