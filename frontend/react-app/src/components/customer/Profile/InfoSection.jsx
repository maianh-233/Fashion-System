import { Cake, Mail, Phone } from "lucide-react";

const formatBirthDate = (value) => {
  if (!value) return "Chưa cập nhật";
  return new Date(`${value}T00:00:00`).toLocaleDateString("vi-VN");
};

export default function InfoSection({ user }) {
  const items = [
    { icon: Mail, label: "Email", value: user.email },
    { icon: Phone, label: "Số điện thoại", value: user.phone },
    { icon: Cake, label: "Ngày sinh", value: formatBirthDate(user.birthDate) },
  ];

  return (
    <div className="customer-profile-info-grid">
      {items.map(({ icon: Icon, label, value }) => (
        <article key={label}>
          <span><Icon size={16} /></span>
          <div>
            <small>{label}</small>
            <strong>{value || "Chưa cập nhật"}</strong>
          </div>
        </article>
      ))}
    </div>
  );
}
