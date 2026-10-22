import { AtSign, Cake, Mail, Phone, UserRound } from "lucide-react";

const GENDER_LABELS = { MALE: "Nam", FEMALE: "Nữ", OTHER: "Khác" };

const formatBirthDate = (value) => {
  if (!value) return "Chưa cập nhật";
  return new Date(`${value}T00:00:00`).toLocaleDateString("vi-VN");
};

export default function InfoSection({ profile }) {
  const items = [
    { icon: AtSign, label: "Tên đăng nhập", value: profile.username },
    { icon: Mail, label: "Email", value: profile.email },
    { icon: Phone, label: "Số điện thoại", value: profile.phone },
    { icon: Cake, label: "Ngày sinh", value: formatBirthDate(profile.dateOfBirth) },
    { icon: UserRound, label: "Giới tính", value: GENDER_LABELS[profile.gender] },
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
