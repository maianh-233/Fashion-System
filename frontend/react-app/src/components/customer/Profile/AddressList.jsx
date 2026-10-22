import Button from "../../common/Button";
import { AlertTriangle, Building2, Home, Map, Pencil, Phone, Plus, Star, Trash2, User } from "lucide-react";

const ADDRESS_TYPE_LABELS = {
  HOME: "Nhà riêng",
  WORK: "Công ty",
  OTHER: "Địa chỉ khác",
};

const AddressTypeIcon = ({ type }) => {
  switch (type) {
    case "HOME":
      return <Home size={14} />;
    case "WORK":
      return <Building2 size={14} />;
    default:
      return <Map size={14} />;
  }
};

/** Sổ địa chỉ của khách (dữ liệu API). */
export default function AddressList({ addresses, busyId, onAdd, onEdit, onDelete, onSetDefault }) {
  return (
    <div className="customer-address-book">
      <div className="customer-address-book__toolbar">
        <p>{addresses.length > 0 ? `${addresses.length} địa chỉ đã lưu` : "Chưa có địa chỉ nào"}</p>
        <Button type="button" variant="unstyled" onClick={onAdd} className="customer-address-book__add">
          <Plus size={15} />
          Thêm địa chỉ
        </Button>
      </div>

      <div className="customer-address-grid">
        {addresses.map((addr) => {
          const isDefault = addr.isDefault;
          const located = addr.latitude != null && addr.longitude != null;
          const busy = busyId === addr.id;
          return (
            <article key={addr.id} className={`customer-address-card ${isDefault ? "is-default" : ""}`} aria-busy={busy || undefined}>
              <div className="customer-address-card__top">
                <span className="customer-address-card__type"><AddressTypeIcon type={addr.addressType} /></span>
                <div>
                  <small>{ADDRESS_TYPE_LABELS[addr.addressType] || ADDRESS_TYPE_LABELS.OTHER}</small>
                  <strong><User size={13} /> {addr.receiverName}</strong>
                </div>
                {isDefault && <span className="customer-address-card__default"><Star size={10} /> Mặc định</span>}
              </div>

              <address>
                <p>{addr.addressLine}</p>
                <span>{[addr.ward, addr.district, addr.province].filter(Boolean).join(", ")}</span>
                {addr.postalCode && <small>Mã bưu điện · {addr.postalCode}</small>}
                {!located && <small className="checkout-addresses__warn"><AlertTriangle size={12} /> Chưa chọn vị trí trên bản đồ, cần có để đặt hàng</small>}
              </address>

              <div className="customer-address-card__phone">
                <Phone size={13} /> {addr.receiverPhone}
              </div>

              <div className="customer-address-card__actions">
                {!isDefault && (
                  <Button type="button" variant="unstyled" disabled={busy} onClick={() => onSetDefault(addr)}>
                    Đặt làm mặc định
                  </Button>
                )}
                <Button type="button" variant="unstyled" disabled={busy} onClick={() => onEdit(addr)} aria-label={`Sửa địa chỉ ${addr.addressLine}`}>
                  <Pencil size={14} />
                </Button>
                <Button type="button" variant="unstyled" className="customer-address-card__delete" disabled={busy}
                  onClick={() => onDelete(addr)} aria-label={`Xóa địa chỉ ${addr.addressLine}`}>
                  <Trash2 size={14} />
                </Button>
              </div>
            </article>
          );
        })}
      </div>
    </div>
  );
}
