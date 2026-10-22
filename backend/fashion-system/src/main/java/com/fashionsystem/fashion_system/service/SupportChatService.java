package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.OrderDetailResponse;
import com.fashionsystem.fashion_system.dto.order.SupportChatDtos;
import com.fashionsystem.fashion_system.entity.ChatSenderType;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderChatMessage;
import com.fashionsystem.fashion_system.entity.OrderChatRoom;
import com.fashionsystem.fashion_system.entity.Shipment;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.entity.SupportChatStatus;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.OrderChatMessageRepository;
import com.fashionsystem.fashion_system.repository.OrderChatRoomRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.ShipmentRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.SupportFaqRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hội thoại hỗ trợ đơn hàng: bot theo kịch bản trả lời trước, khi khách cần (hoặc bot không trả lời được) thì
 * chuyển cho nhân viên của cửa hàng xử lý đơn. Với đơn bị tách, nhân viên của bất kỳ cửa hàng nào giao một phần
 * đều nhận được hội thoại. Bot không thao tác trên đơn.
 */
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(
        reason = "Support chat transcript is its own record; it never changes orders, stock or payments")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportChatService {
    private static final int HISTORY_SIZE = 200;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final OrderChatRoomRepository rooms;
    private final OrderChatMessageRepository messages;
    private final OrderRepository orders;
    private final ShipmentRepository shipments;
    private final StoreRepository stores;
    private final SupportFaqRepository faqs;
    private final CustomerRepository customers;
    private final UserRepository users;
    private final StoreAccessService storeAccess;
    private final SupportBot bot;
    private final ApplicationEventPublisher events;

    // ----------------------------------------------------------- khách

    /** Mở (hoặc tạo) hội thoại cho đơn của khách; lần đầu bot chào và đưa menu. */
    @Transactional
    public SupportChatDtos.Conversation open(UUID customerId, UUID orderId) {
        Order top = customerOrder(customerId, orderId);
        OrderChatRoom room = rooms.findByOrderId(top.getId()).orElse(null);
        if (room == null) {
            LocalDateTime now = LocalDateTime.now();
            room = rooms.save(OrderChatRoom.builder().orderId(top.getId()).customerId(customerId)
                    .status(SupportChatStatus.BOT_HANDLING).createdAt(now).lastMessageAt(now).build());
            OrderChatMessage welcome = botMessage(room, bot.welcome(context(top)), null);
            publish(room, top, welcome, false, false);
        } else if (room.getCustomerUnreadCount() > 0) {
            room.setCustomerUnreadCount(0);
            rooms.save(room);
        }
        return conversation(room, top);
    }

    public SupportChatDtos.Conversation customerConversation(UUID customerId, UUID roomId) {
        OrderChatRoom room = rooms.findById(roomId).filter(r -> customerId.equals(r.getCustomerId()))
                .orElseThrow(() -> BusinessException.notFound("Hội thoại không tồn tại"));
        return conversation(room, orders.findById(room.getOrderId()).orElseThrow());
    }

    /** Khách gửi tin: bot trả lời khi đang ở chế độ bot, ngược lại tin được chuyển tới nhân viên. */
    @Transactional
    public SupportChatDtos.Conversation customerSend(UUID customerId, UUID roomId, SupportChatDtos.CustomerSend request) {
        String option = blankToNull(request.optionCode());
        String text = blankToNull(request.text());
        if (option == null && text == null) throw BusinessException.badRequest("Vui lòng nhập nội dung hoặc chọn một mục");
        OrderChatRoom room = rooms.findByIdForUpdate(roomId).filter(r -> customerId.equals(r.getCustomerId()))
                .orElseThrow(() -> BusinessException.notFound("Hội thoại không tồn tại"));
        Order top = orders.findById(room.getOrderId()).orElseThrow();
        SupportBot.Context context = context(top);
        String label = option == null ? text : bot.menu(context).stream().filter(o -> o.code().equals(option))
                .map(SupportBot.Option::label).findFirst().orElseThrow(() -> BusinessException.badRequest("Lựa chọn không hợp lệ"));
        OrderChatMessage customerMessage = save(room, ChatSenderType.CUSTOMER, customerId, label, option, null);
        room.setCustomerUnreadCount(0);

        // Khách nhắn vào hội thoại đã kết thúc: bot mở lại để hỗ trợ tiếp.
        if (room.getStatus() == SupportChatStatus.CLOSED) {
            room.setStatus(SupportChatStatus.BOT_HANDLING);
            room.setAssignedStaffId(null);
            room.setClosedAt(null);
            room.setClosedBy(null);
        }
        if (room.getStatus() == SupportChatStatus.BOT_HANDLING) {
            publish(room, top, customerMessage, false, false);
            SupportBot.Reply reply = bot.answer(option, option == null ? text : null, context);
            OrderChatMessage botReply = botMessage(room, reply, null);
            boolean escalated = reply.escalate();
            if (escalated) {
                room.setStatus(SupportChatStatus.WAITING_STAFF);
                room.setEscalatedAt(LocalDateTime.now());
                room.setStaffUnreadCount(room.getStaffUnreadCount() + 1);
            }
            rooms.save(room);
            publish(room, top, botReply, escalated, false);
        } else {
            boolean firstUnread = room.getStaffUnreadCount() == 0;
            room.setStaffUnreadCount(room.getStaffUnreadCount() + 1);
            rooms.save(room);
            publish(room, top, customerMessage, false, firstUnread);
        }
        return conversation(room, top);
    }

    // ----------------------------------------------------------- nhân viên

    public Page<SupportChatDtos.Room> list(UUID actor, UUID storeId, List<SupportChatStatus> statuses, Pageable pageable) {
        UUID store = storeAccess.require(actor, storeId, OrderPermissions.SUPPORT_CHAT);
        List<SupportChatStatus> effective = statuses == null || statuses.isEmpty()
                ? List.of(SupportChatStatus.WAITING_STAFF, SupportChatStatus.STAFF_HANDLING, SupportChatStatus.BOT_HANDLING)
                : statuses;
        Page<OrderChatRoom> page = rooms.findForStore(store, effective,
                PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 50)));
        return page.map(room -> roomView(room, orders.findById(room.getOrderId()).orElseThrow()));
    }

    /** Nhân viên mở hội thoại; người đang nhận (hoặc khi chưa ai nhận) đọc thì xóa số tin chưa đọc. */
    @Transactional
    public SupportChatDtos.Conversation staffConversation(UUID actor, UUID roomId) {
        OrderChatRoom room = rooms.findByIdForUpdate(roomId)
                .orElseThrow(() -> BusinessException.notFound("Hội thoại không tồn tại"));
        Order top = requireRoomAccess(actor, room);
        if (room.getStaffUnreadCount() > 0 && (room.getAssignedStaffId() == null || actor.equals(room.getAssignedStaffId()))) {
            room.setStaffUnreadCount(0);
            rooms.save(room);
        }
        return conversation(room, top);
    }

    @Transactional
    public SupportChatDtos.Conversation claim(UUID actor, UUID roomId) {
        OrderChatRoom room = rooms.findByIdForUpdate(roomId)
                .orElseThrow(() -> BusinessException.notFound("Hội thoại không tồn tại"));
        Order top = requireRoomAccess(actor, room);
        assign(actor, room, top);
        return conversation(room, top);
    }

    /** Trả lời khách; hội thoại chưa ai nhận thì người trả lời tự nhận. */
    @Transactional
    public SupportChatDtos.Conversation reply(UUID actor, UUID roomId, String text) {
        OrderChatRoom room = rooms.findByIdForUpdate(roomId)
                .orElseThrow(() -> BusinessException.notFound("Hội thoại không tồn tại"));
        Order top = requireRoomAccess(actor, room);
        if (!actor.equals(room.getAssignedStaffId()) || room.getStatus() != SupportChatStatus.STAFF_HANDLING)
            assign(actor, room, top);
        OrderChatMessage message = save(room, ChatSenderType.STAFF, actor, text.trim(), null, null);
        room.setCustomerUnreadCount(room.getCustomerUnreadCount() + 1);
        room.setStaffUnreadCount(0);
        rooms.save(room);
        publish(room, top, message, false, false);
        return conversation(room, top);
    }

    @Transactional
    public SupportChatDtos.Conversation close(UUID actor, UUID roomId) {
        OrderChatRoom room = rooms.findByIdForUpdate(roomId)
                .orElseThrow(() -> BusinessException.notFound("Hội thoại không tồn tại"));
        Order top = requireRoomAccess(actor, room);
        if (room.getStatus() == SupportChatStatus.CLOSED) throw BusinessException.invalidState("Hội thoại đã kết thúc");
        if (room.getStatus() == SupportChatStatus.STAFF_HANDLING && !actor.equals(room.getAssignedStaffId()))
            throw BusinessException.conflict("Hội thoại đang được nhân viên khác hỗ trợ");
        LocalDateTime now = LocalDateTime.now();
        room.setStatus(SupportChatStatus.CLOSED);
        room.setClosedAt(now);
        room.setClosedBy(actor);
        room.setStaffUnreadCount(0);
        OrderChatMessage message = save(room, ChatSenderType.SYSTEM, null,
                "Cuộc trò chuyện đã kết thúc. Bạn có thể nhắn tiếp bất cứ lúc nào nếu cần hỗ trợ thêm.", "CLOSED", null);
        rooms.save(room);
        publish(room, top, message, false, false);
        return conversation(room, top);
    }

    /** Hội thoại của đơn (để chi tiết đơn liên kết sang màn hình hỗ trợ); null nếu khách chưa nhắn. */
    public UUID roomIdOf(Order storeOrder) {
        UUID top = storeOrder.getParentOrderId() == null ? storeOrder.getId() : storeOrder.getParentOrderId();
        return rooms.findByOrderId(top).map(OrderChatRoom::getId).orElse(null);
    }

    // ----------------------------------------------------------- nội bộ

    private void assign(UUID actor, OrderChatRoom room, Order top) {
        if (room.getStatus() == SupportChatStatus.CLOSED) throw BusinessException.invalidState("Hội thoại đã kết thúc");
        if (room.getStatus() == SupportChatStatus.STAFF_HANDLING && room.getAssignedStaffId() != null
                && !actor.equals(room.getAssignedStaffId()))
            throw BusinessException.conflict("Hội thoại đang được nhân viên khác hỗ trợ");
        if (room.getStatus() == SupportChatStatus.STAFF_HANDLING && actor.equals(room.getAssignedStaffId())) return;
        room.setStatus(SupportChatStatus.STAFF_HANDLING);
        room.setAssignedStaffId(actor);
        room.setAssignedAt(LocalDateTime.now());
        room.setStaffUnreadCount(0);
        OrderChatMessage joined = save(room, ChatSenderType.SYSTEM, null,
                "Nhân viên " + userName(actor) + " đã tham gia hỗ trợ bạn.", "STAFF_JOINED", null);
        rooms.save(room);
        publish(room, top, joined, false, false);
    }

    /** Nhân viên phải có quyền hỗ trợ tại một trong các cửa hàng giao đơn này. */
    private Order requireRoomAccess(UUID actor, OrderChatRoom room) {
        Order top = orders.findById(room.getOrderId()).orElseThrow();
        boolean allowed = storeIds(top).stream().anyMatch(store -> storeAccess.allows(actor, store, OrderPermissions.SUPPORT_CHAT));
        if (!allowed) throw BusinessException.forbidden("Bạn không có quyền hỗ trợ hội thoại của cửa hàng khác");
        return top;
    }

    private List<UUID> storeIds(Order top) {
        if (top.getStoreId() != null) return List.of(top.getStoreId());
        return orders.findAllByParentOrderId(top.getId()).stream().map(Order::getStoreId).distinct().toList();
    }

    private Order customerOrder(UUID customerId, UUID orderId) {
        Order order = orders.findById(orderId).filter(o -> customerId.equals(o.getCustomerId()))
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        return order.getParentOrderId() == null ? order : orders.findById(order.getParentOrderId()).orElseThrow();
    }

    /** Những gì bot được phép biết: trạng thái từng phần, chuyến giao mới nhất và câu trả lời chính sách. */
    SupportBot.Context context(Order top) {
        List<Order> parts = top.getStoreId() != null ? List.of(top)
                : orders.findAllByParentOrderId(top.getId()).stream().sorted(Comparator.comparing(Order::getOrderCode)).toList();
        Map<UUID, String> storeNames = stores.findAllById(parts.stream().map(Order::getStoreId).toList()).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));
        List<SupportBot.Part> botParts = new ArrayList<>();
        for (Order part : parts) {
            Shipment latest = shipments.findAllByOrderIdOrderByCreatedAtDesc(part.getId()).stream().findFirst().orElse(null);
            botParts.add(new SupportBot.Part(part.getOrderCode(), storeNames.getOrDefault(part.getStoreId(), ""),
                    part.getStatus(), latest == null ? null : new SupportBot.Shipment(latest.getShippingProvider(),
                    latest.getStatus(), latest.getTrackingUrl(), latest.getDriverName(), latest.getDriverPlate())));
        }
        Map<com.fashionsystem.fashion_system.entity.SupportTopic, SupportBot.Faq> faqMap =
                new EnumMap<>(com.fashionsystem.fashion_system.entity.SupportTopic.class);
        faqs.findAllByOrderBySortOrderAsc().stream().filter(f -> Boolean.TRUE.equals(f.getActive()))
                .forEach(f -> faqMap.put(f.getTopic(), new SupportBot.Faq(f.getTopic(), f.getTitle(), f.getAnswer(), f.getKeywords())));
        return new SupportBot.Context(top.getOrderCode(), botParts, faqMap);
    }

    private OrderChatMessage botMessage(OrderChatRoom room, SupportBot.Reply reply, String action) {
        String options = reply.options().isEmpty() ? null : JSON.writeValueAsString(reply.options());
        return save(room, ChatSenderType.BOT, null, reply.text(), action, options);
    }

    private OrderChatMessage save(OrderChatRoom room, ChatSenderType sender, UUID senderId, String content,
                                  String action, String metadata) {
        // Lệch vài micro giây để tin trả lời luôn đứng sau tin của khách khi sắp theo thời gian.
        LocalDateTime now = LocalDateTime.now();
        if (room.getLastMessageAt() != null && !now.isAfter(room.getLastMessageAt())) now = room.getLastMessageAt().plusNanos(1000);
        room.setLastMessageAt(now);
        return messages.save(OrderChatMessage.builder().roomId(room.getId()).senderType(sender).senderId(senderId)
                .messageType(metadata == null ? "TEXT" : "OPTIONS").content(content).relatedAction(action)
                .metadata(metadata).createdAt(now).build());
    }

    private void publish(OrderChatRoom room, Order top, OrderChatMessage message, boolean escalated, boolean firstUnread) {
        events.publishEvent(new SupportChatEvents.MessageCreated(room.getId(), top.getId(), top.getOrderCode(),
                room.getCustomerId(), room.getAssignedStaffId(), storeIds(top), message.getSenderType(),
                messageView(message, Map.of(), null), roomView(room, top), escalated, firstUnread));
    }

    private SupportChatDtos.Conversation conversation(OrderChatRoom room, Order top) {
        List<OrderChatMessage> recent = new ArrayList<>(messages.findByRoomIdOrderByCreatedAtDesc(room.getId(),
                PageRequest.of(0, HISTORY_SIZE)));
        Collections.reverse(recent);
        Set<UUID> staffIds = recent.stream().filter(m -> m.getSenderType() == ChatSenderType.STAFF)
                .map(OrderChatMessage::getSenderId).collect(Collectors.toSet());
        Map<UUID, String> names = users.findAllById(staffIds).stream().collect(Collectors.toMap(User::getId, SupportChatService::name));
        String customerName = customers.findById(room.getCustomerId()).map(c -> c.getFullName()).orElse("Khách hàng");
        return new SupportChatDtos.Conversation(roomView(room, top),
                recent.stream().map(m -> messageView(m, names, customerName)).toList());
    }

    private SupportChatDtos.Room roomView(OrderChatRoom room, Order top) {
        var customer = customers.findById(room.getCustomerId()).orElse(null);
        var assigned = room.getAssignedStaffId() == null ? null
                : new OrderDetailResponse.Person(room.getAssignedStaffId(), userName(room.getAssignedStaffId()));
        String last = messages.findByRoomIdOrderByCreatedAtDesc(room.getId(), PageRequest.of(0, 1)).stream()
                .findFirst().map(OrderChatMessage::getContent).orElse(null);
        return new SupportChatDtos.Room(room.getId(), top.getId(), top.getOrderCode(),
                customer == null ? null : customer.getFullName(), customer == null ? null : customer.getPhone(),
                room.getStatus(), SupportChatDtos.statusLabel(room.getStatus()), assigned, room.getStaffUnreadCount(),
                room.getCustomerUnreadCount(), last == null ? null : last.length() > 120 ? last.substring(0, 120) + "…" : last,
                room.getLastMessageAt(), room.getEscalatedAt(), room.getCreatedAt());
    }

    private SupportChatDtos.Message messageView(OrderChatMessage message, Map<UUID, String> staffNames, String customerName) {
        String sender = switch (message.getSenderType()) {
            case BOT -> "Trợ lý tự động";
            case SYSTEM -> "Hệ thống";
            case STAFF -> staffNames.getOrDefault(message.getSenderId(), userName(message.getSenderId()));
            case CUSTOMER -> customerName == null ? "Khách hàng" : customerName;
        };
        List<SupportChatDtos.Option> options = message.getMetadata() == null ? List.of()
                : JSON.readValue(message.getMetadata(), new TypeReference<List<SupportChatDtos.Option>>() {
        });
        return new SupportChatDtos.Message(message.getId(), message.getRoomId(), message.getSenderType(), sender,
                message.getContent(), options, message.getRelatedAction(), message.getCreatedAt());
    }

    private String userName(UUID userId) {
        if (userId == null) return "Nhân viên";
        return users.findById(userId).map(SupportChatService::name).orElse("Nhân viên");
    }

    private static String name(User user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getUsername() : user.getFullName();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
