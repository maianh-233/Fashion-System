import { Edit3 } from "lucide-react";

export default function ProfileHeader({ user, onEdit }) {
  return (
    <div className="bg-gradient-to-br from-amber-400 via-yellow-500 to-orange-500 pt-12 pb-10 px-6 text-center relative">
      <button
        onClick={onEdit}
        className="absolute top-6 right-6 bg-white/30 hover:bg-white/40 p-3 rounded-2xl"
      >
        <Edit3 />
      </button>

      <img
        src={`https://i.pravatar.cc/150?u=${user.email}`}
        className="w-32 h-32 mx-auto rounded-3xl border-4 border-zinc-900 object-cover"
      />

      <h1 className="text-4xl font-bold mt-5 text-zinc-900">
        {user.name}
      </h1>
      <p className="text-zinc-800">Khách hàng thân thiết</p>
    </div>
  );
}