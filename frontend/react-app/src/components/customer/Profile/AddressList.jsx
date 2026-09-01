import Button from "../../common/Button";
import {
  Plus,
  Trash2,
  Star,
  Phone,
  User,
  Home,
  Building2,
  Map
} from "lucide-react";

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

export default function AddressList({
  addresses,
  onAdd,
  onDelete,
  onSetDefault,
}) {
  return (
    <div className="customer-address-book">
      <div className="customer-address-book__toolbar">
        <p>{addresses.length > 0 ? `${addresses.length} địa chỉ đã lưu` : "Chưa có địa chỉ nào"}</p>
        <Button
          type="button"
          variant="unstyled"
          onClick={onAdd}
          className="customer-address-book__add"
        >
          <Plus size={15} />
          Thêm địa chỉ
        </Button>
      </div>

      <div className="customer-address-grid">
          {addresses.map((addr) => {
            const isDefault = addr.is_default;

            return (
              <article key={addr.id} className={`customer-address-card ${isDefault ? "is-default" : ""}`}>
                <div className="customer-address-card__top">
                  <span className="customer-address-card__type"><AddressTypeIcon type={addr.address_type} /></span>
                  <div>
                    <small>{ADDRESS_TYPE_LABELS[addr.address_type] || ADDRESS_TYPE_LABELS.OTHER}</small>
                    <strong><User size={13} /> {addr.receiver_name}</strong>
                  </div>
                  {isDefault && (
                    <span className="customer-address-card__default"><Star size={10} /> Mặc định</span>
                  )}
                </div>

                <address>
                  <p>{addr.address_line}</p>
                  <span>{addr.ward}, {addr.district}, {addr.province}</span>
                  {addr.postal_code && <small>Mã bưu điện · {addr.postal_code}</small>}
                </address>

                <div className="customer-address-card__phone">
                  <Phone size={13} /> {addr.receiver_phone}
                </div>

                <div className="customer-address-card__actions">
                  {!isDefault && (
                    <Button type="button" variant="unstyled"
                      onClick={() => onSetDefault(addr.id)}
                    >
                      Đặt làm mặc định
                    </Button>
                  )}
                  <Button type="button" variant="unstyled" className="customer-address-card__delete" onClick={() => onDelete(addr.id)} aria-label={`Xóa địa chỉ ${addr.address_line}`}>
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
