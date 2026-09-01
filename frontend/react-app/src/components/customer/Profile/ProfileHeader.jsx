import Button from "../../common/Button";
import { Crown, Edit3, PackageCheck, Sparkles } from "lucide-react";

export default function ProfileHeader({ user, onEdit }) {
  return (
    <section className="customer-profile-hero">
      <div className="customer-profile-hero__portrait">
        <img src={`https://i.pravatar.cc/240?u=${user.email}`} alt={`Ảnh đại diện của ${user.name}`} />
        <span><Sparkles size={12} /></span>
      </div>

      <div className="customer-profile-hero__copy">
        <p>Member profile</p>
        <h2>{user.name}</h2>
        <div className="customer-profile-hero__tier"><Crown size={13} /> Thành viên Privilege</div>
        <span>Đồng hành cùng Lunaria từ 2024</span>
      </div>

      <div className="customer-profile-hero__benefit">
        <PackageCheck size={17} />
        <span><small>Quyền lợi hiện tại</small><strong>Ưu tiên hỗ trợ & giao hàng</strong></span>
      </div>

      <Button type="button" variant="unstyled" onClick={onEdit} className="customer-profile-edit">
        <Edit3 size={14} /> Chỉnh sửa hồ sơ
      </Button>
    </section>
  );
}
