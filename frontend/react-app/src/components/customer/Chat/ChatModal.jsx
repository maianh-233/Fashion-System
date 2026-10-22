import { useCallback, useEffect, useRef, useState } from "react";
import { Bot, Clock3, Headphones, LoaderCircle, MessageCircle, PackageCheck, SendHorizontal, ShieldCheck, X } from "lucide-react";
import Button from "../../common/Button";
import { supportChatApi } from "../../../api/customerApi";

const POLL_MS = 4000;
const time = (value) => (value ? new Date(value).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }) : "");

/**
 * Hỗ trợ đơn hàng: bot trả lời theo kịch bản (nút gợi ý hoặc câu khách tự gõ), cần thì chuyển cho nhân viên cửa
 * hàng. Tin của nhân viên được tải lại mỗi vài giây khi cửa sổ đang mở.
 */
export default function ChatModal({ order, onClose }) {
  const [conversation, setConversation] = useState(null);
  const [error, setError] = useState("");
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const roomRef = useRef(null);
  const endRef = useRef(null);

  useEffect(() => {
    let active = true;
    supportChatApi.open(order.id)
      .then((data) => {
        if (!active) return;
        roomRef.current = data.room.id;
        setConversation(data);
      })
      .catch((failure) => { if (active) setError(failure.message); });
    const timer = window.setInterval(() => {
      if (!roomRef.current || document.hidden) return;
      supportChatApi.conversation(roomRef.current).then((data) => { if (active) setConversation(data); }).catch(() => {});
    }, POLL_MS);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [order.id]);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [conversation?.messages?.length]);

  useEffect(() => {
    const onKey = (event) => { if (event.key === "Escape") onClose?.(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const send = useCallback(async (body) => {
    if (!roomRef.current) return;
    setSending(true);
    setError("");
    try {
      setConversation(await supportChatApi.send(roomRef.current, body));
      if (body.text) setInput("");
    } catch (failure) {
      setError(failure.message);
    } finally {
      setSending(false);
    }
  }, []);

  const messages = conversation?.messages || [];
  const lastOptions = [...messages].reverse().find((m) => m.options?.length)?.options || [];
  const room = conversation?.room;

  return (
    <div className="customer-chat" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose?.(); }}>
      <section className="customer-chat__dialog" role="dialog" aria-modal="true" aria-labelledby="customer-chat-title">
        <header className="customer-chat__header">
          <div className="customer-chat__identity">
            <span className="customer-chat__mark"><Headphones size={18} /></span>
            <div>
              <p>Lunaria concierge</p>
              <h2 id="customer-chat-title">Hỗ trợ đơn hàng</h2>
              <span><i /> {room ? room.statusLabel : "Đang kết nối..."}</span>
            </div>
          </div>
          <Button type="button" variant="unstyled" className="customer-chat__close" onClick={onClose} aria-label="Đóng cửa sổ hỗ trợ">
            <X size={17} />
          </Button>
        </header>

        <div className="customer-chat__order">
          <span><PackageCheck size={15} /></span>
          <div><small>Đang trao đổi về</small><strong>#{order.orderCode}</strong></div>
          <p><ShieldCheck size={13} /> Hỗ trợ bảo mật</p>
        </div>

        <div className="customer-chat__messages" aria-live="polite">
          {!conversation && !error && (
            <div className="customer-chat__day"><span><LoaderCircle className="animate-spin" size={13} /> Đang mở hội thoại...</span></div>
          )}
          {messages.map((message) => {
            const isCustomer = message.senderType === "CUSTOMER";
            const isSystem = message.senderType === "SYSTEM";
            if (isSystem) return <div key={message.id} className="customer-chat__day"><span>{message.content}</span></div>;
            return (
              <article key={message.id} className={`customer-chat__message ${isCustomer ? "is-customer" : "is-support"}`}>
                {!isCustomer && (
                  <span className="customer-chat__avatar">{message.senderType === "BOT" ? <Bot size={13} /> : <MessageCircle size={13} />}</span>
                )}
                <div>
                  {!isCustomer && <small className="customer-chat__sender">{message.senderName}</small>}
                  <p>{message.content}</p>
                  <time><Clock3 size={10} /> {time(message.createdAt)}</time>
                </div>
              </article>
            );
          })}
          {error && <p className="checkout-error">{error}</p>}
          <div ref={endRef} />
        </div>

        <footer className="customer-chat__composer">
          {lastOptions.length > 0 && (
            <div className="customer-chat__quick-replies" aria-label="Lựa chọn gợi ý">
              {lastOptions.map((option) => (
                <Button key={option.code} type="button" variant="unstyled" disabled={sending}
                  onClick={() => send({ optionCode: option.code })}>
                  {option.label}
                </Button>
              ))}
            </div>
          )}

          <div className="customer-chat__input-row">
            <textarea
              value={input}
              onChange={(event) => setInput(event.target.value.slice(0, 2000))}
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  if (input.trim()) send({ text: input.trim() });
                }
              }}
              placeholder="Nhập nội dung cần hỗ trợ..."
              rows={1}
              aria-label="Nội dung tin nhắn"
              disabled={!conversation}
            />
            <Button type="button" variant="unstyled" className="customer-chat__send" onClick={() => send({ text: input.trim() })}
              disabled={!input.trim() || sending || !conversation} aria-label="Gửi tin nhắn">
              <SendHorizontal size={17} />
            </Button>
          </div>
          <small>Nhấn Enter để gửi · Shift + Enter để xuống dòng</small>
        </footer>
      </section>
    </div>
  );
}
