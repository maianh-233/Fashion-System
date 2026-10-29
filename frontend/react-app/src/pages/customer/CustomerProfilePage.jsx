import { useCallback, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { MapPin, Ruler, ShieldCheck, Sparkles, UserRound } from "lucide-react";
import ProfileHeader from "../../components/customer/Profile/ProfileHeader";
import InfoSection from "../../components/customer/Profile/InfoSection";
import AddressList from "../../components/customer/Profile/AddressList";
import MeasurementsSection from "../../components/customer/Profile/MeasurementsSection";
import EditProfileModal from "../../components/customer/Profile/EditProfileModal";
import AddAddressModal from "../../components/customer/Profile/AddAddressModal";
import ChangePasswordModal from "../../components/customer/Profile/ChangePasswordModal";
import DetailState from "../../components/customer/DetailState";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { addressApi, profileApi } from "../../api/customer/accountApi";
import { updateCustomerProfile } from "../../api/http/customerSession";
import { useCustomerSession } from "../../hooks/useCustomerSession";

/** Hồ sơ của khách (API): thông tin cá nhân, hạng và điểm, đổi mật khẩu, sổ địa chỉ. */
export default function CustomerProfilePage() {
  const { loggedIn } = useCustomerSession();
  const notification = useSystemNotification();
  const [profile, setProfile] = useState(null);
  const [addresses, setAddresses] = useState([]);
  const [error, setError] = useState("");
  const [editOpen, setEditOpen] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [addressForm, setAddressForm] = useState(null);
  const [busyId, setBusyId] = useState(null);

  const loadAddresses = useCallback(async () => {
    const page = await addressApi.list();
    setAddresses(page.content || []);
  }, []);

  useEffect(() => {
    if (!loggedIn) return undefined;
    let active = true;
    Promise.all([profileApi.get(), addressApi.list()])
      .then(([nextProfile, page]) => {
        if (!active) return;
        setProfile(nextProfile);
        setAddresses(page.content || []);
      })
      .catch((failure) => { if (active) setError(failure.message); });
    return () => {
      active = false;
    };
  }, [loggedIn]);

  if (!loggedIn) return <Navigate to={`/customerlogin?redirect=${encodeURIComponent("/profile")}`} replace />;

  const saveProfile = async (body) => {
    const updated = await profileApi.update(body);
    setProfile(updated);
    updateCustomerProfile({ fullName: updated.fullName, email: updated.email });
    notification.success("Đã cập nhật hồ sơ.");
  };

  const changePassword = async (body) => {
    await profileApi.changePassword(body);
    setProfile((current) => ({ ...current, hasPassword: true }));
    notification.success("Đã lưu mật khẩu mới.");
  };

  const saveAddress = async (body) => {
    if (addressForm?.id) await addressApi.update(addressForm.id, body);
    else await addressApi.create(body);
    await loadAddresses();
    notification.success("Đã lưu địa chỉ.");
  };

  const runOnAddress = async (address, action, success) => {
    setBusyId(address.id);
    try {
      await action();
      await loadAddresses();
      notification.success(success);
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setBusyId(null);
    }
  };

  const deleteAddress = async (address) => {
    if (!await notification.confirm(`Xóa địa chỉ "${address.addressLine}"?`, { confirmText: "Xóa" })) return;
    runOnAddress(address, () => addressApi.remove(address.id), "Đã xóa địa chỉ.");
  };

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

          <DetailState loading={!profile && !error} error={error} icon={UserRound} backTo="/" backLabel="Về trang chủ" />
          {profile && (
            <main className="customer-profile-shell">
              <ProfileHeader profile={profile} onEdit={() => setEditOpen(true)} onChangePassword={() => setPasswordOpen(true)} />

              <div className="customer-profile-content">
                <section className="customer-profile-panel" aria-labelledby="personal-info-title">
                  <div className="customer-profile-section-heading">
                    <span><UserRound size={16} /></span>
                    <div><small>Personal details</small><h2 id="personal-info-title">Thông tin cá nhân</h2></div>
                  </div>
                  <InfoSection profile={profile} />
                </section>

                <section className="customer-profile-panel" aria-labelledby="measurements-title">
                  <div className="customer-profile-section-heading">
                    <span><Ruler size={16} /></span>
                    <div><small>Size profile</small><h2 id="measurements-title">Số đo để gợi ý size</h2></div>
                  </div>
                  <MeasurementsSection />
                </section>

                <section className="customer-profile-panel" aria-labelledby="address-list-title">
                  <div className="customer-profile-section-heading">
                    <span><MapPin size={16} /></span>
                    <div><small>Delivery book</small><h2 id="address-list-title">Sổ địa chỉ</h2></div>
                    <strong>{addresses.length}</strong>
                  </div>
                  <AddressList
                    addresses={addresses}
                    busyId={busyId}
                    onAdd={() => setAddressForm({})}
                    onEdit={(address) => setAddressForm(address)}
                    onDelete={deleteAddress}
                    onSetDefault={(address) => runOnAddress(address, () => addressApi.setDefault(address.id), "Đã đặt địa chỉ mặc định.")}
                  />
                </section>
              </div>
            </main>
          )}
        </div>
      </div>

      {editOpen && profile && <EditProfileModal profile={profile} onClose={() => setEditOpen(false)} onSave={saveProfile} />}
      {passwordOpen && profile && (
        <ChangePasswordModal hasPassword={profile.hasPassword} onClose={() => setPasswordOpen(false)} onSave={changePassword} />
      )}
      {addressForm && profile && (
        <AddAddressModal
          address={addressForm.id ? addressForm : null}
          defaults={{ receiverName: profile.fullName, receiverPhone: profile.phone, isDefault: addresses.length === 0 }}
          onClose={() => setAddressForm(null)}
          onSave={saveAddress}
        />
      )}
    </>
  );
}
