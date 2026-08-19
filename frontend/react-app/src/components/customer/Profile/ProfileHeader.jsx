import Button from "../../common/Button";
import { Edit3 } from "lucide-react";

export default function ProfileHeader({ user, onEdit }) {
  return (
    <div className="bg-gradient-to-br from-amber-400 via-yellow-500 to-orange-500 pt-10 pb-8 px-4 text-center relative sm:pt-12 sm:pb-10 sm:px-6">
      <Button
        onClick={onEdit}
        className="absolute top-4 right-4 flex h-11 w-11 items-center justify-center bg-white/30 hover:bg-white/40 rounded-2xl sm:top-6 sm:right-6"
        aria-label="Chỉnh sửa thông tin cá nhân"
      >
        <Edit3 />
      </Button>

      <img
        src={`https://i.pravatar.cc/150?u=${user.email}`}
        alt={`Ảnh đại diện của ${user.name}`}
        className="w-24 h-24 mx-auto rounded-3xl border-4 border-zinc-900 object-cover sm:w-32 sm:h-32"
      />

      <h1 className="text-2xl font-bold mt-4 text-zinc-900 sm:text-4xl sm:mt-5">
        {user.name}
      </h1>
      <p className="text-zinc-800">Khách hàng thân thiết</p>
    </div>
  );
}
