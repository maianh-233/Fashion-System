import { useState } from "react";
import Button from "../../common/Button";

const STATUS_LABEL = { SENT: "Đã gửi", PENDING: "Đang gửi", FAILED: "Gửi lỗi", SKIPPED: "Không gửi" };
const dateTime = value => value ? new Date(value).toLocaleString("vi-VN") : "—";

/** Hủy đơn: lý do bắt buộc vì được gửi cho khách trong thông báo. */
export function CancelPanel({ busy, onSubmit, onClose }) {
  const [reason, setReason] = useState("");
  return <form className="order-detail__section order-detail__panel" onSubmit={event => { event.preventDefault(); onSubmit(reason.trim()); }}>
    <header><h3>Hủy đơn</h3></header>
    <p className="order-detail__muted">Lý do sẽ được gửi cho khách. Tồn kho được cộng lại; voucher khách đã dùng không được hoàn.</p>
    <textarea className="order-detail__input" rows={3} maxLength={500} required autoFocus aria-label="Lý do hủy đơn"
      placeholder="Ví dụ: Sản phẩm bị lỗi đường may, cửa hàng không còn hàng thay thế" value={reason} onChange={event => setReason(event.target.value)} />
    <div className="order-detail__panel-actions">
      <Button type="button" variant="secondary" onClick={onClose}>Đóng</Button>
      <Button type="submit" variant="danger-solid" loading={busy} disabled={!reason.trim()}>Xác nhận hủy đơn</Button>
    </div>
  </form>;
}

/** Liên hệ khách qua email và/hoặc thông báo trong hệ thống; lịch sử được lưu cùng đơn. */
export function ContactPanel({ hasEmail, busy, onSubmit, onClose }) {
  const [form, setForm] = useState({ subject: "", content: "", viaEmail: hasEmail, viaNotification: true });
  const valid = form.subject.trim() && form.content.trim() && (form.viaEmail || form.viaNotification);
  return <form className="order-detail__section order-detail__panel" onSubmit={event => { event.preventDefault(); onSubmit({ ...form, subject: form.subject.trim(), content: form.content.trim() }); }}>
    <header><h3>Liên hệ khách</h3></header>
    <input className="order-detail__input" required maxLength={200} autoFocus aria-label="Tiêu đề" placeholder="Tiêu đề"
      value={form.subject} onChange={event => setForm({ ...form, subject: event.target.value })} />
    <textarea className="order-detail__input" rows={4} required maxLength={5000} aria-label="Nội dung" placeholder="Nội dung gửi khách"
      value={form.content} onChange={event => setForm({ ...form, content: event.target.value })} />
    <div className="order-detail__checks">
      <label><input type="checkbox" checked={form.viaNotification} onChange={event => setForm({ ...form, viaNotification: event.target.checked })} /> Thông báo trong hệ thống</label>
      <label title={hasEmail ? "" : "Khách chưa có email"}><input type="checkbox" disabled={!hasEmail} checked={form.viaEmail} onChange={event => setForm({ ...form, viaEmail: event.target.checked })} /> Email{hasEmail ? "" : " (khách chưa có email)"}</label>
    </div>
    <div className="order-detail__panel-actions">
      <Button type="button" variant="secondary" onClick={onClose}>Đóng</Button>
      <Button type="submit" variant="primary" loading={busy} disabled={!valid}>Gửi cho khách</Button>
    </div>
  </form>;
}

export function ContactHistory({ contacts }) {
  if (!contacts.length) return <p className="order-detail__muted">Chưa liên hệ khách về đơn này.</p>;
  return <ol className="order-detail__contacts">
    {contacts.map(contact => <li key={contact.id}>
      <strong>{contact.subject}</strong>
      <span>{contact.content}</span>
      <span className="order-detail__muted">
        {contact.sentByName} · {dateTime(contact.createdAt)}
        {contact.viaNotification && ` · Thông báo: ${STATUS_LABEL[contact.notificationStatus]}`}
        {contact.viaEmail && ` · Email ${contact.emailTo}: ${STATUS_LABEL[contact.emailStatus]}`}
      </span>
    </li>)}
  </ol>;
}
