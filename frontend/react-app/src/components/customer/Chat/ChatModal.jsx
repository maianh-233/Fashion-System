import { useEffect, useRef, useState } from "react";
import {
  Clock3,
  Headphones,
  MessageCircle,
  PackageCheck,
  SendHorizontal,
  ShieldCheck,
  X,
} from "lucide-react";
import Button from "../../common/Button";

const quickReplies = [
  "Cho tôi hỏi tình trạng đơn hàng?",
  "Khi nào đơn được giao?",
  "Tôi muốn đổi thông tin nhận hàng.",
];

const getCurrentTime = () =>
  new Date().toLocaleTimeString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
  });

export default function ChatModal({ orderId, onClose }) {
  const [messages, setMessages] = useState([
    {
      id: "welcome",
      from: "support",
      text: `Xin chào! Lunaria có thể hỗ trợ gì cho bạn về đơn hàng ${orderId}?`,
      time: getCurrentTime(),
    },
  ]);
  const [input, setInput] = useState("");
  const messagesEndRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    const handleEscape = (event) => {
      if (event.key === "Escape") onClose?.();
    };

    window.addEventListener("keydown", handleEscape);
    return () => window.removeEventListener("keydown", handleEscape);
  }, [onClose]);

  const sendMessage = (message = input) => {
    const content = message.trim();
    if (!content) return;

    setMessages((previous) => [
      ...previous,
      { id: `customer-${Date.now()}`, from: "customer", text: content, time: getCurrentTime() },
    ]);
    setInput("");

    window.setTimeout(() => {
      setMessages((previous) => [
        ...previous,
        {
          id: `support-${Date.now()}`,
          from: "support",
          text: "Cảm ơn bạn. Chuyên viên chăm sóc khách hàng sẽ kiểm tra và phản hồi ngay.",
          time: getCurrentTime(),
        },
      ]);
    }, 700);
  };

  return (
    <div className="customer-chat" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose?.();
    }}>
      <section
        className="customer-chat__dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="customer-chat-title"
      >
        <header className="customer-chat__header">
          <div className="customer-chat__identity">
            <span className="customer-chat__mark"><Headphones size={18} /></span>
            <div>
              <p>Lunaria concierge</p>
              <h2 id="customer-chat-title">Hỗ trợ đơn hàng</h2>
              <span><i /> Trực tuyến · thường phản hồi trong vài phút</span>
            </div>
          </div>

          <Button type="button" variant="unstyled" className="customer-chat__close" onClick={onClose} aria-label="Đóng cửa sổ hỗ trợ">
            <X size={17} />
          </Button>
        </header>

        <div className="customer-chat__order">
          <span><PackageCheck size={15} /></span>
          <div><small>Đang trao đổi về</small><strong>{orderId}</strong></div>
          <p><ShieldCheck size={13} /> Hỗ trợ bảo mật</p>
        </div>

        <div className="customer-chat__messages" aria-live="polite">
          <div className="customer-chat__day"><span>Hôm nay</span></div>
          {messages.map((message) => {
            const isCustomer = message.from === "customer";
            return (
              <article key={message.id} className={`customer-chat__message ${isCustomer ? "is-customer" : "is-support"}`}>
                {!isCustomer && <span className="customer-chat__avatar"><MessageCircle size={13} /></span>}
                <div>
                  <p>{message.text}</p>
                  <time><Clock3 size={10} /> {message.time}</time>
                </div>
              </article>
            );
          })}
          <div ref={messagesEndRef} />
        </div>

        <footer className="customer-chat__composer">
          <div className="customer-chat__quick-replies" aria-label="Câu hỏi gợi ý">
            {quickReplies.map((reply) => (
              <Button key={reply} type="button" variant="unstyled" onClick={() => sendMessage(reply)}>
                {reply}
              </Button>
            ))}
          </div>

          <div className="customer-chat__input-row">
            <textarea
              value={input}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  sendMessage();
                }
              }}
              placeholder="Nhập nội dung cần hỗ trợ..."
              rows={1}
              aria-label="Nội dung tin nhắn"
            />
            <Button type="button" variant="unstyled" className="customer-chat__send" onClick={() => sendMessage()} disabled={!input.trim()} aria-label="Gửi tin nhắn">
              <SendHorizontal size={17} />
            </Button>
          </div>
          <small>Nhấn Enter để gửi · Shift + Enter để xuống dòng</small>
        </footer>
      </section>
    </div>
  );
}
