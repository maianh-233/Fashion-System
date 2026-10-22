import { BadgeCheck, CheckCircle2, Clock3, Package, PackageX, RotateCcw, Truck, Warehouse, XCircle } from "lucide-react";
import { formatDateTime, orderProgress } from "../../../pages/customer/customerOrderLogic";

const STEP_META = {
  PENDING: { label: "Chờ xác nhận", description: "Cửa hàng đã nhận đơn và sẽ xác nhận sớm", icon: Package },
  CONFIRMED: { label: "Đã xác nhận", description: "Sản phẩm đang được chuẩn bị", icon: CheckCircle2 },
  HANDED_TO_SHIPPER: { label: "Đã giao cho vận chuyển", description: "Đơn vị vận chuyển đang tới lấy hàng", icon: Warehouse },
  SHIPPING: { label: "Đang giao", description: "Đơn hàng đang trên đường đến bạn", icon: Truck },
  COMPLETED: { label: "Hoàn thành", description: "Giao hàng thành công", icon: BadgeCheck },
};
const OFF_TRACK_META = {
  CANCELLED: { label: "Đã hủy", icon: XCircle },
  DELIVERY_FAILED: { label: "Giao không thành công", icon: PackageX },
  RETURNED: { label: "Đã hoàn hàng về cửa hàng", icon: RotateCcw },
};

/** Tiến trình đơn và nhật ký trạng thái (đơn tách ghi kèm mã phần). */
export default function OrderStatusTimeline({ status, history = [], cancelReason }) {
  const { steps, offTrack } = orderProgress(status, history);
  const OffIcon = offTrack ? OFF_TRACK_META[offTrack.status].icon : null;

  return (
    <section className="checkout-section order-detail-timeline">
      <div className="checkout-section__heading">
        <span><Clock3 size={17} /></span>
        <div><small>Live tracking</small><h2>Hành trình đơn hàng</h2></div>
        <strong>{history.length}</strong>
      </div>

      <ol>
        {steps.map((step) => {
          const meta = STEP_META[step.status];
          const Icon = meta.icon;
          return (
            <li key={step.status} className={`${step.current ? "is-current" : ""} ${step.done ? "is-complete" : ""}`}>
              <span><Icon size={15} /></span>
              <div>
                <strong>{meta.label}</strong>
                <p>{meta.description}</p>
                {step.at && <time dateTime={step.at}>{formatDateTime(step.at)}</time>}
              </div>
            </li>
          );
        })}
        {offTrack && (
          <li className="is-current is-off-track">
            <span><OffIcon size={15} /></span>
            <div>
              <strong>{OFF_TRACK_META[offTrack.status].label}</strong>
              {cancelReason && <p>Lý do: {cancelReason}</p>}
              {offTrack.at && <time dateTime={offTrack.at}>{formatDateTime(offTrack.at)}</time>}
            </div>
          </li>
        )}
      </ol>

      {history.length > 0 && (
        <details className="order-detail-timeline__log">
          <summary>Nhật ký chi tiết ({history.length})</summary>
          <ul>
            {[...history].reverse().map((entry, index) => (
              <li key={`${entry.status}-${entry.at}-${index}`}>
                <time>{formatDateTime(entry.at)}</time>
                <span>{entry.partCode ? `${entry.partCode} · ` : ""}{entry.statusLabel}{entry.note ? ` — ${entry.note}` : ""}</span>
              </li>
            ))}
          </ul>
        </details>
      )}
    </section>
  );
}
