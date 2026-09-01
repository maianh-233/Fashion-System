import { useState, useCallback } from "react";
import { MapPin, ShieldCheck, Sparkles, UserRound } from "lucide-react";
import ProfileHeader from "../../components/customer/Profile/ProfileHeader";
import InfoSection from "../../components/customer/Profile/InfoSection";
import AddressList from "../../components/customer/Profile/AddressList";
import EditProfileModal from "../../components/customer/Profile/EditProfileModal";
import AddAddressModal from "../../components/customer/Profile/AddAddressModal";

const USER_INIT = {
  id: "user-1",
  name: "Nguyễn Văn A",
  email: "nguyenvana@example.com",
  phone: "0123 456 789",
  birthDate: "1995-06-15",
};

const ADDRESS_INIT = [
 {
    id: "addr-1",
    user_id: "user-1",
    receiver_name: "Nguyễn Văn A",
    receiver_phone: "0123 456 789",
    province: "TP. Hồ Chí Minh",
    district: "Quận 1",
    ward: "Phường Bến Nghé",
    address_line: "123 Nguyễn Thị Minh Khai",
    postal_code: "700000",
    is_default: true,
    address_type: "HOME",
    created_at: "2024-05-01T08:30:00",
    updated_at: "2024-05-01T08:30:00",
  },
  {
    id: "addr-2",
    user_id: "user-1",
    receiver_name: "Nguyễn Văn A",
    receiver_phone: "0987 654 321",
    province: "TP. Hồ Chí Minh",
    district: "Quận Bình Thạnh",
    ward: "Phường 25",
    address_line: "Landmark 81, Vinhomes Central Park",
    postal_code: "700000",
    is_default: false,
    address_type: "WORK",
    created_at: "2024-05-10T09:00:00",
    updated_at: "2024-05-10T09:00:00",
  },
  {
    id: "addr-3",
    user_id: "user-1",
    receiver_name: "Nguyễn Văn A",
    receiver_phone: "0909 111 222",
    province: "Đà Nẵng",
    district: "Hải Châu",
    ward: "Phường Thạch Thang",
    address_line: "45 Trần Phú",
    postal_code: "550000",
    is_default: false,
    address_type: "OTHER",
    created_at: "2024-06-01T14:20:00",
    updated_at: "2024-06-01T14:20:00",
  },
];

export default function CustomerInfoPage() {
  const [user, setUser] = useState(USER_INIT);
  const [addresses, setAddresses] = useState(ADDRESS_INIT);

  const [editOpen, setEditOpen] = useState(false);
  const [addOpen, setAddOpen] = useState(false);

  const deleteAddress = useCallback(
    (id) => setAddresses((prev) => prev.filter((a) => a.id !== id)),
    []
  );

  const addAddress = (addr) => {
    setAddresses((prev) => [...prev, addr]);
    setAddOpen(false);
  };

  const setDefaultAddress = useCallback((id) => {
    setAddresses((prev) =>
      prev.map((address) => ({
        ...address,
        is_default: address.id === id,
      })),
    );
  }, []);

  return (
    <>
      <div className="customer-page customer-profile-page">
        <div className="customer-page__wide customer-profile-page__inner">
          <header className="customer-profile-heading">
            <div>
              <p><Sparkles size={13} /> Personal concierge</p>
              <h1>Hồ sơ của bạn</h1>
              <span>Quản lý thông tin cá nhân và địa chỉ giao nhận trong một không gian riêng tư.</span>
            </div>
            <div className="customer-profile-heading__trust">
              <ShieldCheck size={16} />
              <span><strong>Dữ liệu được bảo vệ</strong><small>Thông tin chỉ dùng cho đơn hàng</small></span>
            </div>
          </header>

          <main className="customer-profile-shell">
          <ProfileHeader user={user} onEdit={() => setEditOpen(true)} />

            <div className="customer-profile-content">
              <section className="customer-profile-panel" aria-labelledby="personal-info-title">
                <div className="customer-profile-section-heading">
                  <span><UserRound size={16} /></span>
                  <div><small>Personal details</small><h2 id="personal-info-title">Thông tin cá nhân</h2></div>
                </div>
                <InfoSection user={user} />
              </section>

              <section className="customer-profile-panel" aria-labelledby="address-list-title">
                <div className="customer-profile-section-heading">
                  <span><MapPin size={16} /></span>
                  <div><small>Delivery book</small><h2 id="address-list-title">Sổ địa chỉ</h2></div>
                  <strong>{addresses.length}</strong>
                </div>
                <AddressList
                  addresses={addresses}
                  onAdd={() => setAddOpen(true)}
                  onDelete={deleteAddress}
                  onSetDefault={setDefaultAddress}
                />
              </section>
            </div>
          </main>
        </div>
      </div>

      {editOpen && (
        <EditProfileModal
          user={user}
          onClose={() => setEditOpen(false)}
          onSave={setUser}
        />
      )}

      {addOpen && (
        <AddAddressModal
          userId={user.id}
          phone={user.phone}
          onClose={() => setAddOpen(false)}
          onAdd={addAddress}
        />
      )}
    </>
  );
}
