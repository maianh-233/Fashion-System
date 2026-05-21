import { MessageCircle, SendHorizontal, X } from "lucide-react";

export default function OrderChatDialog({
  order,
  chatHistory,
  chatMessage,
  onChatMessageChange,
  onSend,
  onClose,
}) {
  if (!order) {
    return null;
  }

  const quickReplies = [
    "Đơn đang được xử lý nhé bạn.",
    "Shop sẽ cập nhật mã vận đơn trong ít phút.",
    "Mình đã ghi nhận yêu cầu đổi size.",
  ];

  return (
    <div className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center p-4">
      <div className="w-full max-w-2xl h-[620px] bg-zinc-900 rounded-2xl border border-zinc-700 shadow-2xl overflow-hidden flex flex-col">
        <div className="h-14 px-4 bg-zinc-800 border-b border-zinc-700 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-amber-400 text-zinc-900 flex items-center justify-center">
              <MessageCircle size={18} />
            </div>
            <div>
              <p className="font-semibold text-zinc-100">Chat đơn {order.code}</p>
              <p className="text-xs text-emerald-400">{order.customer} • {order.unreadMessages} chưa đọc</p>
            </div>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-zinc-700 transition-colors">
            <X size={16} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-zinc-950/40">
          {(chatHistory[order.code] || []).map((msg) => (
            <div key={msg.id} className={`flex ${msg.sender === "staff" ? "justify-end" : "justify-start"}`}>
              <div
                className={`max-w-[85%] px-3 py-2 rounded-2xl text-sm ${
                  msg.sender === "staff"
                    ? "bg-amber-400 text-zinc-900 rounded-br-md"
                    : "bg-zinc-800 text-zinc-100 rounded-bl-md"
                }`}
              >
                {msg.content}
              </div>
            </div>
          ))}
        </div>

        <div className="px-3 py-3 border-t border-zinc-700 bg-zinc-900">
          <p className="text-xs text-zinc-400 mb-2">Nhập tin nhắn để trao đổi trực tiếp với khách hàng</p>
          <div className="flex flex-wrap gap-2 mb-2">
            {quickReplies.map((reply) => (
              <button
                key={reply}
                onClick={() => onChatMessageChange(reply)}
                className="text-xs px-2.5 py-1.5 rounded-lg bg-zinc-800 hover:bg-zinc-700 transition-colors"
              >
                {reply}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2">
            <textarea
              value={chatMessage}
              onChange={(e) => onChatMessageChange(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  onSend();
                }
              }}
              placeholder="Nhập nội dung phản hồi khách hàng..."
              rows={2}
              className="flex-1 min-h-[44px] max-h-28 rounded-xl bg-zinc-800 border border-zinc-700 px-3 py-2 outline-none focus:border-amber-400 transition-colors text-sm text-white placeholder-zinc-500 resize-none"
            />
            <button
              onClick={onSend}
              className="w-11 h-11 rounded-xl bg-amber-400 text-zinc-900 flex items-center justify-center hover:bg-amber-300 transition-colors"
            >
              <SendHorizontal size={18} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
