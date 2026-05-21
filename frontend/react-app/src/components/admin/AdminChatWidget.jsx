import { useState } from "react";
import {
  MessageCircle,
  SendHorizontal,
  Maximize2,
  Minimize2,
  X,
} from "lucide-react";

const quickReplies = [
  "Đơn mới hôm nay",
  "Tồn kho thấp",
  "Doanh thu theo tuần",
];

export default function AdminChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState([
    {
      id: 1,
      sender: "bot",
      text: "Xin chào Admin, mình có thể hỗ trợ bạn tra cứu nhanh số liệu hệ thống.",
    },
  ]);

  function sendMessage(text) {
    const content = text.trim();
    if (!content) {
      return;
    }

    setMessages((prev) => [
      ...prev,
      { id: Date.now(), sender: "admin", text: content },
      {
        id: Date.now() + 1,
        sender: "bot",
        text: "Đây là phản hồi mẫu từ chatbot hệ thống (dữ liệu giả).",
      },
    ]);
    setInput("");
  }

  return (
    <div className="fixed bottom-6 right-6 z-50">
      {isOpen && (
        <div
          className={`mb-4 bg-zinc-900 border border-zinc-700 rounded-2xl shadow-2xl overflow-hidden transition-all ${
            isExpanded
              ? "w-[560px] h-[620px]"
              : "w-[370px] h-[520px]"
          }`}
        >
          <div className="h-14 px-4 bg-zinc-800 border-b border-zinc-700 flex items-center justify-between">
            <div>
              <p className="font-semibold text-zinc-100">
                Chatbot hệ thống
              </p>
              <p className="text-xs text-emerald-400">
                Online
              </p>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setIsExpanded((prev) => !prev)}
                className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-zinc-700 transition-colors"
                title={isExpanded ? "Thu nhỏ" : "Mở rộng"}
              >
                {isExpanded ? (
                  <Minimize2 size={16} />
                ) : (
                  <Maximize2 size={16} />
                )}
              </button>

              <button
                onClick={() => setIsOpen(false)}
                className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-zinc-700 transition-colors"
                title="Đóng chat"
              >
                <X size={16} />
              </button>
            </div>
          </div>

          <div className="h-[calc(100%-128px)] overflow-y-auto p-4 space-y-3">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${
                  message.sender === "admin"
                    ? "justify-end"
                    : "justify-start"
                }`}
              >
                <div
                  className={`max-w-[85%] px-3 py-2 rounded-2xl text-sm ${
                    message.sender === "admin"
                      ? "bg-amber-400 text-zinc-900 rounded-br-md"
                      : "bg-zinc-800 text-zinc-100 rounded-bl-md"
                  }`}
                >
                  {message.text}
                </div>
              </div>
            ))}
          </div>

          <div className="px-3 pb-3">
            <div className="flex flex-wrap gap-2 mb-2">
              {quickReplies.map((item) => (
                <button
                  key={item}
                  onClick={() => sendMessage(item)}
                  className="text-xs px-2.5 py-1.5 rounded-lg bg-zinc-800 hover:bg-zinc-700 transition-colors"
                >
                  {item}
                </button>
              ))}
            </div>

            <div className="flex items-center gap-2">
              <input
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    sendMessage(input);
                  }
                }}
                placeholder="Nhập câu hỏi cho chatbot..."
                className="flex-1 h-11 rounded-xl bg-zinc-800 border border-zinc-700 px-3 outline-none focus:border-amber-400 transition-colors text-sm"
              />

              <button
                onClick={() => sendMessage(input)}
                className="w-11 h-11 rounded-xl bg-amber-400 text-zinc-900 flex items-center justify-center hover:bg-amber-300 transition-colors"
              >
                <SendHorizontal size={18} />
              </button>
            </div>
          </div>
        </div>
      )}

      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className="w-14 h-14 rounded-full bg-amber-400 text-zinc-900 flex items-center justify-center shadow-lg hover:bg-amber-300 transition-colors"
        title="Mở chat bot"
      >
        <MessageCircle size={24} />
      </button>
    </div>
  );
}
