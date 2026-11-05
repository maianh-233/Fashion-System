import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bot, ExternalLink, MessageCircleQuestionMark, RotateCcw, Send, X } from "lucide-react";
import { useOptionalAdminPermissions } from "../../../contexts/AdminPermissionsContext";
import { ASSISTANT_NAME, answerQuestion, welcomeMessage } from "./adminAssistantKnowledge";

let nextMessageId = 0;
const botMessage = (reply) => ({ id: ++nextMessageId, from: "bot", ...reply });
const userMessage = (text) => ({ id: ++nextMessageId, from: "user", text });

/** Khung chat nổi ở góc màn hình quản trị, trả lời theo kịch bản cố định. */
export default function AdminAssistant() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState(() => [botMessage(welcomeMessage())]);
  const [draft, setDraft] = useState("");
  const listRef = useRef(null);
  const inputRef = useRef(null);
  const navigate = useNavigate();
  const permissions = useOptionalAdminPermissions();
  const hasPermission = permissions?.loading ? undefined : permissions?.hasPermission;

  useEffect(() => {
    if (!open) return;
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, open]);

  useEffect(() => {
    if (open) inputRef.current?.focus();
  }, [open]);

  const ask = (text, topicId) => {
    const question = text.trim();
    if (!question) return;
    setMessages((current) => [...current, userMessage(question), botMessage(answerQuestion(question, { topicId, hasPermission }))]);
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    ask(draft);
    setDraft("");
  };

  const reset = () => setMessages([botMessage(welcomeMessage())]);

  const openLink = (path) => {
    navigate(path);
    if (window.matchMedia("(max-width: 640px)").matches) setOpen(false);
  };

  return (
    <div className="admin-assistant">
      {open && (
        <section className="admin-assistant__panel" role="dialog" aria-label={ASSISTANT_NAME}
          onKeyDown={(event) => event.key === "Escape" && setOpen(false)}>
          <header className="admin-assistant__header">
            <span className="admin-assistant__avatar"><Bot size={18} /></span>
            <div className="admin-assistant__title">
              <strong>{ASSISTANT_NAME}</strong>
              <small>Hướng dẫn sử dụng hệ thống</small>
            </div>
            <button type="button" className="admin-assistant__icon-button" onClick={reset} title="Bắt đầu lại" aria-label="Bắt đầu lại">
              <RotateCcw size={15} />
            </button>
            <button type="button" className="admin-assistant__icon-button" onClick={() => setOpen(false)} title="Đóng" aria-label="Đóng">
              <X size={16} />
            </button>
          </header>

          <div ref={listRef} className="admin-assistant__messages" aria-live="polite">
            {messages.map((message) => (
              <div key={message.id} className={`admin-assistant__message admin-assistant__message--${message.from}`}>
                <p>{message.text}</p>
                {message.link && (
                  <button type="button" className="admin-assistant__link" onClick={() => openLink(message.link.path)}>
                    <ExternalLink size={13} />{message.link.label}
                  </button>
                )}
                {message.suggestions?.length > 0 && (
                  <div className="admin-assistant__suggestions">
                    {message.suggestions.map((suggestion) => (
                      <button key={suggestion.id} type="button" onClick={() => ask(suggestion.title, suggestion.id)}>
                        {suggestion.title}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>

          <form className="admin-assistant__form" onSubmit={handleSubmit}>
            <input ref={inputRef} value={draft} onChange={(event) => setDraft(event.target.value)} maxLength={300}
              placeholder="Hỏi về chức năng của hệ thống..." aria-label="Câu hỏi" />
            <button type="submit" disabled={!draft.trim()} aria-label="Gửi"><Send size={15} /></button>
          </form>
        </section>
      )}

      <button type="button" className="admin-assistant__toggle" onClick={() => setOpen((value) => !value)}
        aria-expanded={open} aria-label={open ? "Đóng trợ lý" : `Mở ${ASSISTANT_NAME}`} title={ASSISTANT_NAME}>
        {open ? <X size={20} /> : <MessageCircleQuestionMark size={21} />}
      </button>
    </div>
  );
}
