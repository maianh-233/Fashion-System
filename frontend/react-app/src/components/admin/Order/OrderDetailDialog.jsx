import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CheckCircle2, EyeOff, ImageOff, MessageSquare, PackageCheck, RotateCcw, Truck, XCircle } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../common/AdminDialog";
import Button from "../../common/Button";
import { useSystemNotification } from "../../common/SystemNotification";
import { orderApi } from "../../../api/admin/orderApi";
import { money } from "../../../pages/admin/offlinePosLogic";
import {
  ACTOR_LABELS, ORDER_STATUS_STYLES, PAYMENT_ATTEMPT_LABELS, PAYMENT_METHOD_LABELS, PAYMENT_STATUS_LABELS, REFUND_STATUS_LABELS,
  SHIPMENT_STATUS_LABELS, handoverLabel,
} from "../../../pages/admin/orderManagementLogic";
import { CancelPanel, ContactHistory, ContactPanel } from "./OrderActionPanels";
import "./OrderDetail.css";

const dateTime = value => value ? new Date(value).toLocaleString("vi-VN") : "—";

function Section({ title, children, aside }) {
  return <section className="order-detail__section">
    <header><h3>{title}</h3>{aside}</header>
    {children}
  </section>;
}

function Field({ label, children }) {
  return <div className="order-detail__field"><span>{label}</span><strong>{children ?? "—"}</strong></div>;
}

/**
 * Chi tiết đơn. Nút thao tác chỉ hiện theo danh sách {@code actions} backend trả về (đã xét trạng thái
 * và quyền); backend vẫn kiểm tra lại khi thực hiện.
 */
export default function OrderDetailDialog({ orderId, onClose, onChanged }) {
  const notification = useSystemNotification();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");
  const [panel, setPanel] = useState("");
  const [contacts, setContacts] = useState([]);

  useEffect(() => {
    const controller = new AbortController();
    orderApi.detail(orderId, { signal: controller.signal })
      .then(setOrder)
      .catch(failure => { if (!controller.signal.aborted) setError(failure.message); });
    orderApi.contacts(orderId, { signal: controller.signal }).then(setContacts).catch(() => {});
    return () => controller.abort();
  }, [orderId]);

  /** Thực hiện một thao tác trả về chi tiết đơn mới; lỗi nghiệp vụ hiển thị nguyên văn từ backend. */
  const perform = async (key, request, successMessage) => {
    setBusy(key);
    try {
      const updated = await request();
      setOrder(updated);
      setPanel("");
      notification.success(successMessage(updated));
      onChanged?.(updated);
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setBusy("");
    }
  };

  const confirmOrder = async () => {
    if (!await notification.confirm(`Xác nhận đơn ${order.orderCode}? Bạn sẽ là nhân viên xử lý đơn này.`, { confirmText: "Xác nhận đơn" })) return;
    perform("CONFIRM", () => orderApi.confirm(order.id), updated => `Đã xác nhận đơn ${updated.orderCode}.`);
  };
  const confirmReturn = async () => {
    if (!await notification.confirm(`Xác nhận đã nhận lại hàng hoàn của đơn ${order.orderCode}? Tồn kho sẽ được cộng lại.`, { confirmText: "Đã nhận hàng" })) return;
    perform("CONFIRM_RETURN", () => orderApi.confirmReturn(order.id), updated => `Đơn ${updated.orderCode} đã hoàn hàng.`);
  };
  const handover = async () => {
    const label = handoverLabel(order.shipments);
    if (!await notification.confirm(`${label} cho đơn ${order.orderCode}? Hệ thống sẽ đặt chuyến với đơn vị vận chuyển.`, { confirmText: label })) return;
    perform("HANDOVER", () => orderApi.handover(order.id), updated => `Đã tạo chuyến giao cho đơn ${updated.orderCode}.`);
  };
  const cancelOrder = reason => perform("CANCEL", () => orderApi.cancel(order.id, reason), updated => `Đã hủy đơn ${updated.orderCode}.`);
  const contactCustomer = async body => {
    setBusy("CONTACT");
    try {
      const created = await orderApi.contact(order.id, body);
      setContacts(current => [created, ...current]);
      setPanel("");
      notification.success("Đã gửi liên hệ cho khách.");
      // Trạng thái gửi được cập nhật sau khi giao dịch hoàn tất; tải lại để hiện kết quả.
      setTimeout(() => orderApi.contacts(order.id).then(setContacts).catch(() => {}), 1500);
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setBusy("");
    }
  };

  const run = async (action) => {
    const hide = action === "HIDE";
    const confirmed = await notification.confirm(hide
      ? `Ẩn đơn ${order.orderCode} khỏi danh sách? Dữ liệu không bị xóa và có thể khôi phục.`
      : `Khôi phục đơn ${order.orderCode} về danh sách?`, { confirmText: hide ? "Ẩn đơn" : "Khôi phục" });
    if (!confirmed) return;
    setBusy(action);
    try {
      const updated = hide ? await orderApi.hide(order.id) : await orderApi.restore(order.id);
      setOrder(updated);
      notification.success(hide ? `Đã ẩn đơn ${updated.orderCode}.` : `Đã khôi phục đơn ${updated.orderCode}.`);
      onChanged?.(updated);
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setBusy("");
    }
  };

  const actions = order?.actions || [];
  return <AdminDialog open onClose={onClose} size="xl" className="order-detail-dialog">
    <AdminDialogHeader title={order ? `Đơn ${order.orderCode}` : "Chi tiết đơn hàng"}
      description={order ? `${order.orderType === "OFFLINE" ? "Đơn tại quầy" : "Đơn online"} · ${order.store?.name || ""} · ${dateTime(order.createdAt)}` : ""}
      onClose={onClose} />
    <AdminDialogBody>
      {!order && !error && <p className="order-detail__muted">Đang tải chi tiết đơn…</p>}
      {error && <p className="order-detail__error">{error}</p>}
      {order && <div className="order-detail">
        <div className="order-detail__chips">
          <span className={`px-4 py-1 rounded-full text-xs ${ORDER_STATUS_STYLES[order.status] || "bg-zinc-700"}`}>{order.statusLabel}</span>
          {!order.visibility.active && <span className="order-detail__chip">Đã ẩn bởi {order.visibility.hiddenBy?.name} · {dateTime(order.visibility.hiddenAt)}</span>}
          {order.parentOrder && <span className="order-detail__chip">Thuộc đơn tổng {order.parentOrder.code}</span>}
        </div>

        <div className="order-detail__grid">
          <Section title="Khách hàng">
            {order.customer ? <>
              <Field label="Họ tên">{order.customer.fullName}</Field>
              <Field label="Số điện thoại">{order.customer.phone}</Field>
              <Field label="Email">{order.customer.email}</Field>
              <Field label="Mã khách">{order.customer.customerCode}</Field>
            </> : <p className="order-detail__muted">Khách vãng lai</p>}
          </Section>
          <Section title="Xử lý đơn">
            <Field label="Nhân viên xử lý">{order.handler?.name}</Field>
            <Field label="Người tạo">{order.createdBy?.name || (order.orderType === "ONLINE" ? "Khách đặt online" : null)}</Field>
            <Field label="Cửa hàng">{order.store?.name}</Field>
            {order.note && <Field label="Ghi chú">{order.note}</Field>}
          </Section>
          {order.shippingAddress && <Section title="Địa chỉ giao hàng"
            aside={order.shippingAddress.differentFromDefault && <span className="order-detail__chip order-detail__chip--warn">Khác địa chỉ mặc định</span>}>
            <Field label="Người nhận">{order.shippingAddress.receiverName} · {order.shippingAddress.receiverPhone}</Field>
            <Field label="Địa chỉ">{[order.shippingAddress.addressLine, order.shippingAddress.ward, order.shippingAddress.district, order.shippingAddress.province].filter(Boolean).join(", ")}</Field>
          </Section>}
          {order.cancellation && <Section title="Hủy đơn">
            <Field label="Bên hủy">{ACTOR_LABELS[order.cancellation.cancelledBy]}</Field>
            <Field label="Lý do">{order.cancellation.reason || "Không nêu lý do"}</Field>
          </Section>}
        </div>

        <Section title={`Sản phẩm (${order.items.reduce((sum, item) => sum + item.quantity, 0)})`}>
          <div className="order-detail__items">
            {order.items.map(item => <article key={item.id} className="order-detail__item">
              {item.imageUrl ? <img src={item.imageUrl} alt="" /> : <span className="order-detail__thumb"><ImageOff size={16} /></span>}
              <div className="min-w-0">
                <strong>{item.productName}</strong>
                <span>{item.sku} · {[item.color, item.size].filter(Boolean).join(" / ")}</span>
                <span>{item.quantity} × {money(item.unitPrice)}{item.promotionCode ? ` · ${item.promotionCode} −${money(item.discount)}` : ""}</span>
              </div>
              <div className="order-detail__price">
                {Number(item.discount) > 0 && <s>{money(Number(item.unitPrice) * item.quantity)}</s>}
                <strong>{money(item.total)}</strong>
              </div>
            </article>)}
          </div>
        </Section>

        <div className="order-detail__grid">
          <Section title="Thanh toán">
            <div className="order-detail__totals">
              <div><span>Tạm tính</span><span>{money(order.totals.subtotal)}</span></div>
              {Number(order.totals.productDiscount) > 0 && <div className="is-discount"><span>Giảm theo sản phẩm</span><span>−{money(order.totals.productDiscount)}</span></div>}
              {order.promotions.filter(p => p.type !== "PRODUCT").map(p => <div key={p.promotionId} className="is-discount"><span>Voucher {p.code}{p.name ? ` · ${p.name}` : ""}</span><span>−{money(p.discount)}</span></div>)}
              {Number(order.totals.shippingFee) > 0 && <div><span>Phí vận chuyển</span><span>{money(order.totals.shippingFee)}</span></div>}
              <div className="is-total"><span>Tổng thanh toán</span><span>{money(order.totals.total)}</span></div>
              {order.totals.cashReceived != null && <>
                <div><span>Tiền khách đưa</span><span>{money(order.totals.cashReceived)}</span></div>
                <div><span>Tiền thối</span><span>{money(order.totals.changeAmount)}</span></div>
              </>}
            </div>
            <Field label="Phương thức">{PAYMENT_METHOD_LABELS[order.payment.method] || order.payment.method}</Field>
            <Field label="Trạng thái">{PAYMENT_STATUS_LABELS[order.payment.status] || order.payment.status}</Field>
            {(order.payment.refunds || []).map(r => <Field key={r.id} label="Hoàn tiền">
              {money(r.amount)} · {REFUND_STATUS_LABELS[r.status] || r.status}
            </Field>)}
            {order.payment.records.filter(r => r.method !== "COD").map(r => <Field key={r.id}
              label={`${PAYMENT_METHOD_LABELS[r.method] || r.method} · ${dateTime(r.createdAt)}`}>
              {money(r.amount)} · {PAYMENT_ATTEMPT_LABELS[r.status] || r.status}
              {r.transactionCode ? ` · mã GD ${r.transactionCode}` : ""}{r.paidAt ? ` · ${dateTime(r.paidAt)}` : ""}
            </Field>)}
          </Section>
          <Section title="Vận chuyển">
            {!order.shipments.length && <p className="order-detail__muted">{order.orderType === "OFFLINE" ? "Đơn tại quầy, không giao hàng." : "Chưa tạo chuyến giao hàng."}</p>}
            {order.shipments.map(s => <div key={s.id} className="order-detail__shipment">
              <Field label={`Lần ${s.attemptNo}`}>{SHIPMENT_STATUS_LABELS[s.status] || s.status}</Field>
              <Field label="Đơn vị · mã vận đơn">{s.provider} · {s.trackingCode}</Field>
              {s.fee != null && <Field label="Phí ship (cửa hàng trả)">{money(s.fee)}</Field>}
              {s.driverName && <Field label="Tài xế">{s.driverName}{s.driverPhone ? ` · ${s.driverPhone}` : ""}{s.driverPlate ? ` · ${s.driverPlate}` : ""}</Field>}
              {s.failureReason && <Field label="Lý do">{s.failureReason}</Field>}
              {s.trackingUrl && <Field label="Theo dõi"><a className="order-detail__link" href={s.trackingUrl} target="_blank" rel="noreferrer">Mở trang theo dõi</a></Field>}
              <Field label="Tạo lúc">{dateTime(s.createdAt)}</Field>
            </div>)}
          </Section>
        </div>

        {panel === "CANCEL" && <CancelPanel busy={busy === "CANCEL"} onSubmit={cancelOrder} onClose={() => setPanel("")} />}
        {panel === "CONTACT" && <ContactPanel hasEmail={!!order.customer?.email} busy={busy === "CONTACT"} onSubmit={contactCustomer} onClose={() => setPanel("")} />}

        {order.customer && <Section title="Liên hệ khách"
          aside={order.supportRoomId && <Link className="order-detail__link" to={`/admin/support?room=${order.supportRoomId}`}>Mở hội thoại hỗ trợ</Link>}>
          <ContactHistory contacts={contacts} />
        </Section>}

        <Section title="Lịch sử trạng thái">
          <ol className="order-detail__timeline">
            {order.statusHistory.map((h, index) => <li key={`${h.changedAt}-${index}`}>
              <span className={`order-detail__dot ${index === order.statusHistory.length - 1 ? "is-current" : ""}`} />
              <div>
                <strong>{h.toLabel}</strong>
                <span>{dateTime(h.changedAt)} · {h.actorName}</span>
                {(h.reason || h.note) && <span>{h.reason || h.note}</span>}
              </div>
            </li>)}
          </ol>
        </Section>
      </div>}
    </AdminDialogBody>
    <AdminDialogFooter>
      {actions.includes("CONTACT_CUSTOMER") && <Button type="button" variant="ghost" onClick={() => setPanel(panel === "CONTACT" ? "" : "CONTACT")}><MessageSquare size={16} /> Liên hệ khách</Button>}
      {actions.includes("CANCEL") && <Button type="button" variant="danger" onClick={() => setPanel(panel === "CANCEL" ? "" : "CANCEL")}><XCircle size={16} /> Hủy đơn</Button>}
      {actions.includes("HANDOVER_SHIPPING") && <Button type="button" variant="primary" loading={busy === "HANDOVER"} onClick={handover}><Truck size={16} /> {handoverLabel(order.shipments)}</Button>}
      {actions.includes("CONFIRM_RETURN") && <Button type="button" variant="primary" loading={busy === "CONFIRM_RETURN"} onClick={confirmReturn}><PackageCheck size={16} /> Đã nhận hàng hoàn</Button>}
      {actions.includes("CONFIRM") && <Button type="button" variant="primary" loading={busy === "CONFIRM"} onClick={confirmOrder}><CheckCircle2 size={16} /> Xác nhận đơn</Button>}
      {actions.includes("HIDE") && <Button type="button" variant="danger" loading={busy === "HIDE"} onClick={() => run("HIDE")}><EyeOff size={16} /> Ẩn đơn</Button>}
      {actions.includes("RESTORE") && <Button type="button" variant="primary" loading={busy === "RESTORE"} onClick={() => run("RESTORE")}><RotateCcw size={16} /> Khôi phục</Button>}
      <Button type="button" variant="secondary" onClick={onClose} className="admin-dialog__secondary-action">Đóng</Button>
    </AdminDialogFooter>
  </AdminDialog>;
}
