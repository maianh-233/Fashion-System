import { BadgeCheck, CheckCircle2, Clock3, Package, Truck, XCircle } from "lucide-react";

const STATUS_META = {
  CREATED: { label: "Đã tạo đơn", description: "Đơn hàng đã được ghi nhận", icon: Package },
  CONFIRMED: { label: "Đã xác nhận", description: "Sản phẩm đang được chuẩn bị", icon: CheckCircle2 },
  SHIPPING: { label: "Đang giao", description: "Đơn hàng đang trên đường đến bạn", icon: Truck },
  DELIVERED: { label: "Hoàn tất", description: "Giao hàng thành công", icon: BadgeCheck },
  CANCELLED: { label: "Đã hủy", description: "Đơn hàng đã được hủy", icon: XCircle },
};

const formatDate = (value) => new Date(value).toLocaleString("vi-VN", {
  hour: "2-digit",
  minute: "2-digit",
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});

export default function OrderStatusTimeline({ status, history = [] }) {
  const currentIndex = history.findIndex((item) => item.status === status);

  return (
    <section className="checkout-section order-detail-timeline">
      <div className="checkout-section__heading">
        <span><Clock3 size={17} /></span>
        <div><small>Live tracking</small><h2>Hành trình đơn hàng</h2></div>
        <strong>{history.length}</strong>
      </div>

      <ol>
        {history.map((item, index) => {
          const meta = STATUS_META[item.status] || STATUS_META.CREATED;
          const Icon = meta.icon;
          const current = index === currentIndex;
          const completed = currentIndex >= 0 && index < currentIndex;

          return (
            <li key={`${item.status}-${item.time}`} className={`${current ? "is-current" : ""} ${completed ? "is-complete" : ""}`}>
              <span><Icon size={15} /></span>
              <div>
                <strong>{meta.label}</strong>
                <p>{meta.description}</p>
                <time dateTime={item.time}>{formatDate(item.time)}</time>
              </div>
            </li>
          );
        })}
      </ol>
    </section>
  );
}
