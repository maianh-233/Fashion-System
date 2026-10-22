package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.SupportChatDtos;
import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SupportChatServiceTest {
    final OrderChatRoomRepository rooms = mock(OrderChatRoomRepository.class);
    final OrderChatMessageRepository messages = mock(OrderChatMessageRepository.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final SupportFaqRepository faqs = mock(SupportFaqRepository.class);
    final CustomerRepository customers = mock(CustomerRepository.class);
    final StoreAccessService storeAccess = mock(StoreAccessService.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final SupportChatService service = new SupportChatService(rooms, messages, orders, mock(ShipmentRepository.class),
            mock(StoreRepository.class), faqs, customers, mock(UserRepository.class), storeAccess, new SupportBot(), events);

    final UUID customerId = UUID.randomUUID(), storeId = UUID.randomUUID(), staff = UUID.randomUUID(), other = UUID.randomUUID();
    final Order order = Order.builder().id(UUID.randomUUID()).orderCode("LB-1").customerId(UUID.randomUUID())
            .storeId(storeId).status(OrderStatus.SHIPPING).build();
    final List<OrderChatMessage> saved = new ArrayList<>();
    OrderChatRoom room;

    @BeforeEach void setUp() {
        order.setCustomerId(customerId);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(rooms.save(any())).thenAnswer(call -> {
            OrderChatRoom r = call.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(messages.save(any())).thenAnswer(call -> { saved.add(call.getArgument(0)); return call.getArgument(0); });
        when(faqs.findAllByOrderBySortOrderAsc()).thenReturn(List.of(SupportFaq.builder().topic(SupportTopic.CANCEL)
                .title("Chính sách hủy đơn").answer("Tự hủy khi chưa xác nhận.").keywords("hủy").active(true).sortOrder(1).build()));
        when(storeAccess.allows(staff, storeId, OrderPermissions.SUPPORT_CHAT)).thenReturn(true);
        when(storeAccess.allows(other, storeId, OrderPermissions.SUPPORT_CHAT)).thenReturn(true);
    }

    void existingRoom(SupportChatStatus status, UUID assigned) {
        room = OrderChatRoom.builder().id(UUID.randomUUID()).orderId(order.getId()).customerId(customerId).status(status)
                .assignedStaffId(assigned).createdAt(LocalDateTime.now()).lastMessageAt(LocalDateTime.now()).build();
        when(rooms.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(rooms.findById(room.getId())).thenReturn(Optional.of(room));
    }

    @Test void firstOpenCreatesBotRoomWithWelcomeMenu() {
        var conversation = service.open(customerId, order.getId());
        assertThat(conversation.room().status()).isEqualTo(SupportChatStatus.BOT_HANDLING);
        assertThat(saved).singleElement().satisfies(m -> {
            assertThat(m.getSenderType()).isEqualTo(ChatSenderType.BOT);
            assertThat(m.getMetadata()).contains("ORDER_STATUS").contains("TALK_TO_STAFF");
        });
    }

    @Test void customerCannotOpenAnotherCustomersOrder() {
        assertThatThrownBy(() -> service.open(UUID.randomUUID(), order.getId())).isInstanceOf(BusinessException.class);
    }

    @Test void botAnswersOptionWithoutInvolvingStaff() {
        existingRoom(SupportChatStatus.BOT_HANDLING, null);
        service.customerSend(customerId, room.getId(), new SupportChatDtos.CustomerSend("ORDER_STATUS", null));
        assertThat(saved).extracting(OrderChatMessage::getSenderType).containsExactly(ChatSenderType.CUSTOMER, ChatSenderType.BOT);
        assertThat(saved.get(1).getContent()).contains("Đang giao");
        assertThat(room.getStatus()).isEqualTo(SupportChatStatus.BOT_HANDLING);
        assertThat(room.getStaffUnreadCount()).isZero();
    }

    @Test void talkToStaffMovesRoomToWaitingAndRaisesEscalation() {
        existingRoom(SupportChatStatus.BOT_HANDLING, null);
        service.customerSend(customerId, room.getId(), new SupportChatDtos.CustomerSend("TALK_TO_STAFF", null));
        assertThat(room.getStatus()).isEqualTo(SupportChatStatus.WAITING_STAFF);
        assertThat(room.getEscalatedAt()).isNotNull();
        verify(events).publishEvent(argThat((Object e) -> e instanceof SupportChatEvents.MessageCreated m && m.escalated()));
    }

    @Test void messagesGoToStaffOnceSomeoneHandlesTheRoom() {
        existingRoom(SupportChatStatus.STAFF_HANDLING, staff);
        service.customerSend(customerId, room.getId(), new SupportChatDtos.CustomerSend(null, "Shop ơi giao giờ nào?"));
        service.customerSend(customerId, room.getId(), new SupportChatDtos.CustomerSend(null, "Mình đợi nhé"));
        assertThat(saved).extracting(OrderChatMessage::getSenderType).containsOnly(ChatSenderType.CUSTOMER);
        assertThat(room.getStaffUnreadCount()).isEqualTo(2);
        // Chỉ tin chưa đọc đầu tiên tạo thông báo.
        verify(events, times(1)).publishEvent(argThat((Object e) -> e instanceof SupportChatEvents.MessageCreated m && m.firstUnread()));
    }

    @Test void staffReplyAutoClaimsWaitingRoomAndAnotherStaffCannotTakeOver() {
        existingRoom(SupportChatStatus.WAITING_STAFF, null);
        service.reply(staff, room.getId(), "Chào bạn, mình hỗ trợ nhé");
        assertThat(room.getStatus()).isEqualTo(SupportChatStatus.STAFF_HANDLING);
        assertThat(room.getAssignedStaffId()).isEqualTo(staff);
        assertThat(room.getCustomerUnreadCount()).isEqualTo(1);
        assertThat(saved).extracting(OrderChatMessage::getSenderType).containsExactly(ChatSenderType.SYSTEM, ChatSenderType.STAFF);
        assertThatThrownBy(() -> service.claim(other, room.getId()))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }

    @Test void staffOfAnotherStoreIsForbidden() {
        existingRoom(SupportChatStatus.WAITING_STAFF, null);
        assertThatThrownBy(() -> service.claim(UUID.randomUUID(), room.getId()))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
    }

    @Test void closedRoomReopensWithBotWhenCustomerWritesAgain() {
        existingRoom(SupportChatStatus.STAFF_HANDLING, staff);
        service.close(staff, room.getId());
        assertThat(room.getStatus()).isEqualTo(SupportChatStatus.CLOSED);
        service.customerSend(customerId, room.getId(), new SupportChatDtos.CustomerSend(null, "cho mình hỏi muốn hủy đơn"));
        assertThat(room.getStatus()).isEqualTo(SupportChatStatus.BOT_HANDLING);
        assertThat(room.getAssignedStaffId()).isNull();
        assertThat(saved.getLast().getContent()).isEqualTo("Tự hủy khi chưa xác nhận.");
    }
}
