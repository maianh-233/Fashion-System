import Button from "../../common/Button";
import { MessageCircle, SendHorizontal, X } from "lucide-react";
import { useState } from "react";

export default function ChatModal({ orderId, onClose }) {
  const [messages, setMessages] = useState([
    {
      from: "admin",
      text: `Xin chào! Bạn cần hỗ trợ gì về đơn hàng ${orderId}?`,
    },
  ]);
  const [input, setInput] = useState("");

  const quickReplies = [
    "Đơn đang được xử lý nhé bạn.",
    "Shop sẽ cập nhật mã vận đơn trong ít phút.",
    "Mình đã ghi nhận yêu cầu đổi size.",
  ];

  const sendMessage = () => {
    if (!input.trim()) return;

    const payload = input;
    setMessages((prev) => [...prev, { from: "user", text: payload }]);
    setInput("");

    setTimeout(() => {
      setMessages((prev) => [
        ...prev,
        { from: "admin", text: "Nhân viên sẽ phản hồi sớm nhất." },
      ]);
    }, 800);
  };

  return (
    <div className="fixed inset-0 bg-black/80 z-50 flex items-end justify-center sm:items-center sm:p-4">
      <div className="w-full max-w-2xl h-[min(88dvh,620px)] bg-zinc-900 rounded-t-2xl border border-zinc-700 shadow-2xl overflow-hidden flex flex-col sm:rounded-2xl">
        <div className="h-14 px-4 bg-zinc-800 border-b border-zinc-700 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-amber-400 text-zinc-900 flex items-center justify-center">
              <MessageCircle size={18} />
            </div>
            <div>
              <p className="font-semibold text-zinc-100">Chat đơn {orderId}</p>
              <p className="text-xs text-emerald-400">Khách hàng • 0 chưa đọc</p>
            </div>
          </div>
          <Button
            onClick={onClose}
            className="w-11 h-11 rounded-xl flex items-center justify-center hover:bg-zinc-700 transition-colors"
            aria-label="Đóng cửa sổ chat"
          >
            <X size={16} />
          </Button>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-zinc-950/40">
          {messages.map((m, i) => (
            <div
              key={i}
              className={`flex ${m.from === "user" ? "justify-end" : "justify-start"}`}
            >
              <div
                className={`max-w-[85%] px-3 py-2 rounded-2xl text-sm ${
                  m.from === "user"
                    ? "bg-amber-400 text-zinc-900 rounded-br-md"
                    : "bg-zinc-800 text-zinc-100 rounded-bl-md"
                }`}
              >
                {m.text}
              </div>
            </div>
          ))}
        </div>

        <div className="px-3 py-3 border-t border-zinc-700 bg-zinc-900">
          <p className="text-xs text-zinc-400 mb-2">
            Nhập tin nhắn để trao đổi trực tiếp với khách hàng
          </p>
          <div className="flex flex-wrap gap-2 mb-2">
            {quickReplies.map((reply) => (
              <Button
                key={reply}
                onClick={() => {
                  // gửi ngay (không phụ thuộc state update bất đồng bộ)
                  setMessages((prev) => [...prev, { from: "user", text: reply }]);

                  setTimeout(() => {
                    setMessages((prev) => [
                      ...prev,
                      { from: "admin", text: "Nhân viên sẽ phản hồi sớm nhất." },
                    ]);
                  }, 800);
                }}
                className="text-xs px-2.5 py-1.5 rounded-lg bg-zinc-800 hover:bg-zinc-700 transition-colors"
              >
                {reply}
              </Button>
            ))}
          </div>

          <div className="flex items-center gap-2">
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  sendMessage();
                }
              }}
              placeholder="Nhập nội dung phản hồi khách hàng..."
              rows={2}
              className="flex-1 min-h-[44px] max-h-28 rounded-xl bg-zinc-800 border border-zinc-700 px-3 py-2 outline-none focus:border-amber-400 transition-colors text-sm text-white placeholder-zinc-500 resize-none"
            />
            <Button
              onClick={sendMessage}
              className="w-11 h-11 rounded-xl bg-amber-400 text-zinc-900 flex items-center justify-center hover:bg-amber-300 transition-colors"
            >
              <SendHorizontal size={18} />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
