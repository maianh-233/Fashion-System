import { NotebookIcon } from "lucide-react";

export default function OrderNote({
  value,
  onChange,
  readOnly = false,
  eyebrow = "Tùy chọn",
  title = "Ghi chú đơn hàng",
}) {
  return (
    <section className="checkout-section checkout-note">
      <div className="checkout-section__heading">
        <span><NotebookIcon size={17} /></span>
        <div><small>{eyebrow}</small><h2>{title}</h2></div>
      </div>

      <textarea
        value={value || ""}
        readOnly={readOnly}
        onChange={(e) => {
          if (!readOnly) onChange?.(e.target.value);
        }}
        rows={4}
        placeholder={
          readOnly
            ? "Không có ghi chú"
            : "VD: Giao giờ hành chính, gọi trước khi giao, lấy hàng sau 18h..."
        }
        className="checkout-note__input"
      />

      {!readOnly && (
        <p className="checkout-note__hint">Không bắt buộc · tối đa 300 ký tự</p>
      )}
    </section>
  );
}
