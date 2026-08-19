import Button from "../common/Button";
import { useState } from "react";
import {
  MessageCircle,
  SendHorizontal,
  Maximize2,
  Minimize2,
  X,
} from "lucide-react";

/* Gợi ý nhanh cho khách hàng */
const quickReplies = [
  "Sản phẩm bán chạy",
  "Chính sách đổi trả",
  "Thời gian giao hàng",
  "Tư vấn size giúp mình",
];

export default function CustomerChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState([
    {
      id: 1,
      sender: "bot",
      text: "Xin chào 👋 Lunaria có thể hỗ trợ gì cho bạn hôm nay?",
    },
  ]);

  function sendMessage(text) {
    const content = text.trim();
    if (!content) return;

    setMessages((prev) => [
      ...prev,
      {
        id: Date.now(),
        sender: "user",
        text: content,
      },
      {
        id: Date.now() + 1,
        sender: "bot",
        text:
          "Cảm ơn bạn đã liên hệ 💛 Nhân viên Lunaria sẽ phản hồi trong ít phút nhé!",
      },
    ]);

    setInput("");
  }

  return (
    <div className="fixed bottom-6 right-6 z-50">
      {/* CHAT BOX */}
      {isOpen && (
        <div
          className={`mb-4 bg-zinc-900 border border-zinc-800 rounded-2xl shadow-2xl overflow-hidden transition-all duration-300 ${
            isExpanded
              ? "w-[560px] h-[620px]"
              : "w-[360px] h-[520px]"
          }`}
        >
          {/* HEADER */}
          <div className="h-14 px-4 bg-zinc-800 border-b border-zinc-700 flex items-center justify-between">
            <div>
              <p className="font-semibold text-zinc-100">
                Lunaria Support
              </p>
              <p className="text-xs text-emerald-400">
                Đang trực tuyến
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Button
                onClick={() => setIsExpanded((prev) => !prev)}
                className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-zinc-700 transition-colors"
                title={isExpanded ? "Thu nhỏ" : "Mở rộng"}
              >
                {isExpanded ? (
                  <Minimize2 size={16} />
                ) : (
                  <Maximize2 size={16} />
                )}
              </Button>

              <Button
                onClick={() => setIsOpen(false)}
                className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-zinc-700 transition-colors"
                title="Đóng chat"
              >
                <X size={16} />
              </Button>
            </div>
          </div>

          {/* MESSAGES */}
          <div className="h-[calc(100%-128px)] overflow-y-auto p-4 space-y-3">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${
                  message.sender === "user"
                    ? "justify-end"
                    : "justify-start"
                }`}
              >
                <div
                  className={`max-w-[85%] px-3 py-2 rounded-2xl text-sm leading-relaxed ${
                    message.sender === "user"
                      ? "bg-orange-500 text-black rounded-br-md"
                      : "bg-zinc-800 text-zinc-100 rounded-bl-md"
                  }`}
                >
                  {message.text}
                </div>
              </div>
            ))}
          </div>

          {/* INPUT + QUICK REPLY */}
          <div className="px-3 pb-3">
            <div className="flex flex-wrap gap-2 mb-2">
              {quickReplies.map((item) => (
                <Button
                  key={item}
                  onClick={() => sendMessage(item)}
                  className="text-xs px-2.5 py-1.5 rounded-lg bg-zinc-800 hover:bg-zinc-700 transition-colors"
                >
                  {item}
                </Button>
              ))}
            </div>

            <div className="flex items-center gap-2">
              <input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") sendMessage(input);
                }}
                placeholder="Nhập tin nhắn..."
                className="
                  flex-1 h-11 rounded-xl bg-zinc-800
                  border border-zinc-700 px-3
                  outline-none focus:border-orange-500
                  transition-colors text-sm
                "
              />

              <Button
                onClick={() => sendMessage(input)}
                className="
                  w-11 h-11 rounded-xl
                  bg-orange-500 text-black
                  flex items-center justify-center
                  hover:bg-orange-400 transition-colors
                "
              >
                <SendHorizontal size={18} />
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* FLOATING BUTTON */}
      <Button
        onClick={() => setIsOpen((prev) => !prev)}
        className="
          w-14 h-14 rounded-full
          bg-orange-500 text-black
          flex items-center justify-center
          shadow-lg hover:bg-orange-400
          transition-colors
        "
        title="Chat với Lunaria"
      >
        <MessageCircle size={24} />
      </Button>
    </div>
  );
}