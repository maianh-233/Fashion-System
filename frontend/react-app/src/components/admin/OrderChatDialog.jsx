import Button from "../common/Button";
import { SendHorizontal } from "lucide-react";
import AdminDialog, {
  AdminDialogBody,
  AdminDialogHeader,
} from "./common/AdminDialog";

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
    <AdminDialog open onClose={onClose} size="md" className="h-[620px]">
        <AdminDialogHeader
          title={`Chat đơn ${order.code}`}
          description={`${order.customer} • ${order.unreadMessages} chưa đọc`}
          onClose={onClose}
        />

        <AdminDialogBody className="space-y-3 bg-zinc-950/40">
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
        </AdminDialogBody>

        <div className="px-3 py-3 border-t border-zinc-700 bg-zinc-900">
          <p className="text-xs text-zinc-400 mb-2">Nhập tin nhắn để trao đổi trực tiếp với khách hàng</p>
          <div className="flex flex-wrap gap-2 mb-2">
            {quickReplies.map((reply) => (
              <Button
                key={reply}
                onClick={() => onChatMessageChange(reply)}
                className="text-xs px-2.5 py-1.5 rounded-lg bg-zinc-800 hover:bg-zinc-700 transition-colors"
              >
                {reply}
              </Button>
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
            <Button
              onClick={onSend}
              className="w-11 h-11 rounded-xl bg-amber-400 text-zinc-900 flex items-center justify-center hover:bg-amber-300 transition-colors"
            >
              <SendHorizontal size={18} />
            </Button>
          </div>
        </div>
    </AdminDialog>
  );
}
