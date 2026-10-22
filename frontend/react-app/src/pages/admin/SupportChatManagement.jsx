import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Bot, CheckCircle2, MessageCircle, Send, Store, UserCheck } from "lucide-react";
import AdminCatalogPageHeader from "../../components/admin/common/AdminCatalogPageHeader";
import Button from "../../components/common/Button";
import StoreSelector from "../../components/common/StoreSelector";
import { useSystemNotification } from "../../components/common/SystemNotification";
import { useAdminAuth } from "../../contexts/AdminAuthContext";
import { useAdminPermissions } from "../../contexts/AdminPermissionsContext";
import useStoreOptions from "../../hooks/useStoreOptions";
import { useRealtime } from "../../realtime/RealtimeContext";
import { supportApi } from "../../api/supportApi";
import { STATUS_STYLES, SUPPORT_TABS, bubbleSide, mergeMessages, roomActions } from "./supportChatLogic";
import "./SupportChat.css";

const time = value => value ? new Date(value).toLocaleString("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit" }) : "";

function FaqEditor({ canEdit }) {
  const notification = useSystemNotification();
  const [faqs, setFaqs] = useState(null);
  const [drafts, setDrafts] = useState({});
  const [error, setError] = useState("");
  const [saving, setSaving] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    supportApi.faqs({ signal: controller.signal })
      .then(list => { setFaqs(list); setDrafts(Object.fromEntries(list.map(f => [f.id, f]))); })
      .catch(failure => { if (!controller.signal.aborted) setError(failure.message); });
    return () => controller.abort();
  }, []);

  const save = async id => {
    setSaving(id);
    try {
      const draft = drafts[id];
      const updated = await supportApi.updateFaq(id, { title: draft.title, answer: draft.answer, keywords: draft.keywords, active: draft.active });
      setFaqs(current => current.map(f => f.id === id ? updated : f));
      notification.success("Đã lưu câu trả lời của bot.");
    } catch (failure) {
      notification.error(failure.message);
    } finally {
      setSaving("");
    }
  };

  if (error) return <p className="support-chat__error">{error}</p>;
  if (!faqs) return <p className="support-chat__muted">Đang tải…</p>;
  return <div className="support-faq">
    <p className="support-chat__muted">Bot dùng các câu trả lời này khi khách hỏi về chính sách. Từ khóa (cách nhau bằng dấu phẩy) giúp bot nhận ra câu hỏi khách tự gõ.{canEdit ? "" : " Bạn chỉ có quyền xem."}</p>
    {faqs.map(faq => {
      const draft = drafts[faq.id] || faq;
      const set = patch => setDrafts(current => ({ ...current, [faq.id]: { ...draft, ...patch } }));
      return <form key={faq.id} className="support-faq__item" onSubmit={event => { event.preventDefault(); save(faq.id); }}>
        <input className="support-chat__input" aria-label="Tiêu đề" value={draft.title} disabled={!canEdit} maxLength={200} onChange={e => set({ title: e.target.value })} />
        <textarea className="support-chat__input" rows={3} aria-label="Câu trả lời" value={draft.answer} disabled={!canEdit} maxLength={4000} onChange={e => set({ answer: e.target.value })} />
        <input className="support-chat__input" aria-label="Từ khóa" placeholder="Từ khóa" value={draft.keywords || ""} disabled={!canEdit} maxLength={500} onChange={e => set({ keywords: e.target.value })} />
        <div className="support-faq__footer">
          <label><input type="checkbox" checked={draft.active} disabled={!canEdit} onChange={e => set({ active: e.target.checked })} /> Đang dùng</label>
          {canEdit && <Button type="submit" variant="primary" size="sm" loading={saving === faq.id}>Lưu</Button>}
        </div>
      </form>;
    })}
  </div>;
}

/**
 * Hỗ trợ đơn hàng: bot trả lời theo kịch bản, khách cần thì chuyển cho nhân viên cửa hàng. Nhân viên nhận hội thoại,
 * trả lời và kết thúc; tin mới đến realtime qua kênh SUPPORT_CHAT.
 */
export default function SupportChatManagement() {
  const notification = useSystemNotification();
  const { user } = useAdminAuth();
  const { isGlobal, currentStoreId, currentStoreName, hasPermission, loading: permissionsLoading } = useAdminPermissions();
  const { stores, loading: storesLoading } = useStoreOptions({ endpoint: "/api/admin/orders/stores", enabled: !permissionsLoading });
  const { subscribe } = useRealtime();
  const [searchParams, setSearchParams] = useSearchParams();
  const [storeId, setStoreId] = useState("");
  const [view, setView] = useState("chats");
  const [tab, setTab] = useState("waiting");
  const [rooms, setRooms] = useState(null);
  const [roomsError, setRoomsError] = useState("");
  const [revision, setRevision] = useState(0);
  const [conversation, setConversation] = useState(null);
  const [draft, setDraft] = useState("");
  const [busy, setBusy] = useState("");
  const endRef = useRef(null);

  const effectiveStoreId = isGlobal ? storeId : currentStoreId || stores[0]?.id || "";
  const openRoomId = searchParams.get("room");
  const statuses = SUPPORT_TABS.find(t => t.key === tab).statuses;

  useEffect(() => {
    if (!effectiveStoreId) return undefined;
    const controller = new AbortController();
    supportApi.rooms({ storeId: effectiveStoreId, statuses }, { signal: controller.signal })
      .then(page => { setRooms(page.content || []); setRoomsError(""); })
      .catch(failure => { if (!controller.signal.aborted) setRoomsError(failure.message); });
    return () => controller.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [effectiveStoreId, tab, revision]);

  useEffect(() => {
    if (!openRoomId) return undefined;
    const controller = new AbortController();
    supportApi.conversation(openRoomId, { signal: controller.signal })
      .then(setConversation)
      .catch(failure => { if (!controller.signal.aborted) notification.error(failure.message); });
    return () => controller.abort();
  }, [openRoomId, notification]);

  // Tin mới: thêm vào hội thoại đang mở và làm mới danh sách.
  useEffect(() => subscribe({ channel: "SUPPORT_CHAT", eventType: "MESSAGE_CREATED" }, event => {
    const payload = event?.payload;
    if (!payload) return;
    setConversation(current => current && current.room.id === payload.roomId
      ? { room: payload.room, messages: mergeMessages(current.messages, [payload.message]) } : current);
    setRevision(value => value + 1);
  }), [subscribe]);

  useEffect(() => { endRef.current?.scrollIntoView({ block: "end" }); }, [conversation?.messages?.length]);

  const openRoom = id => setSearchParams(current => { const next = new URLSearchParams(current); next.set("room", id); return next; });

  const run = async (key, action) => {
    setBusy(key);
    try {
      const updated = await action();
      setConversation(updated);
      setRevision(value => value + 1);
      return true;
    } catch (failure) {
      notification.error(failure.message);
      return false;
    } finally {
      setBusy("");
    }
  };

  const send = async event => {
    event.preventDefault();
    const text = draft.trim();
    if (!text) return;
    if (await run("reply", () => supportApi.reply(conversation.room.id, text))) setDraft("");
  };

  const closeRoom = async () => {
    if (!await notification.confirm("Kết thúc cuộc trò chuyện này? Khách nhắn tiếp thì bot sẽ hỗ trợ lại.", { confirmText: "Kết thúc" })) return;
    run("close", () => supportApi.close(conversation.room.id));
  };

  const room = conversation?.room;
  const actions = roomActions(room, user?.id);
  const storeName = isGlobal ? stores.find(s => s.id === storeId)?.name : currentStoreName || stores[0]?.name;

  return <div className="admin-catalog-page support-chat">
    <AdminCatalogPageHeader icon={MessageCircle} eyebrow="Vận hành bán hàng" title="Hỗ trợ đơn hàng"
      description="Bot trả lời khách theo kịch bản; khi khách cần gặp nhân viên, hội thoại được chuyển cho cửa hàng xử lý đơn." />

    <div className="support-chat__toolbar">
      {isGlobal
        ? <StoreSelector stores={stores} value={storeId} onChange={value => { setStoreId(value); setRooms(null); }} required loading={storesLoading} label="Cửa hàng" placeholder="Chọn cửa hàng" />
        : <span className="support-chat__muted inline-flex items-center gap-2"><Store size={16} /> {storeName || "Cửa hàng của bạn"}</span>}
      <div className="support-chat__tabs" role="tablist">
        <Button role="tab" aria-selected={view === "chats"} variant={view === "chats" ? "primary" : "secondary"} onClick={() => setView("chats")}>Hội thoại</Button>
        <Button role="tab" aria-selected={view === "faq"} variant={view === "faq" ? "primary" : "secondary"} onClick={() => setView("faq")}><Bot size={16} /> Câu trả lời của bot</Button>
      </div>
    </div>

    {view === "faq" ? <section className="support-chat__panel"><FaqEditor canEdit={hasPermission("SUPPORT_FAQ_MANAGE") && isGlobal} /></section>
      : !effectiveStoreId ? <section className="support-chat__panel support-chat__muted">{isGlobal ? "Chọn cửa hàng để xem hội thoại." : "Đang tải cửa hàng…"}</section>
        : <div className="support-chat__layout">
          <section className="support-chat__panel support-chat__list">
            <div className="support-chat__tabs">
              {SUPPORT_TABS.map(t => <button type="button" key={t.key} className={`support-chat__tab ${tab === t.key ? "is-active" : ""}`}
                onClick={() => { setTab(t.key); setRooms(null); }}>{t.label}</button>)}
            </div>
            {roomsError && <p className="support-chat__error">{roomsError}</p>}
            {!rooms && !roomsError && <p className="support-chat__muted">Đang tải…</p>}
            {rooms && !rooms.length && <p className="support-chat__muted">Không có hội thoại.</p>}
            {rooms?.map(r => <button type="button" key={r.id} className={`support-chat__room ${room?.id === r.id ? "is-active" : ""}`} onClick={() => openRoom(r.id)}>
              <span className="support-chat__room-head">
                <strong>{r.customerName || "Khách hàng"}</strong>
                {r.staffUnread > 0 && <span className="support-chat__badge">{r.staffUnread}</span>}
              </span>
              <span className="support-chat__muted">Đơn {r.orderCode} · {time(r.lastMessageAt)}</span>
              <span className="support-chat__preview">{r.lastMessage}</span>
              <span className={`support-chat__status ${STATUS_STYLES[r.status]}`}>{r.statusLabel}{r.assignedStaff ? ` · ${r.assignedStaff.name}` : ""}</span>
            </button>)}
          </section>

          <section className="support-chat__panel support-chat__conversation">
            {!conversation ? <p className="support-chat__muted">Chọn một hội thoại để xem.</p> : <>
              <header className="support-chat__conversation-head">
                <div>
                  <strong>{room.customerName || "Khách hàng"}</strong>
                  <span className="support-chat__muted"> · {room.customerPhone} · <Link className="support-chat__link" to={`/admin/orders?order=${room.orderId}`}>Đơn {room.orderCode}</Link></span>
                  <div><span className={`support-chat__status ${STATUS_STYLES[room.status]}`}>{room.statusLabel}{room.assignedStaff ? ` · ${room.assignedStaff.name}` : ""}</span></div>
                </div>
                <div className="flex flex-wrap gap-2">
                  {actions.canClaim && <Button variant="primary" size="sm" loading={busy === "claim"} onClick={() => run("claim", () => supportApi.claim(room.id))}><UserCheck size={14} /> Nhận hỗ trợ</Button>}
                  {actions.canClose && <Button variant="secondary" size="sm" loading={busy === "close"} onClick={closeRoom}><CheckCircle2 size={14} /> Kết thúc</Button>}
                </div>
              </header>
              <ol className="support-chat__messages">
                {conversation.messages.map(m => <li key={m.id} className={`support-chat__message is-${bubbleSide(m.senderType)} is-${m.senderType.toLowerCase()}`}>
                  <span className="support-chat__sender">{m.senderName} · {time(m.createdAt)}</span>
                  <p>{m.content}</p>
                  {m.options?.length > 0 && <div className="support-chat__options">{m.options.map(o => <span key={o.code}>{o.label}</span>)}</div>}
                </li>)}
                <li ref={endRef} aria-hidden="true" />
              </ol>
              {actions.takenByOther && <p className="support-chat__muted">Hội thoại đang được {room.assignedStaff.name} hỗ trợ.</p>}
              {actions.canReply && <form className="support-chat__composer" onSubmit={send}>
                <textarea className="support-chat__input" rows={2} maxLength={2000} placeholder={room.status === "STAFF_HANDLING" ? "Nhập tin nhắn cho khách" : "Trả lời để nhận hỗ trợ khách"}
                  aria-label="Tin nhắn" value={draft} onChange={e => setDraft(e.target.value)}
                  onKeyDown={e => { if (e.key === "Enter" && !e.shiftKey) send(e); }} />
                <Button type="submit" variant="primary" loading={busy === "reply"} disabled={!draft.trim()} aria-label="Gửi"><Send size={16} /></Button>
              </form>}
            </>}
          </section>
        </div>}
  </div>;
}
