import { Check, Home, MapPin } from "lucide-react";

export default function SavedAddresses({ addresses, selectedId, onSelect }) {
  return (
    <section className="checkout-section checkout-addresses">
      <div className="checkout-section__heading">
        <span><Home size={17} /></span>
        <div><small>Địa chỉ nhanh</small><h2>Địa chỉ đã lưu</h2></div>
      </div>

      <div className="checkout-addresses__list">
        {addresses.map((address) => {
          const active = selectedId === address.id;
          return (
            <button type="button" key={address.id} className={active ? "is-active" : ""} onClick={() => onSelect(address)}>
              <MapPin size={16} />
              <span>
                <strong>{address.receiver_name}</strong>
                <small>{address.receiver_phone}</small>
                <p>{address.address_line}, {address.ward}, {address.district}</p>
              </span>
              <i>{active && <Check size={13} />}</i>
            </button>
          );
        })}
      </div>
    </section>
  );
}
