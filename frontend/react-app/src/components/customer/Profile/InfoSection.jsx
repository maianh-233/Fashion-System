import { Mail, Phone, Cake } from "lucide-react";

export default function InfoSection({ user }) {
  const items = [
    { icon: Mail, label: "Email", value: user.email },
    { icon: Phone, label: "Số điện thoại", value: user.phone },
    { icon: Cake, label: "Ngày sinh", value: user.birthDate },
  ];

  return (
    <div className="grid md:grid-cols-2 gap-4">
      {items.map(({ icon: Icon, label, value }) => (
        <div
          key={label}
          className="bg-zinc-800 border border-zinc-700 rounded-2xl p-6 flex gap-5 hover:border-amber-400 transition"
        >
          <Icon className="w-7 h-7 text-amber-400" />
          <div>
            <p className="text-zinc-400 text-sm">{label}</p>
            <p className="text-lg font-medium">{value}</p>
          </div>
        </div>
      ))}
    </div>
  );
}