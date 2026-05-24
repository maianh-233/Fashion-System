export default function ChatMessage({ from, text }) {
  const isUser = from === "user";

  return (
    <div className={`flex ${isUser ? "justify-end" : "justify-start"}`}>
      <div
        className={`
          chat-bubble
          px-4 py-3
          rounded-3xl
          ${isUser
            ? "bg-amber-400 text-black rounded-tr-none"
            : "bg-zinc-800 text-zinc-200 rounded-tl-none"}
        `}
      >
        {text}
      </div>
    </div>
  );
}