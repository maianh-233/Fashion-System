import { MapPinned } from "lucide-react";

const fields = [
  { name: "receiver_name", label: "Người nhận", placeholder: "Họ và tên", span: "half" },
  { name: "receiver_phone", label: "Số điện thoại", placeholder: "Số điện thoại liên hệ", span: "half", type: "tel" },
  { name: "province", label: "Tỉnh / Thành phố", placeholder: "Tỉnh hoặc thành phố" },
  { name: "district", label: "Quận / Huyện", placeholder: "Quận hoặc huyện" },
  { name: "ward", label: "Phường / Xã", placeholder: "Phường hoặc xã" },
];

export default function ShippingForm({
  form,
  setForm,
  readOnly = false,
  eyebrow = "Bước 03",
  title = "Thông tin giao hàng",
}) {
  if (!form) return null;

  const change = (event) => {
    if (readOnly) return;
    const { name, value } = event.target;
    setForm?.((previous) => ({ ...previous, [name]: value }));
  };

  return (
    <section className="checkout-section checkout-shipping-form">
      <div className="checkout-section__heading">
        <span><MapPinned size={17} /></span>
        <div><small>{eyebrow}</small><h2>{title}</h2></div>
      </div>

      <div className="checkout-form-grid">
        {fields.map((field) => (
          <label className={field.span === "half" ? "is-half" : ""} key={field.name}>
            <span>{field.label}</span>
            <input
              type={field.type || "text"}
              name={field.name}
              value={form[field.name] || ""}
              placeholder={field.placeholder}
              readOnly={readOnly}
              onChange={change}
            />
          </label>
        ))}
        <label className="is-wide">
          <span>Địa chỉ chi tiết</span>
          <textarea
            name="address_line"
            value={form.address_line || ""}
            placeholder="Số nhà, tên đường, tòa nhà..."
            readOnly={readOnly}
            onChange={change}
            rows={3}
          />
        </label>
      </div>
    </section>
  );
}
