import { Check, Store, Truck } from "lucide-react";

export default function OrderTypeSelector({
  value,
  onChange,
}) {
  const options = [
    { value: "ONLINE", title: "Giao tận nơi", description: "Giao hàng bảo đảm đến địa chỉ của bạn", icon: Truck },
    { value: "PICKUP", title: "Nhận tại cửa hàng", description: "Linh hoạt thời gian, không mất phí giao hàng", icon: Store },
  ];

  return (
    <section className="checkout-section checkout-order-type">
      <div className="checkout-section__heading">
        <span><Truck size={17} /></span>
        <div><small>Bước 02</small><h2>Hình thức nhận hàng</h2></div>
      </div>

      <div className="checkout-choice-grid">
        {options.map((option) => {
          const Icon = option.icon;
          const active = value === option.value;
          return (
            <button type="button" key={option.value} className={active ? "is-active" : ""} onClick={() => onChange(option.value)}>
              <Icon size={19} />
              <span><strong>{option.title}</strong><small>{option.description}</small></span>
              <i>{active && <Check size={13} />}</i>
            </button>
          );
        })}
      </div>
    </section>
  );
}
