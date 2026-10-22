import { Link } from "react-router-dom";
import Button from "../../common/Button";
import { Crown, Edit3, KeyRound, Sparkles, Star } from "lucide-react";
import { productImage } from "../../../pages/customer/storefrontLogic";

/** Đầu trang hồ sơ: tên, hạng thành viên và điểm tích lũy thật. */
export default function ProfileHeader({ profile, onEdit, onChangePassword }) {
  const since = profile.createdAt ? new Date(profile.createdAt).getFullYear() : null;
  return (
    <section className="customer-profile-hero">
      <div className="customer-profile-hero__portrait">
        <img src={profile.avatar || productImage(null, profile.fullName)} alt={`Ảnh đại diện của ${profile.fullName}`} />
        <span><Sparkles size={12} /></span>
      </div>

      <div className="customer-profile-hero__copy">
        <p>Member profile · {profile.customerCode}</p>
        <h2>{profile.fullName}</h2>
        <div className="customer-profile-hero__tier"><Crown size={13} /> Hạng {profile.tierName || "thành viên"}</div>
        {since && <span>Đồng hành cùng Lunaria từ {since}</span>}
      </div>

      <div className="customer-profile-hero__benefit">
        <Star size={17} />
        <span><small>Điểm tích lũy</small><strong>{Number(profile.points || 0).toLocaleString("vi-VN")} điểm</strong>
          <Link to="/vouchers" className="customer-profile-hero__redeem">Đổi điểm lấy voucher</Link></span>
      </div>

      <div className="customer-profile-hero__actions">
        <Button type="button" variant="unstyled" onClick={onEdit} className="customer-profile-edit">
          <Edit3 size={14} /> Chỉnh sửa hồ sơ
        </Button>
        <Button type="button" variant="unstyled" onClick={onChangePassword} className="customer-profile-edit">
          <KeyRound size={14} /> {profile.hasPassword ? "Đổi mật khẩu" : "Đặt mật khẩu"}
        </Button>
      </div>
    </section>
  );
}
